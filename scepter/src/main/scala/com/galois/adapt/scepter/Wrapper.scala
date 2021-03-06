package com.galois.adapt.scepter

import java.io._
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.security.{DigestInputStream, MessageDigest}
import java.util.Scanner

import scala.util._
import sys.process._

import scopt._

/* Everytime it is run, this app compares its MD5 to 'adapt-tester.hash' it downloads. If it is not
 * the same, it downloads a new version of 'adapt-tester.jar' (checks the hash again) and gets the
 * new JAR started before exiting.
 *
 * Then, it downloads 'adapt.jar' and its hash, double-checking that these are consistent, and runs
 * it in acceptance test mode.
 */
object Wrapper extends App {

  // remote URLs
  val adaptDownloadUrl = "https://adapt.galois.com/acceptance_tests/current.jar"
  val testerDownloadUrl = "https://adapt.galois.com/acceptance_tests/current-tester.jar"
  val adaptHashUrl = "https://adapt.galois.com/acceptance_tests/current.hash"
  val testerHashUrl = "https://adapt.galois.com/acceptance_tests/current-tester.hash"

  // local JARs
  val adaptJarPath = "adapt.jar"
  val testerJarPath = Try {
    val p = classOf[Config].getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
    new FileInputStream(new File(p))
    p
  }.getOrElse("adapt-tester.jar")
  val temporaryJarPath = "temporary.jar"

  // Fetch a hash remotely
  def fetchHash(path: String): String = Try {
    val hashUrl = new URL(path)
    val scanner = new Scanner(hashUrl.openStream()).useDelimiter("\\A")
    return scanner.next().trim
  }.toOption.getOrElse(throw new Exception(s"Failed to get remote hash at $path."))

  // Compute a hash of a file
  // The output of this function should match the output of running "md5 -q <file>"
  def computeHash(path: String): String = Try {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")
    
    val dis = new DigestInputStream(new FileInputStream(new File(path)), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }
    
    md5.digest.map("%02x".format(_)).mkString
  }.toOption.getOrElse(throw new Exception(s"Failed to compute hash of $path."))

  // Download a file synchronously
  def downloadFile(downloadUrl: String, filePath: String): Unit = Try {
    val in = Channels.newChannel(new URL(downloadUrl).openStream)
    val out = new FileOutputStream(filePath).getChannel
    out.transferFrom(in, 0, Long.MaxValue)
  }.toOption.getOrElse(throw new Exception(s"Failed to download file from $downloadUrl."))

  Try {

    // Compare the hash of the current 'adapt-tester.jar' to the published one
    def testerStatedHash = fetchHash(testerHashUrl)
    def testerActualHash = computeHash(testerJarPath)

    // See if the hash should be checked. Note that if the command line arguments don't parse, the hash will be checked.
    val checkHash = Config.parser.parse(args, Config()) match {
      case Some(config) => !config.skipCheck
      case None => true
    }

    if (checkHash && testerStatedHash != testerActualHash) {
     
      // Update 'adapt-tester.jar' 
      print("Your version of 'adapt-tester.jar' is outdated. Downloading the new version... ")
      downloadFile(testerDownloadUrl, temporaryJarPath)
      val temporaryJar = new File(temporaryJarPath)
      if (temporaryJar.exists) temporaryJar.deleteOnExit()
      println("done.")

      // Check that this updated version of 'adapt-tester.jar' matches the newest hash
      val testerActualHash = computeHash(temporaryJarPath)
      require(
        testerStatedHash.equals(testerActualHash),
        s"""Hash comparison failed. The downloaded 'adapt-tester.jar' file is corrupt or there was an error on the server
           |  Stated hash:   $testerStatedHash
           |  Computed hash: $testerActualHash
           |""".stripMargin
      )
      Files.move(Paths.get(temporaryJarPath), Paths.get(testerJarPath), StandardCopyOption.REPLACE_EXISTING)
      
      // Re-run the java program (and stream its output to stdout)
      val cmd = s"java -jar $testerJarPath ${args.mkString(" ")}"
      println("Starting the updated 'adapt-tester.jar'. This may take a while...")
      cmd ! ProcessLogger(System.out.println, System.err.println)
    
    } else {

      // Process command-line arguments
      val opts = Config.parser.parse(args, Config()) match {
        case Some(config) => config
        case None => sys.exit(1);
      }

      // Find the 'adapt.jar', either from the hidden option, or by downloading it
      val actualAdaptJarPath = opts.adaptJarPath.getOrElse {
        // Download 'adapt.jar'
        print("Getting latest tests... ")
        downloadFile(adaptDownloadUrl, adaptJarPath)
        println("done.")

        // Expected and actual hashes of 'adapt.jar'
        val adaptStatedHash = fetchHash(adaptHashUrl)
        val adaptActualHash = computeHash(adaptJarPath)

        // Check that 'adapt.jar' matches the newest hash
        require(
          adaptStatedHash.equals(adaptActualHash),
          s"""Hash comparison failed. The downloaded 'adapt.jar' file is corrupt or there was an error on the server
             |  Stated hash:   $adaptStatedHash
             |  Computed hash: $adaptActualHash
             |""".stripMargin
        )
        val file = new File(adaptJarPath)
        if (file.exists) file.deleteOnExit()

        adaptJarPath
      }

      // Run the tests
      val loadFiles = opts.targets.zipWithIndex.map { case (t,i) => s"-Dadapt.ingest.hosts.0.parallel.0.sequential.0.paths.$i=$t" }
      val cmd = s"""java -Xmx${opts.heapSize}
                   |     -Dadapt.runflow=accept
                   |     -Dadapt.skipshutdown=true
                   |     -Dadapt.test.web-ui=${opts.webUi}
                   |     -Dakka.loglevel=ERROR
                   |     -Dadapt.ingest.hosts.0.hostname=host
                   |     -Dadapt.ingest.hosts.0.parallel.0.sequential.0.type=file
                   |     -Dadapt.ingest.hosts.0.parallel.0.sequential.0.namespace=\"\"
                   |     ${loadFiles.mkString(" ")}
                   |     -jar $actualAdaptJarPath
                   |""".stripMargin
      println("Running tests on the data. This could take a moment...")
      cmd ! ProcessLogger(System.out.println, System.err.println)
    }
  } match {
    case Success(exitCode) => sys.exit(exitCode) // Pipe the exit code through
    case Failure(e: Throwable) =>
      println(s"Something went unexpectedly wrong:\n ${e.getMessage}")
      sys.exit(1)
  }
}

case class Config(
    heapSize: String = "6G",
    targets: Seq[String] = Seq(),
    webUi: Boolean = false,
    skipCheck: Boolean = false,
    adaptJarPath: Option[String] = None
)
object Config {
  // Option parser
  val parser = new OptionParser[Config]("adapt-tester") {
    help("help").text("Prints this usage text")

    opt[String]('s', "heap-size")
      .text("Size of heap to use (passed to Java's '-Xmx' option). Default is '6G'.")
      .optional()
      .action((s,c) => c.copy(heapSize = s))

    opt[Unit]('w', "web-ui")
      .text("If tests with visualizations fail, open them in the browser.")
      .optional()
      .action((_,c) => c.copy(webUi = true))

    opt[Unit]('s', "skip-check")
      .text("Skip the checksum test that ensures you are testing with the latest `adapt-tester.jar'.")
      .hidden()
      .optional()
      .action((_,c) => c.copy(skipCheck = true))

    opt[String]("adapt-jar-path")
      .text("Use this particular `adapt.jar' instead of downloading something")
      .hidden()
      .optional()
      .action((s,c) => c.copy(adaptJarPath = Some(s)))

    arg[String]("targets...")
      .text("Either data-files or folders containing data-files")
      .minOccurs(1)
      .unbounded()
      .action((t,c) => c.copy(targets = c.targets :+ t))

    note(
      """
        |Very roughly, heap-size should be ~3G of RAM per million CDM statements.
        |By Java conventions, valid suffixes for heap sizes are 'K', 'M', and 'G'.
        |""".stripMargin
    )
  }
}
