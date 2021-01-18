enablePlugins(GitVersioning)

val gerritApiVersion = "3.2.3"
val pluginName       = "analytics-wizard"

scalaVersion := "2.11.12"

git.useGitDescribe := true

scalafmtOnCompile in ThisBuild := true

lazy val root = (project in file("."))
  .settings(
    name := pluginName,
    resolvers += Resolver.mavenLocal,
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice"             % "3.0" % Provided,
      "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources (),
      "com.spotify"       % "docker-client"     % "8.14.1",
      "com.beachape"      %% "enumeratum"       % "1.5.13",
      "org.scalatest"     %% "scalatest"        % "3.0.4" % Test,
      "net.codingwell"    %% "scala-guice"      % "4.1.0" % Test
    ),
    assemblyJarName in assembly := s"$pluginName.jar",
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
      ("Gerrit-ApiType", "plugin"),
      ("Gerrit-PluginName", pluginName),
      ("Gerrit-Module", "com.googlesource.gerrit.plugins.analytics.wizard.Module"),
      ("Gerrit-HttpModule", "com.googlesource.gerrit.plugins.analytics.wizard.HttpModule"),
      ("Implementation-Title", "Analytics plugin wizard")
    )
  )
