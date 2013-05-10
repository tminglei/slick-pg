import sbt._
import Keys._

object CoreBuild extends Build {

  lazy val sharedSettings = Seq(
    version := "0.1.0-SNAPSHOT",
    organizationName := "slick",
    organization := "slick-pg",
    resolvers += Resolver.mavenLocal,
    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
    publishMavenStyle := true,
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps"),
    libraryDependencies := Seq(
      "com.typesafe.slick" % "slick_2.10" % "1.0.0",
      "com.vividsolutions" % "jts" % "1.13",
      "javax.transaction" % "jta" % "1.1",
      "postgresql" % "postgresql" % "9.2-1002.jdbc4",
      "org.slf4j" % "slf4j-api" % "1.7.2",
      "org.slf4j" % "slf4j-simple" % "1.7.2" % "test",
      "junit" % "junit" % "4.11" % "test",
      "org.scalatest" % "scalatest_2.10" % "2.0.M6-SNAP16" % "test",
      "joda-time" % "joda-time" % "2.1" % "test",
      "org.joda" % "joda-convert" % "1.2" % "test"
    ),
    licenses += ("Two-clause BSD-style license", url("http://github.com/tminglei/slick-pg/blob/master/LICENSE.txt")),
    pomExtra :=
      <developers>
        <developer>
          <id>tminglei</id>
          <name>Minglei Tu</name>
          <timezone>+8</timezone>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:tminglei/slick-pg.git</url>
        <connection>scm:git:git@github.com:tminglei/slick-pg.git</connection>
      </scm>
  )

  lazy val root = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      publishArtifact := true
    ))

}
