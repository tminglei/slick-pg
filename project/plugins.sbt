// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Add sbt PGP Plugin
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.2")
