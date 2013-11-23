name := "fixturate"

version := "1.0-SNAPSHOT"

scalaVersion := "2.8.1"

libraryDependencies ++= Seq(
    "org.clapper" % "grizzled-slf4j_2.8.1" % "0.6.10",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "commons-beanutils" % "commons-beanutils" % "1.8.3",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" % "scalatest_2.8.1" % "1.8" % "test"
)

javacOptions ++= Seq("-source", "1.6")

compileOrder in Compile := CompileOrder.ScalaThenJava

compileOrder in Test := CompileOrder.JavaThenScala
