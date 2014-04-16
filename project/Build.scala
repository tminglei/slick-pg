import sbt._
import Keys._

object SlickPgBuild extends Build {

  lazy val distDictionary = file("./../quickfish/dist")
  lazy val commonSettings = Seq(
    organizationName := "slick-pg",
    organization := "com.github.tminglei",

    scalaVersion := "2.10.3",
    scalacOptions ++= Seq("-deprecation", "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:higherKinds",
      "-language:postfixOps"),

    resolvers += "local dist" at "file:///" + distDictionary.getAbsolutePath,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    publishTo := Some(Resolver.file("file",  distDictionary)),
//    publishTo <<= version { (v: String) =>
//      val nexus = "https://oss.sonatype.org/"
//      if (v.trim.endsWith("SNAPSHOT"))
//        Some("snapshots" at nexus + "content/repositories/snapshots")
//      else
//        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
//    },
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
  
  lazy val mainDependencies = Seq (
    "org.scala-lang" % "scala-reflect" % "2.10.3",
    "com.typesafe.slick" % "slick_2.10" % "2.1.0-SNAPSHOT",
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
    "junit" % "junit" % "4.11" % "test",
    "com.novocode" % "junit-interface" % "0.10" % "test"
  )

  lazy val coreSettings = Seq(
    name := "slick-pg_core",
    description := "Slick extensions for PostgreSQL - Core",
    version := "0.5.3",
    libraryDependencies := mainDependencies
  )

  lazy val slickPgSettings = Seq(
    version := "0.5.3",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  lazy val slickPgProject = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg",
      description := "Slick extensions for PostgreSQL",
      libraryDependencies := mainDependencies
    ) 
  ).dependsOn (slickPgCore)
   .aggregate (slickPgCore, slickPgJoda, slickPgJson4s, slickPgJts, slickPgPlayJson, slickPgSprayJson, slickPgThreeten, slickPgDate2)

  lazy val slickPgCore = Project(id = "slick-pg_core", base = file("./core"),
    settings = Project.defaultSettings ++ commonSettings ++ coreSettings)

  lazy val slickPgJoda = Project(id = "slick-pg_joda-time", base = file("./addons/joda-time"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_joda-time",
      description := "Slick extensions for PostgreSQL - joda time module",
      libraryDependencies := mainDependencies ++ Seq(
        "joda-time" % "joda-time" % "2.3",
        "org.joda" % "joda-convert" % "1.5"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgJson4s = Project(id = "slick-pg_json4s", base = file("./addons/json4s"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_json4s",
      description := "Slick extensions for PostgreSQL - json4s module",
      libraryDependencies := mainDependencies ++ Seq(
        "org.json4s" % "json4s-ast_2.10" % "3.2.6",
        "org.json4s" % "json4s-core_2.10" % "3.2.6",
        "org.json4s" % "json4s-native_2.10" % "3.2.6" % "test"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgJts = Project(id = "slick-pg_jts", base = file("./addons/jts"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_jts",
      description := "Slick extensions for PostgreSQL - jts module",
      libraryDependencies := mainDependencies ++ Seq(
        "com.vividsolutions" % "jts" % "1.13"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgPlayJson = Project(id = "slick-pg_play-json", base = file("./addons/play-json"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_play-json",
      description := "Slick extensions for PostgreSQL - play-json module",
      libraryDependencies := mainDependencies ++ Seq(
        "com.typesafe.play" % "play-json_2.10" % "2.2.1"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgSprayJson = Project(id = "slick-pg_spray-json", base = file("./addons/spray-json"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_spray-json",
      description := "Slick extensions for PostgreSQL - spray-json module",
      libraryDependencies := mainDependencies ++ Seq(
        "io.spray" %%  "spray-json" % "1.2.5"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgThreeten = Project(id = "slick-pg_threeten", base = file("./addons/threeten"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_threeten",
      description := "Slick extensions for PostgreSQL - threeten module",
      libraryDependencies := mainDependencies ++ Seq(
        "org.threeten" % "threetenbp" % "0.8.1"
      )
    )
  ) dependsOn (slickPgCore)

  lazy val slickPgDate2 = Project(id = "slick-pg_date2", base = file("./addons/date2"),
    settings = Project.defaultSettings ++ commonSettings ++ slickPgSettings ++ Seq(
      name := "slick-pg_date2",
      description := "Slick extensions for PostgreSQL - date2 module (jdk8 time)",
      libraryDependencies := mainDependencies
    )
  ) dependsOn (slickPgCore)

}
