name := "Structure Parser"

version := "1.0"

scalaVersion := "2.10.4" // cluster version of scala

libraryDependencies += "com.typesafe.play" % "play-json_2.10" % "2.4.0-M3"

resourceDirectory in Compile := baseDirectory.value / "resources"

resourceDirectory in Test := baseDirectory.value / "resources"
