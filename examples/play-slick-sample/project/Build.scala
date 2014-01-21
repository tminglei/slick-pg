import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play-slick-sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc, filters,
    "org.scala-lang" % "scala-reflect" % "2.10.3",
    "com.typesafe.slick" % "slick_2.10" % "2.0.0-RC1",
    "com.typesafe.play" % "play-slick_2.10" % "0.5.0.9-SNAPSHOT",
    "com.github.tminglei" % "slick-pg_2.10.3" % "0.5.0-RC1",
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.5",
    "com.typesafe.play" % "play-json_2.10" % "2.2.1",
    "com.vividsolutions" % "jts" % "1.13"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    scalaVersion := "2.10.3"
    // Add your own project settings here      
  )

}
