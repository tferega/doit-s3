organization := "hr.element.doit"

name         := "doit-s3"

version      := "0.0.3"

scalaVersion := "2.10.4"

credentials += Credentials(Path.userHome / ".config" / "doit-s3" / "nexus.config")

publishArtifact in (Compile, packageDoc) := false

publishTo := Some(if (version.value endsWith "SNAPSHOT")
    "Element Snapshots" at "http://repo.element.hr/nexus/content/repositories/snapshots/"
  else
    "Element Releases"  at "http://repo.element.hr/nexus/content/repositories/releases/"
)

libraryDependencies ++= Seq (
  "org.scalaz" %% "scalaz-core" % "6.0.4"
, "commons-io" % "commons-io" % "2.4"
, "com.amazonaws" % "aws-java-sdk" % "1.8.3" % "provided"
)

unmanagedSourceDirectories in Test := Nil

unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value)

initialCommands := "import hr.element.doit._; import s3._; import statistics._;"
