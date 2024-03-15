val scala212 = "2.12.19"
val scala213 = "2.13.13"
val scala3 = "3.3.1"

lazy val commonSettings = Seq(
  organizationName := "slick-pg",
  organization := "com.github.tminglei",
  name := "slick-pg",
  version := "0.22.0",

  scalaVersion := scala213,
  crossScalaVersions := Seq(scala212, scala213, scala3),
  scalacOptions ++= Seq("-deprecation", "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials"),

  fork := true,
  javaOptions ++= Seq("-XX:MaxMetaspaceSize=512m"),

  resolvers += Resolver.mavenLocal,
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
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
  (Test / publishArtifact) := false,
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
  val isScala3 = scalaVersion.startsWith("3")

  Seq (
    "org.scala-lang.modules" %% "scala-parser-combinators" % (if (isScala3) "2.3.0" else "1.1.2"),
    "dev.zio" %% "izumi-reflect" % "2.3.8",
    "com.typesafe.slick" %% "slick" % "3.5.0",
    "org.postgresql" % "postgresql" % "42.7.2",
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
    "org.slf4j" % "slf4j-simple" % "2.0.12" % "provided",
    "org.scalatest" %% "scalatest" % "3.2.18" % "test",
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.3" % "test",
    "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.3" % "test"
  ) ++ (if (isScala3) Nil else Seq("org.scala-lang" % "scala-reflect" % scalaVersion))
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
  .dependsOn (slickPgCore % "test->test;compile->compile")
  .aggregate (slickPgCore, slickPgJoda, slickPgJson4s, slickPgJts, slickPgJtsLt, slickPgPlayJson, slickPgSprayJson, slickPgCirceJson, slickPgArgonaut, slickPgJawn)

lazy val slickPgJoda = (project in file("./addons/joda-time"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_joda-time",
    description := "Slick extensions for PostgreSQL - joda time module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "joda-time" % "joda-time" % "2.12.7"
    )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgJson4s = (project in file("./addons/json4s"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_json4s",
    description := "Slick extensions for PostgreSQL - json4s module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "org.json4s" %% "json4s-ast" % "4.0.7",
      "org.json4s" %% "json4s-core" % "4.0.7",
      "org.json4s" %% "json4s-native" % "4.0.7" % "test"
    )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgJts = (project in file("./addons/jts"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jts",
    description := "Slick extensions for PostgreSQL - jts module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "com.vividsolutions" % "jts-core" % "1.14.0"
    )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgJtsLt = (project in file("./addons/jts_lt"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jts_lt",
    description := "Slick extensions for PostgreSQL - (locationtech) jts module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "org.locationtech.jts" % "jts-core" % "1.19.0"
    )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

def playJsonDependencies(scalaVersion: String) = {
  if (scalaVersion.startsWith("3")) Seq("org.playframework" %% "play-json" % "3.0.2")
  else Seq("com.typesafe.play" %% "play-json" % "2.10.4")
}
lazy val slickPgPlayJson = (project in file("./addons/play-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_play-json",
    description := "Slick extensions for PostgreSQL - play-json module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++
      playJsonDependencies(scalaVersion.value)
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgSprayJson = (project in file("./addons/spray-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_spray-json",
    description := "Slick extensions for PostgreSQL - spray-json module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++ Seq(
      "io.spray" %%  "spray-json" % "1.3.6"
    )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgCirceJson = (project in file("./addons/circe-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_circe-json",
    description := "Slick extensions for PostgreSQL - circe module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++
      Seq(
        "io.circe" %% "circe-core" % "0.14.6",
        "io.circe" %% "circe-generic" % "0.14.6",
        "io.circe" %% "circe-parser" % "0.14.6"
      )
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgUPickleJson = (project in file("./addons/upickle-json"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_upickle-json",
    description := "Slick extensions for PostgreSQL - uPickle module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++
      Seq("com.lihaoyi" %% "ujson" % "3.1.4")
  )
  .dependsOn(slickPgCore % "test->test;compile->compile")

lazy val slickPgArgonaut = (project in file("./addons/argonaut"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_argonaut",
    description := "Slick extensions for PostgreSQL - argonaut module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++
      Seq("io.argonaut" %% "argonaut" % "6.3.9")
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")

lazy val slickPgJawn = (project in file("./addons/jawn"))
  .settings(commonSettings)
  .settings(
    name := "slick-pg_jawn",
    description := "Slick extensions for PostgreSQL - jawn module",
    libraryDependencies := mainDependencies(scalaVersion.value) ++
      Seq("org.typelevel" %% "jawn-ast" % "1.5.1")
  )
  .dependsOn (slickPgCore % "test->test;compile->compile")
