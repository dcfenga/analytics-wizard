enablePlugins(GitVersioning)

val gerritApiVersion = "2.16-rc0"
val pluginName = "analytics-wizard"

git.useGitDescribe := true

lazy val root = (project in file("."))
  .settings(
    name := pluginName,
    resolvers += Resolver.mavenLocal,
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "3.0" % Provided,
      "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources (),
      "com.spotify" % "docker-client" % "8.14.1",
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

