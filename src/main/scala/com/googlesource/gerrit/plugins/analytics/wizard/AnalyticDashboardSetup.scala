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

import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import com.googlesource.gerrit.plugins.analytics.wizard.model.ETLConfig

trait ConfigWriter {
  def write(outputPath: Path, out: String)
}

class ConfigWriterImpl extends ConfigWriter {
  def write(outputPath: Path, out: String) {
    Files.write(outputPath, out.getBytes(StandardCharsets.UTF_8))
  }
}

case class AnalyticDashboardSetup(name: String,
                                  dockerComposeYamlPath: Path,
                                  gerritLocalUrl: URL,
                                  etlConfig: ETLConfig)(implicit val writer: ConfigWriter) {

  // Docker doesn't like container names with '/', hence the replace with '-'
  // Furthermore timestamp has been added to avoid conflicts among container names, i.e.:
  // A project named 'foo/bar' would be encoded as 'foo-bar' and thus its container
  // would be potentially in conflict with another 'foo-bar' project's one
  private val sanitisedName =
    s"${name.replace("/", "-")}-${System.currentTimeMillis}"
  private def analyticsArgs: String = {
    val args = List(
      Since(etlConfig.since.map(_.toString)),
      Until(etlConfig.until.map(_.toString)),
      ProjectPrefix(etlConfig.projectPrefix),
      Aggregate(Some(etlConfig.aggregate.entryName)),
      Password(etlConfig.password),
      Username(etlConfig.username),
      DashboardName(Option(name))
    ).filter(_.value.isDefined) mkString " "
    s"$args -e gitcommits"
  }
  private val dockerComposeTemplate = {
    s"""
       |version: '3'
       |services:
       |
       |  gerrit-analytics-etl-gitcommits:
       |    extra_hosts:
       |      - gerrit:${gerritLocalUrl.getHost}
       |    image: gerritforge/gerrit-analytics-etl-gitcommits:1.0-62-g4f7fbeb-SNAPSHOT
       |    container_name: gerrit-analytics-etl-gitcommits
       |    environment:
       |      - ES_PORT=9200
       |      - ES_HOST=10.180.108.98
       |      - GERRIT_URL=${gerritLocalUrl.getProtocol}://gerrit:${gerritLocalUrl.getPort}
       |      - ANALYTICS_ARGS=$analyticsArgs
       |      - SPARK_LOCAL_IP=10.180.108.98
       |    network_mode: host
     """.stripMargin
  }

  def createDashboardSetupFile(): Unit = {
    writer.write(dockerComposeYamlPath, dockerComposeTemplate)
  }

}

object AnalyticDashboardSetup {
  implicit val writer = new ConfigWriterImpl()
}

sealed trait AnalyticsOption {
  val name: String
  val value: Option[String]

  override def toString: String = s"$name ${value.getOrElse("")}"
}
case class Since(value: Option[String]) extends AnalyticsOption {
  val name = "--since"
}
case class Until(value: Option[String]) extends AnalyticsOption {
  val name = "--until"
}
case class ProjectPrefix(value: Option[String]) extends AnalyticsOption {
  val name = "--prefix"
}
case class Aggregate(value: Option[String]) extends AnalyticsOption {
  val name = "--aggregate"
}
case class Password(value: Option[String]) extends AnalyticsOption {
  val name = "--password"
}
case class Username(value: Option[String]) extends AnalyticsOption {
  val name = "--username"
}
case class DashboardName(value: Option[String]) extends AnalyticsOption {
  val name = "--dashboard"
}
