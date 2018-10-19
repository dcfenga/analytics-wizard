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

import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.googlesource.gerrit.plugins.analytics.wizard.model.AggregationType.Email
import enumeratum._

import scala.util.{Failure, Success, Try}

case class ETLConfig(aggregate: AggregationType,
                     projectPrefix: Option[String],
                     since: Option[LocalDate],
                     until: Option[LocalDate],
                     eventsUrl: Option[URL],
                     writeNotProcessedEventsTo: Option[URL],
                     emailAliasesPath: Option[String],
                     username: Option[String],
                     password: Option[String])

sealed trait AggregationType extends EnumEntry
object AggregationType extends Enum[AggregationType] {
  val values = findValues

  case object Email      extends AggregationType
  case object EmailHour  extends AggregationType
  case object EmailDay   extends AggregationType
  case object EmailMonth extends AggregationType
  case object EmailYear  extends AggregationType
}

object ETLConfig {
  def fromRaw(raw: ETLConfigRaw): Either[ETLConfigValidationError, ETLConfig] = {
    for {
      s  <- validateLocalDate("since", raw.since).right
      u  <- validateLocalDate("until", raw.until).right
      w  <- validateUrl("writeNotProcessedEventsTo", raw.writeNotProcessedEventsTo).right
      eu <- validateUrl("eventsUrl", raw.eventsUrl).right
      a  <- validateAggregate(raw.aggregate).right
    } yield
      ETLConfig(
        aggregate = a,
        projectPrefix = Option(raw.projectPrefix),
        since = s,
        until = u,
        eventsUrl = eu,
        writeNotProcessedEventsTo = w,
        emailAliasesPath = Option(raw.emailAliasesPath),
        username = Option(raw.username),
        password = Option(raw.password)
      )
  }

  private def validateLocalDate(
      parameter: String,
      value: String): Either[LocalDateValidationError, Option[LocalDate]] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Option(value)
      .map { maybeDate =>
        Try(LocalDate.parse(maybeDate, formatter)) match {
          case Success(date)      => Right(Some(date))
          case Failure(exception) => Left(LocalDateValidationError(parameter, maybeDate, exception))
        }
      }
      .getOrElse(Right(None))
  }
  private def validateUrl(parameter: String,
                          value: String): Either[UrlValidationError, Option[URL]] = {
    Option(value)
      .map { u =>
        Try(new URL(u)) match {
          case Success(url)       => Right(Some(url))
          case Failure(exception) => Left(UrlValidationError(parameter, u, exception))
        }
      }
      .getOrElse(Right(None))
  }
  private def validateAggregate(
      value: String): Either[AggregateValidationError, AggregationType] = {
    val maybeAggregate =
      AggregationType.withNameInsensitiveOption(Option(value).getOrElse("email").replace("_", ""))
    Either.cond(maybeAggregate.isDefined, maybeAggregate.get, AggregateValidationError(value))
  }
}

sealed trait ETLConfigValidationError {
  def message: String = s"Error validating '$parameter' parameter: $value. Exception: $cause"
  def value: String
  def parameter: String
  def cause: Throwable
}
case class LocalDateValidationError(parameter: String, value: String, cause: Throwable)
    extends ETLConfigValidationError
case class UrlValidationError(parameter: String, value: String, cause: Throwable)
    extends ETLConfigValidationError
case class AggregateValidationError(value: String) extends ETLConfigValidationError {
  val parameter        = "aggregate"
  val cause: Throwable = new Throwable(s"Value $value is not a valid aggregation type")
}
