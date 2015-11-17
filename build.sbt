organization := "org.gardner"

name := "music"

version := "0.1"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-target", "1.7")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "17.0",
  // Testing dependencies
  "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
