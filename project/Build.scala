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
  
  lazy val mainDependencies = Seq (
    "org.scala-lang" % "scala-reflect" % "2.10.3",
    "com.typesafe.slick" % "slick_2.10" % "2.0.0-RC1",
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
    "junit" % "junit" % "4.11" % "test",
    "com.novocode" % "junit-interface" % "0.10" % "test"
  )

  lazy val coreSettings = Seq(
    name := "slick-pg_core",
    description := "Slick extensions for PostgreSQL - Core",
    version := "0.5.0-RC1",
    libraryDependencies := mainDependencies
  )

  lazy val modulesSettings = Seq(
    version := "0.5.0-RC1",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
//      "com.vividsolutions" % "jts" % "1.13",
//      "org.threeten" % "threetenbp" % "0.8.1",
//      "joda-time" % "joda-time" % "2.3",
//      "org.joda" % "joda-convert" % "1.5",
//      "com.typesafe.play" % "play-json_2.10" % "2.2.1",
//      "org.json4s" % "json4s-ast_2.10" % "3.2.6",
//      "org.json4s" % "json4s-core_2.10" % "3.2.6",
//      "org.json4s" % "json4s-native_2.10" % "3.2.6" % "test",
//    )
  )

  lazy val slickPgProject = Project(id = "slick-pg", base = file("."),
    settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg",
      description := "Slick extensions for PostgreSQL",
      libraryDependencies := mainDependencies ++ Seq(
          "org.json4s" % "json4s-native_2.10" % "3.2.6" % "test"
      )
    ) 
  ).dependsOn(slickPgCore,slickPgCommon,slickPgJoda,slickPgJson4s,slickPgJts,slickPgPlayJson,slickPgThreeten)
   .aggregate (slickPgCore,slickPgCommon,slickPgJoda,slickPgJson4s,slickPgJts,slickPgPlayJson,slickPgThreeten)

  lazy val slickPgCore = Project(id = "slick-pg_core", base = file("./core"),
    settings = Project.defaultSettings ++ commonSettings ++ coreSettings)

  lazy val slickPgCommon = Project(id = "slick-pg-common", base = file("./modules/common"),
    settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg-common",
      description := "Slick extensions for PostgreSQL - common module",
      libraryDependencies := mainDependencies
    )
  ) dependsOn (slickPgCore) aggregate (slickPgCore)
      
  lazy val slickPgJoda = Project(id = "slick-pg_joda-time", base = file("./modules/joda-time"),
    settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg_joda-time",
      description := "Slick extensions for PostgreSQL - joda time module",
      libraryDependencies := mainDependencies ++ Seq(
          "joda-time" % "joda-time" % "2.3",
          "org.joda" % "joda-convert" % "1.5"
      )
    )
  ).dependsOn(slickPgCommon)

  lazy val slickPgJson4s = Project(id = "slick-pg_json4s", base = file("./modules/json4s"),
  settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg_json4s",
      description := "Slick extensions for PostgreSQL - json4s module",
      libraryDependencies := mainDependencies ++ Seq(
          "org.json4s" % "json4s-ast_2.10" % "3.2.6",
          "org.json4s" % "json4s-core_2.10" % "3.2.6"
      )
    )
  ).dependsOn(slickPgCommon)

  lazy val slickPgJts = Project(id = "slick-pg_jts", base = file("./modules/jts"),
  settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg_jts",
      description := "Slick extensions for PostgreSQL - jts module",
      libraryDependencies := mainDependencies ++ Seq(
          "com.vividsolutions" % "jts" % "1.13"
      )
    )
  ).dependsOn(slickPgCommon)

  lazy val slickPgPlayJson = Project(id = "slick-pg_play-json", base = file("./modules/play-json"),
  settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg_play-json",
      description := "Slick extensions for PostgreSQL - play-json module",
      libraryDependencies := mainDependencies ++ Seq(
          "com.typesafe.play" % "play-json_2.10" % "2.2.1"
      )
    )
  ).dependsOn(slickPgCommon)

  lazy val slickPgThreeten = Project(id = "slick-pg_threeten", base = file("./modules/threeten"),
  settings = Project.defaultSettings ++ commonSettings ++ modulesSettings ++ Seq(
      name := "slick-pg_threeten",
      description := "Slick extensions for PostgreSQL - threeten module",
      libraryDependencies := mainDependencies ++ Seq(
          "org.threeten" % "threetenbp" % "0.8.1"
      )
    )
  ).dependsOn(slickPgCommon)
      
}
