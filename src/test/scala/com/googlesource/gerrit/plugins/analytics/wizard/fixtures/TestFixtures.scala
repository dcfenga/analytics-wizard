package com.googlesource.gerrit.plugins.analytics.wizard.fixtures

import org.eclipse.jgit.lib.Config

trait TestFixtures {

  def gerritConfig(httpProtocol: String, httpPort: Int): Config = {
    val gerritConfig = new Config()
    gerritConfig.fromText(s"""
         |[httpd]
         |   listenUrl = $httpProtocol://*:$httpPort/
      """.stripMargin)
    gerritConfig
  }
}
