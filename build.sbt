lazy val commonSettings = Seq(
  organizationName := "slick-pg",
  organization := "com.github.tminglei",
  name := "slick-pg",
  version := "0.19.0",

  scalaVersion := "2.13.3",
  crossScalaVersions := Seq("2.13.3", "2.12.12", "2.11.12"),
  scalacOptions ++= Seq("-deprecation", "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials"),

  fork := true,
  javaOptions ++= Seq("-XX:MaxMetaspaceSize=512m"),

  resolvers += Resolver.mavenLocal,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",

  //    publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  makePomConfiguration := makePomConfiguration.value.withConfigurations(
    configurations = Vector(Compile, Runtime, Optional)
  ),

  pomExtra :=
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

def mainDependencies(scalaVersion: String) = {
  val extractedLibs = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")
    case _ =>
      Seq()
  }
  Seq (
    "org.scala-lang" % "scala-reflect" % scalaVersion,
    "com.typesafe.slick" %% "slick" % "3.3.2",
    "org.postgresql" % "postgresql" % "42.2.14",
    "org.slf4j" % "slf4j-simple" % "1.7.24" % "provided",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test"
  ) ++ extractedLibs
}

lazy val slickPgCore = (project in file("./core"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_core",
    description := "Slick extensions for PostgreSQL - Core",
    libraryDependencies := mainDependencies(scalaVersion.value)
  )

lazy val slickPg = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "slick-pg",
    description := "Slick extensions for PostgreSQL",
    libraryDependencies := mainDependencies(scalaVersion.value)
  )
  .dependsOn (slickPgCore)
  .aggregate (slickPgCore, slickPgJoda, slickPgJson4s, slickPgJts, slickPgJtsLt, slickPgPlayJson, slickPgSprayJson, slickPgCirceJson, slickPgArgonaut, slickPgJawn)

lazy val slickPgJoda = (project in file("./addons/joda-time"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_joda-time",
    description := "Slick extensions for PostgreSQL - joda time module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "joda-time" % "joda-time" % "2.10.5"
    )
  )
  .dependsOn (slickPgCore)
  
lazy val slickPgJson4s = (project in file("./addons/json4s"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_json4s",
    description := "Slick extensions for PostgreSQL - json4s module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "org.json4s" %% "json4s-ast" % "3.6.6",
      "org.json4s" %% "json4s-core" % "3.6.6",
      "org.json4s" %% "json4s-native" % "3.6.6" % "test"
    )
  ) 
  .dependsOn (slickPgCore)

lazy val slickPgJts = (project in file("./addons/jts"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jts",
    description := "Slick extensions for PostgreSQL - jts module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "com.vividsolutions" % "jts-core" % "1.14.0"
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgJtsLt = (project in file("./addons/jts_lt"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jts_lt",
    description := "Slick extensions for PostgreSQL - (locationtech) jts module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "org.locationtech.jts" % "jts-core" % "1.16.1"
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgPlayJson = (project in file("./addons/play-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_play-json",
    description := "Slick extensions for PostgreSQL - play-json module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "com.typesafe.play" %% "play-json" % "2.7.4"
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgSprayJson = (project in file("./addons/spray-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_spray-json",
    description := "Slick extensions for PostgreSQL - spray-json module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "io.spray" %%  "spray-json" % "1.3.5"
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgCirceJson = (project in file("./addons/circe-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_circe-json",
    description := "Slick extensions for PostgreSQL - circe module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor > 11 =>
          Seq(
            "io.circe" %% "circe-core" % "0.12.3",
            "io.circe" %% "circe-generic" % "0.12.3",
            "io.circe" %% "circe-parser" % "0.12.3"
          )
        case _ =>
          Seq(
            "io.circe" %% "circe-core" % "0.11.2",
            "io.circe" %% "circe-generic" % "0.11.2",
            "io.circe" %% "circe-parser" % "0.11.2"
          )
      }
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgArgonaut = (project in file("./addons/argonaut"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_argonaut",
    description := "Slick extensions for PostgreSQL - argonaut module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "io.argonaut" %% "argonaut" % "6.2.3"
    )
  )
  .dependsOn (slickPgCore)

lazy val slickPgJawn = (project in file("./addons/jawn"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jawn",
    description := "Slick extensions for PostgreSQL - jawn module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "org.typelevel" %% "jawn-ast" % "0.14.2"
    )
  )
  .dependsOn (slickPgCore)



