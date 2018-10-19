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
package com.googlesource.gerrit.plugins.analytics.wizard.model

import java.net.{MalformedURLException, URL}
import java.time.LocalDate
import java.time.format.DateTimeParseException

import org.scalatest.EitherValues._
import org.scalatest.{FlatSpec, Matchers}

class ETLConfigSpec extends FlatSpec with Matchers {

  behavior of "ETLConfig.fromRaw"

  val etlConfigRaw = ETLConfigRaw(
    aggregate = "email",
    projectPrefix = "prefix",
    since = "2018-10-10",
    until = "2018-10-13",
    eventsUrl = "file:///tmp/gerrit-events-export.json",
    emailAliasesPath = "/tmp/path",
    writeNotProcessedEventsTo = "file://tmp/myfile.json",
    username = "dss",
    password = "dsd"
  )

  it should "pass validation with correct parameters" in {

    ETLConfig.fromRaw(etlConfigRaw).right.value shouldBe ETLConfig(
      aggregate = AggregationType.Email,
      projectPrefix = Some(etlConfigRaw.projectPrefix),
      since = Some(LocalDate.of(2018, 10, 10)),
      until = Some(LocalDate.of(2018, 10, 13)),
      eventsUrl = Some(new URL(etlConfigRaw.eventsUrl)),
      writeNotProcessedEventsTo = Some(new URL(etlConfigRaw.writeNotProcessedEventsTo)),
      emailAliasesPath = Some(etlConfigRaw.emailAliasesPath),
      username = Some(etlConfigRaw.username),
      password = Some(etlConfigRaw.password)
    )
  }

  it should "defaults non mandatory parameters" in {
    ETLConfig
      .fromRaw(
        etlConfigRaw.copy(projectPrefix = null,
                          since = null,
                          until = null,
                          eventsUrl = null,
                          writeNotProcessedEventsTo = null,
                          emailAliasesPath = null,
                          username = null,
                          password = null))
      .right
      .value shouldBe ETLConfig(
      aggregate = AggregationType.Email,
      projectPrefix = None,
      since = None,
      until = None,
      eventsUrl = None,
      writeNotProcessedEventsTo = None,
      emailAliasesPath = None,
      username = None,
      password = None
    )
  }

  it should "handle `null` mandatory parameters" in {
    ETLConfig
      .fromRaw(
        etlConfigRaw.copy(
          aggregate = null,
          projectPrefix = null,
          since = null,
          until = null,
          eventsUrl = null,
          writeNotProcessedEventsTo = null,
          emailAliasesPath = null,
          username = null,
          password = null
        ))
      .right
      .value shouldBe ETLConfig(
      aggregate = AggregationType.Email,
      projectPrefix = None,
      since = None,
      until = None,
      eventsUrl = None,
      writeNotProcessedEventsTo = None,
      emailAliasesPath = None,
      username = None,
      password = None
    )
  }

  it should "fail validation for invalid 'since' parameter" in {
    val invalidDate = "randomString"
    val error = ETLConfig
      .fromRaw(etlConfigRaw.copy(since = invalidDate))
      .left
      .value
    error.value shouldBe invalidDate
    error.parameter shouldBe "since"
    error.cause shouldBe a[DateTimeParseException]
  }

  it should "fail validation for invalid 'until' parameter" in {
    val invalidDate = "randomString"
    val error = ETLConfig
      .fromRaw(etlConfigRaw.copy(until = invalidDate))
      .left
      .value
    error.value shouldBe invalidDate
    error.parameter shouldBe "until"
    error.cause shouldBe a[DateTimeParseException]
  }

  it should "fail validation for invalid 'writeNotProcessedEventsTo' parameter" in {
    val invalidUrl = "randomString"
    val error = ETLConfig
      .fromRaw(etlConfigRaw.copy(writeNotProcessedEventsTo = invalidUrl))
      .left
      .value
    error.value shouldBe invalidUrl
    error.parameter shouldBe "writeNotProcessedEventsTo"
    error.cause shouldBe a[MalformedURLException]
  }

  it should "fail validation for invalid 'eventsPath' parameter" in {
    val invalidUrl = "not|good.txt"
    val error = ETLConfig
      .fromRaw(etlConfigRaw.copy(eventsUrl = invalidUrl))
      .left
      .value
    error.value shouldBe invalidUrl
    error.parameter shouldBe "eventsUrl"
    error.cause shouldBe a[MalformedURLException]
  }
}
