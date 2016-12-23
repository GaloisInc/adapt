package com.galois.adapt.scepter

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

import scala.util.Try
import sys.process._


object Wrapper extends App {

  val downloadUrl = "http://192.168.40.210:8000/AdaptJVM-assembly-0.1.jar"
  val filePath = "AdaptJVM-assembly-0.1.jar"

  val dataDownloadUrl = "http://192.168.40.210:8000/ta1-clearscope-cdm13_pandex.bin"
  val dataFilePath = "data.bin"

  val r = Try {
    println("Getting latest tests...")
    val in = Channels.newChannel(new URL(downloadUrl).openStream)
    val out = new FileOutputStream(filePath).getChannel
    out.transferFrom(in, 0, Long.MaxValue)
  }

  val s = Try {
    println("Getting ClearScope test data...")
    val in = Channels.newChannel(new URL(dataDownloadUrl).openStream)
    val out = new FileOutputStream(dataFilePath).getChannel
    out.transferFrom(in, 0, Long.MaxValue)
  }

  val cmd = s"java -jar $filePath $dataFilePath"
  println(cmd)

  println(
    r.map(_ => cmd.!!)
  )

}
