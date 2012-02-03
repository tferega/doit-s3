
name         := "doit-S3"

version      := "0.0.1"

organization := "hr.element"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq (
    "org.scalaz" %% "scalaz-core" % "6.0.4"
  , "commons-io" % "commons-io" % "2.1"
  , "com.amazonaws" % "aws-java-sdk" % "1.2.15"
  )

