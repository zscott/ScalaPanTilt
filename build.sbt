enablePlugins(JavaServerAppPackaging)

name := "scala-pantilt"

version := "1.0"

organization := "io.synaptix"

resolvers ++=
  Seq(Resolver.typesafeRepo("releases"),
    "Spray Repository" at "http://repo.spray.io")

libraryDependencies ++= {
  val akkaVersion = "2.4.1"
  val sprayVersion = "1.3.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )
}