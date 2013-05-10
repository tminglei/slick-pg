import sbt._
import Keys._

object CoreBuild extends Build {

  lazy val sharedSettings = Seq(
    libraryDependencies := Seq(
      "com.typesafe.slick" % "slick_2.10" % "1.0.0",
      "com.vividsolutions" % "jts" % "1.13",
      "javax.transaction" % "jta" % "1.1",
      "joda-time" % "joda-time" % "2.1",
      "org.joda" % "joda-convert" % "1.2",
      "postgresql" % "postgresql" % "9.2-1002.jdbc4",
      "org.slf4j" % "slf4j-api" % "1.7.2",
      "org.slf4j" % "slf4j-simple" % "1.7.2" % "test",
      "junit" % "junit" % "4.11" % "test",
      "org.scalatest" % "scalatest_2.10" % "2.0.M6-SNAP16" % "test"
    ),
    resolvers += Resolver.mavenLocal,
    organizationName := "org.slick",
    organization := "org.slick",
    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
    publishMavenStyle := true,
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps")
  )

  lazy val root = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      publishArtifact := false
    ))

}
