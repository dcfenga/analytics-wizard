package com.googlesource.gerrit.plugins.analytics.wizard

import java.io.File
import java.net.URL
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.googlesource.gerrit.plugins.analytics.wizard.model.AggregationType.{Email, EmailYear}
import com.googlesource.gerrit.plugins.analytics.wizard.model.ETLConfig
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
    val ads =
      AnalyticDashboardSetup("aProject",
                             composeYamlFile,
                             new URL("http://gerrit_local_ip_address:8080"),
                             ETLConfig(Email, None, None, None, None, None, None, None))
    ads.createDashboardSetupFile()
    gotFilename shouldBe Some(composeYamlFile)
  }

  it should "create a config file with correct analytics args" in {
    var gotFilename: Option[Path] = None
    var dockerCompose: String     = ""
    class MockWriter extends ConfigWriter {
      override def write(filename: Path, out: String): Unit = {
        gotFilename = Some(filename)
        dockerCompose = out
      }
    }
    implicit val writer = new MockWriter()

    val dateSince       = "2018-10-10"
    val localDateSince  = LocalDate.parse(dateSince, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val dateUntil       = "2018-10-20"
    val localDateUntil  = LocalDate.parse(dateUntil, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val prefix          = "myProjectPrefix"
    val password        = "myPassword"
    val username        = "myUsername"
    val composeYamlFile = File.createTempFile(getClass.getName, ".yaml").toPath
    val ads =
      AnalyticDashboardSetup(
        "aProject",
        composeYamlFile,
        new URL("http://gerrit_local_ip_address:8080"),
        ETLConfig(EmailYear,
                  Some(prefix),
                  Some(localDateSince),
                  Some(localDateUntil),
                  None,
                  None,
                  Some(username),
                  Some(password))
      )
    ads.createDashboardSetupFile()
    gotFilename shouldBe Some(composeYamlFile)
    dockerCompose should include("--aggregate email_year")
    dockerCompose should include(s"--until $dateUntil")
    dockerCompose should include(s"--since $dateSince")
    dockerCompose should include(s"--prefix $prefix")
    dockerCompose should include(s"--password $password")
    dockerCompose should include(s"--username $username")
  }
}
