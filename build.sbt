import com.typesafe.sbt.packager.docker._

lazy val standardResolvers = Seq(
  Resolver.typesafeRepo("releases"),
  "Spray Repository" at "http://repo.spray.io"
)

lazy val dependencies = {
  val akkaVersion = "2.4.1"
  val sprayVersion = "1.3.3"
  val sprayJsonVersion = "1.3.2"
  val scalatestVersion = "2.2.4"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % sprayJsonVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  )
}

lazy val commonSettings = Seq(
  organization := "io.synaptix"
)

lazy val root = Project("scala-pantilt", file(".")).
  enablePlugins(JavaServerAppPackaging, SbtTwirl, DockerPlugin).
  settings(commonSettings: _*).
  settings(
    name := "scala-pantilt",
    version := "1.0",
    resolvers ++= standardResolvers,
    libraryDependencies ++= dependencies,
    sources in(Compile, doc) := Seq.empty,
    publishArtifact in(Compile, packageDoc) := false
  ).
  settings(dockerSettings: _*)

lazy val dockerSettings = {
  dockerExposedPorts := Seq(8888, 5000, 2550)
  dockerCommands ++= Seq(
    Cmd("MAINTAINER", "zach.mobile@gmail.com")
  )
}

