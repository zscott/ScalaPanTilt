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
  dockerExposedPorts := Seq(8888, 5000)
  dockerCommands ++= Seq(
    Cmd("MAINTAINER", "zach.mobile@gmail.com")
  )
}

/*
FROM node:5-wheezy
MAINTAINER Zach Scott <zach.mobile@gmail.com>

RUN echo 'will expose port 8888'
EXPOSE 8888

RUN echo 'copying files'
COPY app app/

RUN cd app && npm install

CMD cd app && ./start.sh

 */



