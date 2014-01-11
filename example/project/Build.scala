import sbt._
import Keys._

object ExampleBuild extends Build {

  lazy val theSettings = Seq(
    name := "example",
    description := "slick-pg example project",
    version := "0.5-SNAPSHOT",
    organizationName := "slick-pg",
    organization := "com.example",

    scalaVersion := "2.10.3",
    scalaBinaryVersion <<= scalaVersion,
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps"),

    libraryDependencies := Seq(
      "com.typesafe.slick" % "slick_2.10" % "2.0.0-RC1",
      "com.github.tminglei" % "slick-pg_2.10.3" % "0.5.0-RC1",
      "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
      "com.vividsolutions" % "jts" % "1.13",
      "org.json4s" % "json4s-native_2.10" % "3.2.6"
    ),
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("snapshots"),

    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
    publishMavenStyle := true
  )

  lazy val root = Project(id = "example", base = file("."),
    settings = Project.defaultSettings ++ theSettings ++ Seq(
      publishArtifact := false
    ))
}
