import sbt._
import Keys._

object SlickPgBuild extends Build {

  lazy val theSettings = Seq(
    name := "slick-pg",
    description := "Slick extensions for PostgreSQL",
    version := "0.1.0-SNAPSHOT",
    organizationName := "slick-pg",
    organization := "com.github.slickpg",

    scalaVersion := "2.10.1",
    scalaBinaryVersion <<= scalaVersion,
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps"),
    libraryDependencies := Seq(
      "com.typesafe.slick" % "slick_2.10" % "1.0.0",
      "com.vividsolutions" % "jts" % "1.13",
      "postgresql" % "postgresql" % "9.2-1002.jdbc4",
      "junit" % "junit" % "4.11" % "test",
      "com.novocode" % "junit-interface" % "0.10-M4" % "test"
    ),

    resolvers += Resolver.mavenLocal,
    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
    publishMavenStyle := true,

//    resolvers += Resolver.sonatypeRepo("snapshots"),
//    publishTo <<= version { (v: String) =>
//      val nexus = "https://oss.sonatype.org/"
//      if (v.trim.endsWith("SNAPSHOT"))
//        Some("snapshots" at nexus + "content/repositories/snapshots")
//      else
//        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
//    },
//    publishMavenStyle := true,
//    publishArtifact in Test := false,
//    pomIncludeRepository := { _ => false },
//    makePomConfiguration ~= { _.copy(configurations = Some(Seq(Compile, Runtime, Optional))) },

    pomExtra := (
      <url>https://github.com/tminglei/slick-pg</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:tminglei/slick-pg.git</url>
        <connection>scm:git:git@github.com:tminglei/slick-pg.git</connection>
      </scm>
      <developers>
        <developer>
          <id>tminglei</id>
          <name>Minglei Tu</name>
          <timezone>+8</timezone>
        </developer>
      </developers>)
  )

  lazy val slickPgProject = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ theSettings)

}
