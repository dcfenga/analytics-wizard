package com.googlesource.gerrit.plugins.analytics.wizard

import java.util
import com.google.gerrit.extensions.client.MenuItem
import com.google.gerrit.extensions.webui.TopMenu
import scala.collection.JavaConverters._

class AnalyticsWizardTopMenu extends TopMenu {
  override def getEntries: util.List[TopMenu.MenuEntry] =
    List(
      new TopMenu.MenuEntry(
        "Analytics Wizard",
        List(
          new MenuItem("Configure Dashboard",
                       "/plugins/analytics-wizard/static/analytics-dashboard.html",
                       "_self")).asJava)).asJava
}
