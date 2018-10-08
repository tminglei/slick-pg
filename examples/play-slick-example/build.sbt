name := "play-slick-pg"

version := "1.0dev"

PlayKeys.playOmnidoc := false

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.12.7"

description := "slick-pg play integration example project"

libraryDependencies ++= Seq(
  guice, specs2,
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.vividsolutions" % "jts" % "1.13",
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
  "com.github.tminglei" %% "slick-pg" % "0.15.3",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.15.3"
)

//resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
//
//routesGenerator := InjectedRoutesGenerator
//
//pipelineStages := Seq(digest)
