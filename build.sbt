import sbt.Keys.version
import sbt._

enablePlugins(JavaAppPackaging)

val JDK = "1.8"
val buildNumber = scala.util.Properties.envOrNone("version").map(v => "." + v).getOrElse("")
val hydraNotificationsVersion = "0.1.0" + buildNumber

val registryUrl = sys.env.get("DOCKER_REGISTRY_URL")

lazy val dockerSettings = Seq(
  buildOptions in docker := BuildOptions(
    cache = false,
    removeIntermediateContainers = BuildOptions.Remove.Always,
    pullBaseImage = BuildOptions.Pull.IfMissing
  ),
  dockerfile in docker := {
    val appDir: File = stage.value
    val targetDir = "/opt/hydra-notifications-server"

    val portNumber = sys.env.getOrElse[String]("PORT_NUMBER", "8080").toInt

    new Dockerfile {
      from("java")
      maintainer("Alex Silva <alex-silva@pluralsight.com>")
      user("root")
      env("JAVA_OPTS", "-Xmx4G")
      run("mkdir", "-p", "/var/log/hydra")
      expose(portNumber)
      entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      copy(appDir, targetDir)
    }
  },
  imageNames in docker := Seq(
    ImageName(s"$registryUrl/hydra-notifications:latest"),
    ImageName(
      namespace = Some(registryUrl),
      repository = "hydra-notifications",
      tag = Some(version.value)
    )
  )
)

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
  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
)

lazy val root = Project(
  id = "hydra-notifications",
  base = file(".")
).settings(defaultSettings)
  .aggregate(server, client)

lazy val client = Project(
  id = "client",
  base = file("client")
).settings(defaultSettings, name := "hydra-notifications-client", libraryDependencies ++= Dependencies.clientDeps)

val serverSettings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.serverDeps) ++ Seq(
  mainClass in Compile := Some("hydra.notifications.NotificationsService"),
  javaOptions += "-Xmx2G"
)

lazy val server = Project(
  id = "server",
  base = file("server")
).dependsOn(client)
  .settings(serverSettings, dockerSettings, name := "hydra-notifications-server")
  .enablePlugins(JavaAppPackaging, sbtdocker.DockerPlugin)