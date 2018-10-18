package com.googlesource.gerrit.plugins.analytics.wizard.utils

import java.net.{MalformedURLException, URL}

import com.googlesource.gerrit.plugins.analytics.wizard.fixtures.TestFixtures
import org.eclipse.jgit.lib.Config
import org.scalatest.TryValues._
import org.scalatest.{FlatSpec, Matchers}

class GerritConfigHelperSpec extends FlatSpec with Matchers with TestFixtures {
  behavior of "getGerritLocalAddress"

  it should "retrieve a URL successfully when the right configuration is set" in {
    val httpProtocol = "http"
    val httpPort     = 8080
    val helper = new GerritConfigHelper(gerritConfig(httpProtocol, httpPort))
    with TestLocalAddressGetter

    helper.getGerritLocalAddress.success.value shouldBe new URL(
      s"$httpProtocol://${helper.getLocalAddress}:$httpPort")

  }

  it should "fail in retrieving a URL when configuration is missing" in {
    val helper = new GerritConfigHelper(new Config()) with TestLocalAddressGetter

    helper.getGerritLocalAddress.failure.exception shouldBe a[MalformedURLException]
  }

  it should "fail in retrieving a URL when local address cannot be retrieved" in {
    val helper = new GerritConfigHelper(new Config()) with FailingLocalAddressGetter

    helper.getGerritLocalAddress.failure.exception shouldBe a[MalformedURLException]
  }
}

trait TestLocalAddressGetter extends LocalAddressGetter {
  override def getLocalAddress: String = "127.0.0.1"
}

trait FailingLocalAddressGetter extends LocalAddressGetter {
  override def getLocalAddress: String = throw new Exception("Cannot get local address")
}
