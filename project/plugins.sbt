logLevel := Level.Warn

resolvers += Classpaths.typesafeReleases

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.1.1")
