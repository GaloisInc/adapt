val scalaV = "2.11.8"   // Scala 2.12 requires JVM 1.8.0_111 or newer. Cannot count on other having that recent a version
val akkaV = "2.4.14"
val akkaHttpV = "10.0.0"

lazy val adapt = (project in file(".")).settings(
  name := "adapt",
  version := "0.1",
  organization := "com.galois",
  scalaVersion := scalaV,
//  scalacOptions := Seq(
//    "-unchecked",
//    "-deprecation",
//    "-feature",
//    "-Yno-adapted-args",
//    "-Ywarn-dead-code",
//    "-Ywarn-numeric-widen",
//    "-Ywarn-value-discard",
//    "-Ywarn-unused"
//  ),

  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
    "org.scalatest" %% "scalatest" % "3.0.0", // % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "org.apache.avro" % "avro" % "1.8.1",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
    //  "org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.2.3",
    //  "org.slf4j" % "slf4j-simple" % "1.7.21",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    // "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
    // "com.github.scopt" %% "scopt" % "3.5.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"
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
  mainClass in assembly := Some("com.galois.adapt.Application"),

  // Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
  logBuffered in Test := false,

//  mainClass in Test := Some("com.galois.adapt.scepter.SimpleTestRunner"),

  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }

)

lazy val scepter = (project in file("scepter")).settings(
  name := "scepter",
  version := "0.1",
  organization := "com.galois",
  scalaVersion := scalaV,

  libraryDependencies ++= Seq(
    // "org.scalaj" %% "scalaj-http" % "2.3.0",
    "com.github.scopt" %% "scopt" % "3.5.0"
  ),
  mainClass in (Compile, run) := Some("com.galois.adapt.scepter.Wrapper"),
  mainClass in assembly := Some("com.galois.adapt.scepter.Wrapper")
)
