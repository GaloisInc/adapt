package com.galois.adapt.scepter

import java.io._
import java.math.BigInteger
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.{Files, Paths}
import java.security.{DigestInputStream, MessageDigest}
import java.util.Scanner

import scala.util._
import sys.process._


object Wrapper extends App {

  val downloadUrl = "https://adapt.galois.com/acceptance_tests/current.jar"
  val executableJarPath = "adapt.jar"

  val dataFilePath = args.headOption.getOrElse(
    throw new RuntimeException(s"First argument must be a path to the data file you want to test.")
  )

  val r = Try {
    print("Getting latest tests...")
    val in = Channels.newChannel(new URL(downloadUrl).openStream)
    val out = new FileOutputStream(executableJarPath).getChannel
    out.transferFrom(in, 0, Long.MaxValue)
    print(" ...done.\n")
  }

  val fileHash = Try {
    val md = MessageDigest.getInstance("MD5")
    val is = Files.newInputStream(Paths.get(executableJarPath))
    val dis = new DigestInputStream(is, md)
    val bytes = md.digest()
    val bi = new BigInteger(1, bytes)
    val digest = String.format("%0" + (bytes.length << 1) + "X", bi)
    digest.toLowerCase
  }
//  println(s"Hash of downloaded file:\n${fileHash.get}")


  val hashUrl = new URL("https://adapt.galois.com/acceptance_tests/current.hash")
  val statedHash = new Scanner(hashUrl.openStream()).useDelimiter("\\A").next()
//  println(s"Stated hash:\n$statedHash")

//  require(fileHash.get.equals(statedHash), s"Hash comparison failed. The download file is corrupt or there was an error on the server\nStated hash:   $statedHash\nComputed hash: ${fileHash.get}\n")

  val cmd = s"java -Xmx4G -Dadapt.app=accept -Dadapt.loadlimit=0 -Dadapt.loadfile=$dataFilePath -jar $executableJarPath"

  val file = new File(executableJarPath)
  if (file.exists) file.deleteOnExit()

  r match {
    case _: Success[_] =>
      println(s"Running tests on the data. This could take a moment...")
      println(cmd.!!)
    case f: Failure[_] =>
      println(s"Getting new tests failed: ${f.exception.getMessage}")
  }
}
