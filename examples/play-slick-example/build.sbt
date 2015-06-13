name := "play-slick-pg"

version := "1.0dev"

scalaVersion := "2.11.6"

description := "slick-pg play integration example project"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-jdbc" % "2.4.0",
  "com.typesafe.play" %% "play-json" % "2.4.0",
  "com.typesafe.play" %% "play-slick" % "1.0.0",
  "com.typesafe.play" %% "play-specs2" % "2.4.0",
  "com.github.tminglei" %% "slick-pg" % "0.9.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.vividsolutions" % "jts" % "1.13"
)

resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayScala)