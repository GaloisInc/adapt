name := "AdaptJVM"

version := "0.1"

organization := "com.galois"

scalaVersion := "2.11.8"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)  // for avro-scala-macro-annotations

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
//  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.apache.avro" % "avro" % "1.8.1",
//  "com.julianpeeters" % "avro-scala-macro-annotations_2.11" % "0.10.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
//  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "jline" % "jline" % "2.12.1"   // Exists here to resolve ambiguous dependency chains
)

// Compile Avro schema at the command line with `sbt avroCompile`
lazy val avroCompile = taskKey[Unit]("Compile Avro sources from the schema")
val avroToolsJarPath = "lib/avro-tools-1.8.1.jar"
val avroSpecPath = "src/main/avro/TCCDMDatum13.avsc"
avroCompile := s"java -jar $avroToolsJarPath compile schema $avroSpecPath target/scala-2.11/src_managed/main/".!

// Run the Ingest main class at the command line with `sbt run`
mainClass in (Compile, run) := Some("com.galois.adapt.Ingest")

// Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
logBuffered in Test := false
