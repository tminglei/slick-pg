import sbt._
import Keys._
import play.PlayImport.PlayKeys._

object ApplicationBuild extends Build {

  lazy val appDependencies = Seq(
    "com.typesafe.play" %% "play-jdbc" % "2.3.1",
    "com.typesafe.play" %% "play-json" % "2.3.1",
    "com.typesafe.play" %% "play-slick" % "0.8.0-RC2",
    "com.github.tminglei" %% "slick-pg" % "0.6.0-M2",
    "com.github.tminglei" %% "slick-pg_joda-time" % "0.6.0-M2",
    "com.github.tminglei" %% "slick-pg_play-json" % "0.6.0-M2",
    "com.github.tminglei" %% "slick-pg_jts" % "0.6.0-M2",
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.5",
    "com.vividsolutions" % "jts" % "1.13"
  )

  lazy val main = Project(id = "play-slick-pg", base = file(".")).enablePlugins(play.PlayScala).settings(
    libraryDependencies ++= appDependencies,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:higherKinds",
      "-language:postfixOps"
    )
  )
}
