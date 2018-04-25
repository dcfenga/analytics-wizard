enablePlugins(GitVersioning)

val gerritApiVersion = "2.15.1"
val pluginName = "analytics-wizard"

git.useGitDescribe := true

lazy val root = (project in file("."))
  .settings(
    name := pluginName,

    scalaVersion := "2.12.5",

    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "3.0" % Provided,
      "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources(),
      "com.google.code.gson" % "gson" % "2.7" % Provided,

      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "net.codingwell" %% "scala-guice" % "4.1.0" % Test),

    assemblyJarName in assembly := s"$pluginName.jar",

    packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
      ("Gerrit-ApiType", "plugin"),
      ("Gerrit-PluginName", pluginName),
      ("Gerrit-Module", "com.googlesource.gerrit.plugins.analytics.wizard.Module"),
      ("Gerrit-HttpModule", "com.googlesource.gerrit.plugins.analytics.wizard.HttpModule"),
      ("Implementation-Title", "Analytics plugin wizard")
    )
  )

