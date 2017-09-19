import sbt.Keys.version
import sbt._

enablePlugins(JavaAppPackaging)

val JDK = "1.8"
val buildNumber = scala.util.Properties.envOrNone("version").map(v => "." + v).getOrElse("")
val hydraNotificationsVersion = "0.1.0" + buildNumber

lazy val defaultSettings = Seq(
  organization := "pluralsight",
  version := hydraNotificationsVersion,
  scalaVersion := "2.12.3",
  description := "Hydra Notifications API",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  excludeDependencies += "org.slf4j" % "slf4j-log4j12",
  excludeDependencies += "log4j" % "log4j",
  packageOptions in(Compile, packageBin) +=
    Package.ManifestAttributes("Implementation-Build" -> buildNumber),
  logLevel := Level.Info,
  scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-language:_", "-deprecation", "-unchecked"),
  javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", JDK, "-target", JDK,
    "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:-options"),

  resolvers += Resolver.mavenLocal,
  resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Confluent Maven Repo" at "http://packages.confluent.io/maven/",
  resolvers += "jitpack" at "https://jitpack.io",
  coverageExcludedPackages := "hydra\\.ingest\\.HydraIngestApp.*",
  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
  ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))
)

lazy val root = Project(
  id = "hydra-notifications",
  base = file("."),
  settings = defaultSettings
).aggregate(server, client)

lazy val client = Project(
  id = "client",
  base = file("client"),
  settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.clientDeps)
).settings(name := "hydra-notifications-client")

lazy val server = Project(
  id = "server",
  base = file("server"),
  settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.serverDeps)
    ++ Seq(
    mainClass in Compile := Some("hydra.notifications.NotificationsService"),
    javaOptions += "-Xmx2G"
  )
).dependsOn(client).settings(name := "hydra-notifications-server").enablePlugins(JavaAppPackaging)