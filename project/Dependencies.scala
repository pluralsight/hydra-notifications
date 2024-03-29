import sbt.{ExclusionRule, _}


object Dependencies {
  val akkaVersion = "2.6.7"
  val scalaTestVersion = "3.0.1"
  val slf4jVersion = "2.16.0"
  val log4jVersion = "2.17.1"
  val kxbmapConfigVersion = "0.4.4"
  val typesafeConfigVersion = "1.3.1"
  val jodaTimeVersion = "2.9.9"
  val jodaConvertVersion = "1.8.1"
  val akkaHTTPVersion = "10.1.14"
  val scalaMockVersion = "3.5.0"
  val serviceContainerVersion = "2.0.7"
  val slackVersion = "0.2.14"
  val jerseyVersion = "1.9.1"
  val scalaCacheVersion = "0.9.3"
  val reflectionsVersion = "0.9.10"

  object Compile {

    val scalaConfigs = "com.github.kxbmap" %% "configs" % kxbmapConfigVersion

    val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

    val logging = Seq(
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
      "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
      "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
      "org.apache.logging.log4j" % "log4j-1.2-api" % log4jVersion)

    val akka = Seq("com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHTTPVersion,
      "ch.megard" %% "akka-http-cors" % "0.4.2")

    val guavacache = "com.github.cb372" %% "scalacache-guava" % scalaCacheVersion

    val joda = Seq("joda-time" % "joda-time" % jodaTimeVersion, "org.joda" % "joda-convert" % jodaConvertVersion)

    val serviceContainer = ("com.github.vonnagy" %% "service-container" % serviceContainerVersion)
      .excludeAll(
        ExclusionRule(organization = "ch.qos.logback"),
        ExclusionRule(organization = "org.slf4j"),
        ExclusionRule(organization = "com.typesafe.akka")
      )

    val slack = "com.github.slack-scala-client" %% "slack-scala-client" % slackVersion

    val opsGenie = Seq(
      "com.opsgenie.integration" % "sdk" % "2.11.2",
      "com.sun.jersey" % "jersey-core" % jerseyVersion,
      "com.sun.jersey" % "jersey-server" % jerseyVersion exclude("asm", "asm"),
      "com.sun.jersey" % "jersey-json" % jerseyVersion,
      "com.sun.jersey.contribs" % "jersey-multipart" % jerseyVersion
    )

    val reflections = "org.reflections" % "reflections" % reflectionsVersion

    val wireMock = "com.github.tomakehurst" % "wiremock-standalone" % "2.8.0" % "test"
  }

  object Test {
    val akkaTest = Seq("com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHTTPVersion % "test")

    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % "test"
  }

  import Compile._
  import Test._

  val testDeps = Seq(scalaTest, scalaMock) ++ akkaTest

  val testClientDeps = Seq(scalaTest, scalaMock, wireMock) ++ akkaTest

  val serverDeps = Seq(scalaConfigs, slack, guavacache, typesafeConfig, serviceContainer,
    reflections) ++ akka ++ joda ++ logging ++ testDeps ++ opsGenie

  val clientDeps = akka ++ testClientDeps

  val overrides = Set(logging, typesafeConfig, joda)
}
