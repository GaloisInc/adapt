name := "AdaptJVM"

version := "0.1"

organization := "com.galois"

scalaVersion := "2.11.8"

libraryDependencies := Seq(
  "org.apache.tinkerpop" % "gremlin-driver" % "3.2.3",
  "org.apache.avro" % "avro" % "1.8.1",
  "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "jline" % "jline" % "2.12.1"   // Exists here to resolve ambiguous dependency chains
)

// Compile Avro schema at the command line with `sbt avroCompile`
lazy val avroCompile = taskKey[Unit]("Compile Avro sources from the schema")
val avroToolsJarPath = "lib/avro-tools-1.8.1.jar"
val avroSpecPath = "src/main/avro/TCCDMDatum13.avsc"
avroCompile := s"java -jar $avroToolsJarPath compile schema $avroSpecPath target/scala-2.11/src_managed/main/".!

// Run the Ingest main class at the command line with `sbt run`
mainClass in (Compile, run) := Some("com.galois.adapt.Ingest")