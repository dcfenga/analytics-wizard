package com.googlesource.gerrit.plugins.analytics.wizard.utils

import java.net.{InetAddress, URL}

import org.eclipse.jgit.lib.Config

import scala.util.Try

class GerritConfigHelper(gerritConfig: Config) { self: LocalAddressGetter =>

  def getGerritLocalAddress: Try[URL] = for {
    listenUrl    <- Try { new URL(gerritConfig.getString("httpd", null, "listenUrl")) }
    localAddress <- Try { getLocalAddress }
  } yield new URL(s"${listenUrl.getProtocol}://$localAddress:${listenUrl.getPort}")
}

trait LocalAddressGetter {
  def getLocalAddress = InetAddress.getLocalHost.getHostAddress
}