/** main project containing main source code depending on slick and codegen project */
lazy val root = (project in file("."))
  .settings(sharedSettings)
  .settings(slick := slickCodeGenTask.value) // register manual sbt command)
  .settings(sourceGenerators in Compile += slickCodeGenTask.taskValue) // register automatic code generation on every compile, remove for only manual use)
  .dependsOn(codegen)


/** codegen project containing the customized code generator */
lazy val codegen = project
  .settings(sharedSettings)
  .settings(libraryDependencies += "com.typesafe.slick" %% "slick-codegen" % "3.2.3")


// shared sbt config between main project and codegen project
lazy val sharedSettings = Seq(
  scalaVersion := "2.12.5",
  scalacOptions := Seq("-feature", "-unchecked", "-deprecation"),
  libraryDependencies ++= List(
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "org.postgresql" % "postgresql" % "42.2.5",
    "com.github.tminglei" %% "slick-pg" % "0.16.0",
    "com.github.tminglei" %% "slick-pg_play-json" % "0.16.0",
    "com.github.tminglei" %% "slick-pg_joda-time" % "0.16.0",
    "com.github.tminglei" %% "slick-pg_jts" % "0.16.0",
    "joda-time" % "joda-time" % "2.9.9",
    "org.joda" % "joda-convert" % "2.0.1",
    "com.vividsolutions" % "jts" % "1.13",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
)


// code generation task that calls the customized code generator
lazy val slick = taskKey[Seq[File]]("slick")
lazy val slickCodeGenTask = Def.task {
  val dir = sourceManaged.value
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  val outputDir = (dir / "slick").getPath // place generated files in sbt's managed sources folder
  toError(r.run("demo.CustomizedCodeGenerator", cp.files, Array(outputDir), s.log))
  val fname = outputDir + "/demo/Tables.scala"
  Seq(file(fname))
}