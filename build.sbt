
name         := "doit-S3"

version      := "0.0.2"

organization := "hr.element.doit"

scalaVersion := "2.9.1"

credentials += Credentials(Path.userHome / ".publish" / "element.credentials")

publishArtifact in (Compile, packageDoc) := false

publishTo <<= (version) { version => Some(
  if (version.endsWith("SNAPSHOT"))
    "Element Snapshots" at "http://maven.element.hr/nexus/content/repositories/snapshots/"
  else
    "Element Releases"  at "http://maven.element.hr/nexus/content/repositories/releases/"
)}

libraryDependencies ++= Seq (
    "org.scalaz" %% "scalaz-core" % "6.0.4"
  , "commons-io" % "commons-io" % "2.1"
  , "com.amazonaws" % "aws-java-sdk" % "1.2.15"
  )

unmanagedSourceDirectories in Test := Nil

unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)( _ :: Nil)
