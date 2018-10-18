// Copyright (C) 2018 The Android Open Source Project
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

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.google.gerrit.extensions.annotations.PluginName
import com.google.gerrit.extensions.registration.DynamicItem
import com.google.gerrit.httpd.{AllRequestFilter, WebSession}
import com.google.gerrit.server.AccessPath
import com.google.inject.{Inject, Singleton}
import org.slf4j.{Logger, LoggerFactory}

@Singleton
class XAuthFilter @Inject()(val webSession: DynamicItem[WebSession], @PluginName pluginName: String)
    extends AllRequestFilter {
  implicit val log: Logger    = LoggerFactory.getLogger(classOf[XAuthFilter])
  val authenticatedPluginURIs = (s".*/a/.*$pluginName.*").r

  override def init(filterConfig: FilterConfig) {}

  override def destroy() {}

  override def doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
    val uri = req.asInstanceOf[HttpServletRequest].getRequestURI
    authenticatedPluginURIs.findFirstIn(uri).foreach { _ =>
      val session = webSession.get
      if (session != null && session.isSignedIn && session.getXGerritAuth != null) {
        session.setAccessPathOk(AccessPath.REST_API, true)
        log.debug(s"Set URI $uri as authenticated REST-API access path")
      }
    }

    chain.doFilter(req, resp)
  }
}
