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
      Username(etlConfig.username)
    ).filter(_.value.isDefined) mkString " "
    s"$args --writeNotProcessedEventsTo file:///tmp/failed-events -e gerrit"
  }
  private val dockerComposeTemplate = {
    s"""
       |version: '3'
       |services:
       |
       |  gerrit-analytics-etl-gitcommits:
       |    extra_hosts:
       |      - gerrit:${gerritLocalUrl.getHost}
       |    image: gerritforge/gerrit-analytics-etl-gitcommits:latest
       |    container_name: gerrit-analytics-etl-gitcommits
       |    environment:
       |      - ES_HOST=elasticsearch
       |      - GERRIT_URL=${gerritLocalUrl.getProtocol}://gerrit:${gerritLocalUrl.getPort}
       |      - ANALYTICS_ARGS=$analyticsArgs
       |    networks:
       |      - ek
       |    links:
       |      - elasticsearch
       |    depends_on:
       |      - elasticsearch
       |
       |  dashboard-importer:
       |    image: gerritforge/analytics-dashboard-importer:latest
       |    networks:
       |      - ek
       |    links:
       |      - elasticsearch
       |      - kibana
       |
       |  kibana:
       |    image: gerritforge/analytics-kibana:latest
       |    container_name: "kibana-for-${sanitisedName}-project"
       |    networks:
       |      - ek
       |    depends_on:
       |      - elasticsearch
       |    ports:
       |      - "5601:5601"
       |
       |  elasticsearch:
       |    image: gerritforge/analytics-elasticsearch:latest
       |    container_name: "es-for-${sanitisedName}-project"
       |    networks:
       |      - ek
       |    environment:
       |      - ES_JAVA_OPTS=-Xmx1g -Xms1g
       |      - http.host=0.0.0.0
       |      - network.host=_site_
       |      - http.publish_host=_site_
       |      - http.cors.allow-origin=*
       |      - http.cors.enabled=true
       |
       |    ports:
       |      - "9200:9200"
       |      - "9300:9300"
       |networks:
       |  ek:
       |    driver: bridge
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
