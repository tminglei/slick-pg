import sbt.Keys._
import sbt._

object SlickPgBuild extends Build {

  lazy val commonSettings = Seq(
    organizationName := "slick-pg",
    organization := "com.github.tminglei",
    name := "slick-pg",
    version := "0.9.0-beta",

    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:higherKinds",
      "-language:postfixOps",
      "-language:existentials"),

    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "spray" at "http://repo.spray.io/",
//    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    makePomConfiguration ~= { _.copy(configurations = Some(Seq(Compile, Runtime, Optional))) },

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
      </developers>
    )
  )

  def mainDependencies(scalaVersion: String) = {
    val extractedLibs = CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1" % "provided")
      case _ =>
        Seq()
    }
    Seq (
      "org.scala-lang" % "scala-reflect" % scalaVersion,
      "com.typesafe.slick" %% "slick" % "3.0.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    ) ++ extractedLibs
  }

  lazy val slickPgCore = Project(id = "slick-pg_core", base = file("./core"),
    settings = Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-pg_core",
      description := "Slick extensions for PostgreSQL - Core",
      libraryDependencies := mainDependencies(scalaVersion.value)
    )
  )

  val json4sVersion = "3.2.10"
  lazy val slickPgProject = Project(id = "slick-pg", base = file("."),
    settings = Defaults.coreDefaultSettings ++ commonSettings ++ Seq(
      name := "slick-pg",
      description := "Slick extensions for PostgreSQL",
      libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
        "joda-time" % "joda-time" % "2.4" % "provided",
        "org.joda" % "joda-convert" % "1.7" % "provided",
        "org.threeten" % "threetenbp" % "1.0" % "provided",
        "org.json4s" %% "json4s-ast" % json4sVersion % "provided",
        "org.json4s" %% "json4s-core" % json4sVersion % "provided",
        "org.json4s" %% "json4s-native" % json4sVersion % "test",
        "com.typesafe.play" %% "play-json" % "2.3.0" % "provided",
        "io.spray" %%  "spray-json" % "1.3.1" % "provided",
        "io.argonaut" %% "argonaut" % "6.0.4" % "provided",
        "com.vividsolutions" % "jts" % "1.13" % "provided"
      )
    )
  ).dependsOn (slickPgCore)
    .aggregate (slickPgCore)
}
