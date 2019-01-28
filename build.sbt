name := "scala-smaz"

version := "1.0.3-SNAPSHOT"

scalaVersion := "2.12.8"

crossScalaVersions ++= List("2.12.8", "2.11.12")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

organization := "com.scalableminds"

organizationName := "scalable minds UG (haftungsbeschränkt) & Co. KG"

organizationHomepage := Some(url("http://scalableminds.com"))

startYear := Some(2015)

sonatypeProfileName := "com.scalableminds"

description := "A small library to compress short strings using a dictionary lookup method."

licenses := List("MIT" -> url("https://github.com/tmbo/scala-smaz/blob/master/LICENSE"))

homepage := Some(url("https://github.com/tmbo/scala-smaz"))

scmInfo := Some(ScmInfo(url("https://github.com/tmbo/scala-smaz"), "https://github.com/tmbo/scala-smaz.git"))

pomExtra := <developers>
  <developer>
    <id>tmbo</id>
    <name>Tom Bocklisch</name>
    <email>tom.bocklisch@scalableminds.com</email>
    <url>http://github.com/tmbo</url>
  </developer>
</developers>

libraryDependencies ++= List(
  "org.specs2" %% "specs2-core" % "4.4.1" % Test,
  "org.apache.logging.log4j" % "log4j-api" % "2.11.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.1"
  )
