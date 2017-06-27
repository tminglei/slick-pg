// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Add sbt PGP Plugin
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.2")
