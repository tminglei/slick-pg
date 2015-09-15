name := "play-slick-pg"

version := "1.0dev"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

description := "slick-pg play integration example project"

libraryDependencies ++= Seq(
  specs2,
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.0",
  "com.github.tminglei" %% "slick-pg" % "0.9.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.vividsolutions" % "jts" % "1.13"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

pipelineStages := Seq(digest)
