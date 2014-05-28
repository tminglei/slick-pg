organization := "com.github.tminglei"

version := "0.1.0-SNAPSHOT"

name := "slick-pg"

description := "Slick extensions for PostgreSQL"

homepage := Some(url("https://github.com/tminglei/slick-pg"))

scalaVersion := "2.10.4"

sbtVersion := "0.13.2"
    
licenses := Seq("Two-clause BSD-style license" -> url("http://github.com/tminglei/slick-pg/blob/master/LICENSE.txt"))

pomExtra := (
  <scm>
      <url>git@github.com:tminglei/slick-pg.git</url>
      <connection>scm:git:git@github.com:tminglei/slick-pg.git</connection>
  </scm>
)

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "2.0.0",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "com.vividsolutions" % "jts" % "1.13",
  "org.threeten" % "threetenbp" % "0.8.1",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "io.spray" %% "spray-json" % "1.2.5",
  "org.json4s" %% "json4s-ast" % "3.2.6",
  "org.json4s" %% "json4s-core" % "3.2.6",
  "org.json4s" %% "json4s-native" % "3.2.6" % "test",
  "junit" % "junit" % "4.11" % "test"
)

publishTo := {
  Some("releases" at "http://repo.typesafe.com/typesafe/maven-releases")
}
