name := "slick-pg"

version := "0.5.3"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe.slick" %% "slick" % "2.0.1",
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

unmanagedSourceDirectories in Compile += baseDirectory.value / "core" / "src" / "main" / "scala"
