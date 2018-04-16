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

import com.google.gerrit.extensions.restapi.{
  Response,
  RestModifyView,
  RestReadView
}
import com.google.gerrit.server.project.ProjectResource
import com.google.inject.Inject

import scala.io.Source

import AnalyticDashboardSetup.writer

class GetAnalyticsStack @Inject()() extends RestReadView[ProjectResource] {
  override def apply(
      resource: ProjectResource): Response[AnalyticDashboardSetup] = {

    val projectName = resource.getControl.getProject.getName
    Response.ok(
      AnalyticDashboardSetup(
        projectName,
        Some(
          Source
            .fromFile(s"/tmp/docker-compose.${projectName}.yaml")
            .getLines
            .mkString)))
  }
}

class Input(var dashboardName: String)

class PutAnalyticsStack @Inject()()
    extends RestModifyView[ProjectResource, Input] {
  override def apply(resource: ProjectResource,
                     input: Input): Response[String] = {

    val projectName = resource.getControl.getProject.getName
    AnalyticDashboardSetup(projectName).createDashboardSetupFile()
    Response.created(s"Dashboard configuration created for $projectName!")
  }
}
