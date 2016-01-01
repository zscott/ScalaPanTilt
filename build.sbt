

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
    "io.spray" %% "spray-json" %  sprayJsonVersion,
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
  enablePlugins(JavaServerAppPackaging, SbtTwirl).
  settings(commonSettings: _*).
  settings(
    name := "scala-pantilt",
    version := "1.0",
    resolvers ++= standardResolvers,
    libraryDependencies ++= dependencies
  )



