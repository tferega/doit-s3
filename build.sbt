
name         := "doit-S3"

version      := "0.0.1"

organization := "hr.element"

scalaVersion := "2.9.1"

credentials += Credentials(Path.userHome / ".publish" / "element.credentials")

publishArtifact in (Compile, packageDoc) := false

publishTo <<= (version) { version => Some(
  if (version.endsWith("SNAPSHOT"))
    "Element Private Snapshots" at "http://maven.element.hr/nexus/content/repositories/snapshots-private/"
  else
    "Element Private Releases"  at "http://maven.element.hr/nexus/content/repositories/releases-private/"
)}

libraryDependencies ++= Seq (
    "org.scalaz" %% "scalaz-core" % "6.0.4"
  , "commons-io" % "commons-io" % "2.1"
  , "com.amazonaws" % "aws-java-sdk" % "1.2.15"
  )

