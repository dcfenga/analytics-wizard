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

import java.io.PrintWriter

trait ConfigWriter {
  def write(filename: String, out: String)
}

class ConfigWriterImpl extends ConfigWriter {
  def write(filename: String, out: String) = {
    val p = new PrintWriter(filename)
    p.write(out)
    p.close()
  }
}

case class AnalyticDashboardSetup(name: String, config: Option[String] = None)(
    implicit val writer: ConfigWriter) {

  private val dockerComposeTemplate = { (name: String) =>
    s"""
       |version: '3'
       |services:
       |  kibana:
       |    image: gerritforge/analytics-kibana:latest
       |    container_name: "kibana-for-${name}-project"
       |    environment:
       |      SERVER_BASEPATH: "/kibana"
       |    depends_on:
       |      - elasticsearch
       |  elasticsearch:
       |    image: gerritforge/analytics-elasticsearch:latest
       |    container_name: "es-for-${name}-project"
       |    environment:
       |      - ES_JAVA_OPTS=-Xmx4g -Xms4g
       |      - http.host=0.0.0.0
       |    volumes:
       |      - es-indexes:/usr/share/elasticsearch/data
     """.stripMargin
  }

  val configFileName = s"/tmp/docker-compose.${name}.yaml"
  def createDashboardSetupFile(): Unit = {
    writer.write(configFileName, dockerComposeTemplate(name))
  }

}

object AnalyticDashboardSetup {
  implicit val writer = new ConfigWriterImpl()
}
