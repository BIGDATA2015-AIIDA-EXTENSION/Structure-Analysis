name := "Structure Parser"

version := "1.0"

scalaVersion := "2.10.4" // cluster version of scala

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.8"

resourceDirectory in Compile := baseDirectory.value / "resources"

resourceDirectory in Test := baseDirectory.value / "resources"
