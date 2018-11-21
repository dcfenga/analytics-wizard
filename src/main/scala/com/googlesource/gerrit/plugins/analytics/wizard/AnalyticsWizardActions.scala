// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.analytics.wizard
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path

import com.google.common.io.ByteStreams
import com.google.gerrit.extensions.annotations.PluginData
import com.google.gerrit.extensions.restapi.{
  Response,
  RestApiException,
  RestModifyView,
  RestReadView
}
import com.google.gerrit.server.config.GerritServerConfig
import com.google.gerrit.server.project.ProjectResource
import com.google.inject.{ImplementedBy, Inject}
import com.googlesource.gerrit.plugins.analytics.wizard.AnalyticDashboardSetup.writer
import com.googlesource.gerrit.plugins.analytics.wizard.model.{
  ETLConfig,
  ETLConfigRaw,
  ETLConfigValidationError
}
import com.googlesource.gerrit.plugins.analytics.wizard.utils._
import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import org.eclipse.jgit.lib.Config

import scala.util.{Failure, Success}
case class Input(dashboardName: String, etlConfig: ETLConfigRaw)
class PutAnalyticsStack @Inject()(@PluginData val dataPath: Path,
                                  @GerritServerConfig gerritConfig: Config)
    extends RestModifyView[ProjectResource, Input] {
  override def apply(resource: ProjectResource, input: Input): Response[String] = {
    val projectName                                             = resource.getName
    val encodedName                                             = AnalyticsWizardActions.encodedName(projectName)
    val etlConfigE: Either[ETLConfigValidationError, ETLConfig] = ETLConfig.fromRaw(input.etlConfig)
    etlConfigE.fold(
      configError =>
        Response.withStatusCode(400,
                                s"Cannot create dashboard configuration: ${configError.message}"),
      etlConfig => {
        val configHelper = new GerritConfigHelper(gerritConfig) with LocalAddressGetter
        configHelper.getGerritLocalAddress match {
          case Success(gerritLocalUrl) =>
            AnalyticDashboardSetup(
              input.dashboardName,
              dataPath.resolve(s"docker-compose.${input.dashboardName}.yaml"),
              gerritLocalUrl,
              etlConfig
            ).createDashboardSetupFile()
            Response.created(s"Dashboard configuration created for $encodedName!")
          case Failure(exception) =>
            Response.withStatusCode(
              500,
              s"Cannot create dashboard configuration - '${exception.getMessage}'")
        }
      }
    )
  }
}
class DockerComposeCommand(var action: String, var dashboardName: String)
class PostAnalyticsStack @Inject()(@PluginData val dataPath: Path)
    extends RestModifyView[ProjectResource, DockerComposeCommand] {
  override def apply(resource: ProjectResource, input: DockerComposeCommand): Response[String] = {
    val encodedName = AnalyticsWizardActions
      .encodedName(input.dashboardName)
    val pb = new ProcessBuilder(
      "docker-compose",
      "-f",
      s"${dataPath.toFile.getAbsolutePath}/docker-compose.${encodedName}.yaml",
      input.action.toLowerCase,
      "--detach"
    )
    pb.redirectErrorStream(true)
    val ps: Process = pb.start
    ps.getOutputStream.close
    val output =
      new String(ByteStreams.toByteArray(ps.getInputStream), UTF_8)
    ps.waitFor
    ps.exitValue match {
      case 0 => Response.created(output)
      case _ =>
        throw new RestApiException(s"Failed with exit code: ${ps.exitValue} - $output")
    }
  }
}
class GetAnalyticsStackStatus @Inject()(@PluginData val dataPath: Path,
                                        val dockerClientProvider: DockerClientProvider)
    extends RestReadView[ProjectResource] {
  override def apply(resource: ProjectResource): Response[String] = {
    val containerName = "gerrit-analytics-etl-gitcommits"
    responseFromContainerInfo(dockerClientProvider.client.inspectContainer(containerName))
  }
  private def responseFromContainerInfo(containerInfo: ContainerInfo) = {
    containerInfo.state match {
      case s if s.exitCode != 0 =>
        throw new RestApiException(s"Data import failed")
      case s if s.running =>
        Response.withStatusCode(202, "processing")
      case s if s.status == "exited" =>
        //Spark ETL job exited successfully
        Response.withStatusCode(204, "finished")
      case _ => throw new RestApiException(s"Case not handled")
    }
  }
}
object AnalyticsWizardActions {
  // URLEncoder could potentially throw UnsupportedEncodingException,
  // but UTF-8 will *always* be resolved, otherwise, Gerrit wouldn't work at all
  def encodedName(name: String) =
    try {
      URLEncoder.encode(name, "UTF-8")
    } catch {
      case e: Throwable => throw new RuntimeException(e)
    }
}
@ImplementedBy(classOf[DockerClientProviderImpl])
trait DockerClientProvider {
  def client: DockerClient
}
class DockerClientProviderImpl extends DockerClientProvider {
  def client: DockerClient = DefaultDockerClient.fromEnv.build
}
