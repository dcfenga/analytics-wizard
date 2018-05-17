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

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path

import com.google.common.io.ByteStreams
import com.google.gerrit.extensions.restapi.{
  Response,
  RestApiException,
  RestModifyView,
  RestReadView
}
import com.google.gerrit.server.project.ProjectResource
import com.google.inject.Inject
import AnalyticDashboardSetup.writer
import com.google.gerrit.extensions.annotations.PluginData

import scala.io.Source

class GetAnalyticsStack @Inject()(@PluginData val dataPath: Path) extends RestReadView[ProjectResource] {
  override def apply(
      resource: ProjectResource): Response[AnalyticDashboardSetup] = {

    val projectName = resource.getControl.getProject.getName
    Response.ok(
      AnalyticDashboardSetup(
        projectName,
        dataPath.resolve(s"docker-compose.${projectName}.yaml")))
  }
}

class Input(var dashboardName: String)

class PutAnalyticsStack @Inject()(@PluginData val dataPath: Path)
    extends RestModifyView[ProjectResource, Input] {
  override def apply(resource: ProjectResource,
                     input: Input): Response[String] = {

    val projectName = resource.getControl.getProject.getName
    AnalyticDashboardSetup(projectName, dataPath.resolve(s"docker-compose.${projectName}.yaml")).createDashboardSetupFile()
    Response.created(s"Dashboard configuration created for $projectName!")
  }
}

class DockerComposeCommand(var action: String)
class PostAnalyticsStack @Inject()(@PluginData val dataPath: Path)
    extends RestModifyView[ProjectResource, DockerComposeCommand] {
  override def apply(resource: ProjectResource,
                     input: DockerComposeCommand): Response[String] = {

    val projectName = resource.getControl.getProject.getName
    val pb = new ProcessBuilder(
      "docker-compose",
      "-f",
      s"${dataPath.toFile.getAbsolutePath}/docker-compose.${projectName}.yaml",
      input.action.toLowerCase)
    pb.redirectErrorStream(true)

    val ps: Process = pb.start
    ps.getOutputStream.close
    val output = new String(ByteStreams.toByteArray(ps.getInputStream), UTF_8)
    ps.waitFor

    ps.exitValue match {
      case 0 => Response.created(output)
      case _ =>
        throw new RestApiException(
          s"Failed with exit code: ${ps.exitValue} - $output")
    }

  }
}
