name := "play-slick-pg"

version := "1.0dev"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.11.8"

description := "slick-pg play integration example project"

libraryDependencies ++= Seq(
  specs2,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.vividsolutions" % "jts" % "1.13",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.github.tminglei" %% "slick-pg" % "0.12.1",
  "com.github.tminglei" %% "slick-pg_date2" % "0.12.1",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.12.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

pipelineStages := Seq(digest)
