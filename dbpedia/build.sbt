val scalaV = "2.11.11"   // "2.12.2"  // Scala 2.12 requires JVM 1.8.0_111 or newer.
val akkaV = "2.4.19" //"2.5.6"
val akkaHttpV = "10.0.10"
val neoV = "3.3.0"

//resolvers += Resolver.jcenterRepo  // for akka persistence in memory
resolvers += Resolver.sonatypeRepo("snapshots")  // for scala-pickling 0.10.2-SNAPSHOT  // for Quine
resolvers += Resolver.mavenLocal 

name := "dpbedia"
version := "0.0.1"
organization := "com.galois"
scalaVersion := scalaV

autoScalaLibrary := false
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaV,
  "com.typesafe" % "config" % "1.3.1",
  "org.scalatest" %% "scalatest" % "3.0.0", // % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.apache.avro" % "avro" % "1.8.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "org.mapdb" % "mapdb" % "3.0.5",
  "com.rrwright" %% "quine" % "0.1-SNAPSHOT",
  "com.lihaoyi" % "ammonite-sshd" % "1.1.2" cross CrossVersion.full
)

// offline := true

//  fork in run := true,
//  javaOptions in run ++= Seq("-Xmx6G"),

mainClass in (Compile, run) := Some("com.galois.dbpedia.Application")
mainClass in assembly := Some("com.galois.dbpedia.Application")

  // Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
logBuffered in Test := false

