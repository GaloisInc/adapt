package com.galois.adapt.scepter

import java.io._
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.{Files, Paths, StandardCopyOption}
//import java.security.{DigestInputStream, MessageDigest}
import java.util.Scanner

import scala.util._
import sys.process._

/* Everytime it is run, this app compares its md5 to 'adapt-tester.hash' it downloads. If it is
 * not the same, it downloads a new version of 'adapt-tester.jar' (checks the hash again) and gets
 * the new jar started before exiting.
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
  val testerJarPath = "adapt-tester.jar"
  val temporaryJarPath = "temporary.jar"

  // Fetch a hash remotely
  def fetchHash(path: String): String = {
    val hashUrl = new URL(path)
    val scanner = new Scanner(hashUrl.openStream()).useDelimiter("\\A")
    return scanner.next().trim
  }

  // Compute a hash of a file
  // TODO: consider using java.security
  def computeHash(path: String): String = {
    val cmd = s"md5 -q $path"
    return cmd.!!.trim
  }

  // Download a file synchronously
  def downloadFile(downloadUrl: String, filePath: String): Unit = {
    val in = Channels.newChannel(new URL(downloadUrl).openStream)
    val out = new FileOutputStream(filePath).getChannel
    out.transferFrom(in, 0, Long.MaxValue)
  }

  val dataFilePath = args.headOption.getOrElse(
    throw new RuntimeException(s"First argument must be a path to the data file you want to test.")
  )

  try {

    // Compare the hash of the current 'adapt-tester.jar' to the published one
    val testerStatedHash = fetchHash(testerHashUrl)
    val testerActualHash = computeHash(testerJarPath)

    if (testerStatedHash != testerActualHash) {
     
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
      
      // Re-run the java program (and shutdown the current one)
      val cmd = s"java -jar $testerJarPath $dataFilePath"
      println("Starting the updated 'adapt-tester.jar'. This may take a while...")
      cmd ! ProcessLogger(println, println)
    
    } else {
      
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

      // Run the tests
      val cmd = s"java -Dadapt.app=accept -Dadapt.loadlimit=0 -Dadapt.loadfile=$dataFilePath -jar $adaptJarPath"
      println("Running tests on the data. This could take a moment...")
      cmd ! ProcessLogger(println, println)

    } 
  } catch {
    // TODO: consider adder finer grain error handling (better error messages)
    case e: Throwable => println(s"Something went wrong:\n ${e.getMessage}")
  }
}
