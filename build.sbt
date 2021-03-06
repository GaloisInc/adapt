val scalaV = "2.11.12"   // "2.12.2"  // Scala 2.12 requires JVM 1.8.0_111 or newer.
val akkaV = "2.5.17"
val akkaHttpV = "10.1.5"
val neoV = "3.3.3"

resolvers += Resolver.jcenterRepo  // for akka persistence in memory

resolvers += Resolver.mavenLocal  // for BBN repositories built locally

lazy val adapt = (project in file(".")).settings(
  name := "adapt",
  version := "0.6.2",
  organization := "com.galois",
  scalaVersion := scalaV,

  scalacOptions += "-target:jvm-1.8",

  autoScalaLibrary := false,
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-library" % scalaV,
//    "com.typesafe" % "config" % "1.3.1",
    "com.github.pureconfig" %% "pureconfig" % "0.9.2",
    "org.scalatest" %% "scalatest" % "3.0.0", // % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "org.apache.avro" % "avro" % "1.8.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
    "org.mapdb" % "mapdb" % "3.0.7",
    "com.github.alexandrnikitin" %% "bloom-filter" % "0.10.1",
    "org.neo4j" % "neo4j-community" % neoV,
    "org.neo4j" % "neo4j-cypher" % neoV,
    "org.neo4j" % "neo4j-tinkerpop-api" % "0.1",
    "org.neo4j" % "neo4j-tinkerpop-api-impl" % "0.7-3.2.3" exclude("org.neo4j", "neo4j-enterprise"),
    "org.apache.tinkerpop" % "neo4j-gremlin" % neoV,
    "org.apache.tinkerpop" % "tinkergraph-gremlin" % neoV,



//  , "com.bbn" % "tc-avro" % "1.0-SNAPSHOT"
    "commons-io" % "commons-io" % "2.6",
    "com.univocity" % "univocity-parsers" % "2.6.1",
    "com.github.felfert" % "cidrutils" % "1.1",  // For testing IP address ranges in the policy enforcement demo.

    "com.lihaoyi" % "ammonite-sshd" % "1.1.2" cross CrossVersion.full
  ),

//  fork in run := true,
//  javaOptions in run ++= Seq("-Xmx6G"),

  {
  // Compile Avro schema at the command line with `sbt avroCompile`
    lazy val avroCompile = taskKey[Unit]("Compile Avro sources from the schema")
    val avroToolsJarPath = "lib/avro-tools-1.8.2.jar"
    val avroSpecPath = "src/main/avro/TCCDMDatum14.avdl"
    // TODO Now takes two commands to compile schema, check with Ryan on how to change build file...
    // java -jar lib/avro-tools-1.8.2.jar idl src/main/avro/CDM14.avdl src/main/avro/CDM14.avpr
    // java -jar lib/avro-tools-1.8.2.jar compile protocol src/main/avro/CDM14.avpr src/main/java/
    avroCompile := s"java -jar $avroToolsJarPath compile schema $avroSpecPath target/scala-2.11/src_managed/main/".!
  },

  // Run the Ingest main class at the command line with `sbt run`
  //mainClass in (Compile, run) := Some("com.galois.adapt.scepter.SimpleTestRunner") //Some("com.galois.adapt.Ingest")
  mainClass in assembly := Some("com.galois.adapt.Application"),

  // Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
  logBuffered in Test := false,

  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
//    case PathList("META-INF", "services" /*, "org.neo4j.kernel.extension.KernelExtensionFactory"*/) => MergeStrategy.first
    case PathList("META-INF", xs @ _*) => xs.map(_.toLowerCase) match {
      case "services" :: rfqdn :: Nil => MergeStrategy.first
      case list if list.exists(_.contains("neo4j")) => MergeStrategy.first
      case _ => MergeStrategy.discard
    }
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
