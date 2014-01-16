import sbt._
import Keys._

object SlickPgBuild extends Build {

  lazy val commonSettings = Seq(
    organizationName := "slick-pg",
    organization := "com.github.tminglei",

    scalaVersion := "2.10.3",
    scalaBinaryVersion <<= scalaVersion,
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:higherKinds",
      "-language:postfixOps"),

//    resolvers += Resolver.mavenLocal,
//    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
//    publishMavenStyle := true,

    resolvers += Resolver.sonatypeRepo("snapshots"),
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

  lazy val coreSettings = Seq(
    name := "slick-pg_core",
    description := "Slick extensions for PostgreSQL - Core",
    version := "0.2.5",
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-reflect" % "2.10.3",
      "com.typesafe.slick" % "slick_2.10" % "1.0.1",
      "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
      "com.vividsolutions" % "jts" % "1.13",
      "junit" % "junit" % "4.11" % "test",
      "com.novocode" % "junit-interface" % "0.10" % "test"
    )
  )

  lazy val slickPgSettings = Seq(
    name := "slick-pg",
    description := "Slick extensions for PostgreSQL",
    version := "0.2.5",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-reflect" % "2.10.3",
      "com.typesafe.slick" % "slick_2.10" % "1.0.1",
      "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
      "com.vividsolutions" % "jts" % "1.13",
      "org.threeten" % "threetenbp" % "0.8.1",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.5",
      "com.typesafe.play" % "play-json_2.10" % "2.2.1",
      "org.json4s" % "json4s-ast_2.10" % "3.2.6",
      "org.json4s" % "json4s-core_2.10" % "3.2.6",
      "org.json4s" % "json4s-native_2.10" % "3.2.6" % "test",
      "junit" % "junit" % "4.11" % "test",
      "com.novocode" % "junit-interface" % "0.10" % "test"
    )
  )

  lazy val slickPgProject = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings
  ) dependsOn (slickPgCore) aggregate (slickPgCore)

  lazy val slickPgCore = Project(id = "slick-pg_core", base = file("./core"),
    settings = Project.defaultSettings ++ commonSettings ++ coreSettings)

}
