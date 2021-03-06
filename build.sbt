name := "structure-analysis"

version := "1.0"

scalaVersion := "2.10.4" // cluster version of scala

resourceDirectory in Compile := baseDirectory.value / "resources"

resourceDirectory in Test := baseDirectory.value / "resources"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.8",
  "org.apache.spark" %% "spark-core" % "1.2.1" % "provided",
  "org.apache.spark" %% "spark-mllib" % "1.2.1" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
)

scalacOptions ++= Seq(
  "-Xlint",
  "-feature",
  "-deprecation",
  "-unchecked"
)
