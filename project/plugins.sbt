// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// PostgreSQL Driver repository
resolvers += "Adam Gent Maven Repository" at "http://mvn-adamgent.googlecode.com/svn/maven/release"

// Add sbt idea plugin
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.4.0")

// Add sbt eclipse plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1")

// Add sbt PGP Plugin
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")
