val scalaV = "2.11.8"   // Scala 2.12 requires JVM 1.8.0_111 or newer. Cannot count on other having that recent a version
val akkaV = "2.4.14"
val akkaHttpV = "10.0.0"

lazy val adapt = (project in file(".")).settings(
  name := "adapt",
  version := "0.1",
  organization := "com.galois",
  scalaVersion := scalaV,

  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
    "org.scalatest" %% "scalatest" % "3.0.0", // % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "org.apache.avro" % "avro" % "1.8.1",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
    //  "org.slf4j" % "slf4j-simple" % "1.7.21",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
    //  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
  ),

  {
  // Compile Avro schema at the command line with `sbt avroCompile`
    lazy val avroCompile = taskKey[Unit]("Compile Avro sources from the schema")
    val avroToolsJarPath = "lib/avro-tools-1.8.1.jar"
    val avroSpecPath = "src/main/avro/TCCDMDatum13.avsc"
    avroCompile := s"java -jar $avroToolsJarPath compile schema $avroSpecPath target/scala-2.11/src_managed/main/".!
  },

  // Run the Ingest main class at the command line with `sbt run`
  //mainClass in (Compile, run) := Some("com.galois.adapt.scepter.SimpleTestRunner") //Some("com.galois.adapt.Ingest")
  mainClass in assembly := Some("com.galois.adapt.scepter.SimpleTestRunner"), //Some("com.galois.adapt.Ingest")

  // Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
  logBuffered in Test := false,

//  mainClass in Test := Some("com.galois.adapt.scepter.SimpleTestRunner"),

  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  },

  org.softnetwork.sbt.plugins.GroovyPlugin.groovy.settings

)

lazy val scepter = (project in file("scepter")).settings(
//  libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0",
  mainClass in (Compile, run) := Some("com.galois.adapt.scepter.Wrapper"),
  mainClass in assembly := Some("com.galois.adapt.scepter.Wrapper"),
  scalaVersion := scalaV
)