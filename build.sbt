name := "scala-smaz"

version := "0.9-SNAPSHOT"

scalaVersion := "2.11.2"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

organization := "com.scalableminds"

organizationName := "scalable minds UG (haftungsbeschränkt) & Co. KG"

organizationHomepage := Some(url("http://scalableminds.com"))

startYear := Some(2015)

sonatypeProfileName := "com.scalableminds"

description := "A small library to compress short strings using a dictionary lookup method."

licenses := Seq("MIT" -> url("https://github.com/tmbo/scala-smaz/blob/master/LICENSE"))

homepage := Some(url("https://github.com/tmbo/scala-smaz"))

scmInfo := Some(ScmInfo(url("https://github.com/tmbo/scala-smaz"), "https://github.com/tmbo/scala-smaz.git"))

pomExtra := (
  <developers>
    <developer>
      <id>tmbo</id>
      <name>Tom Bocklisch</name>
      <email>tom.bocklisch@scalableminds.com</email>
      <url>http://github.com/tmbo</url>
    </developer>
  </developers>
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.6.5" % "test"
)