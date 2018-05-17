package com.googlesource.gerrit.plugins.analytics.wizard

import java.io.File
import java.nio.file.Path

import org.scalatest.{FlatSpec, Matchers}

class AnalyticDashboardSetupSpec extends FlatSpec with Matchers {
  behavior of "AnalyticDashboardSetup"

  it should "create a config file with correct name" in {
    var gotFilename: Option[Path] = None
    class MockWriter extends ConfigWriter {
      override def write(filename: Path, out: String): Unit = {
        gotFilename = Some(filename)
      }
    }
    implicit val writer = new MockWriter()

    val composeYamlFile = File.createTempFile(getClass.getName, ".yaml").toPath
    val ads = AnalyticDashboardSetup("aProject", composeYamlFile)
    ads.createDashboardSetupFile()
    gotFilename shouldBe Some(composeYamlFile)
  }
}
