package com.galois.adapt

import com.galois.adapt.feature._
import com.typesafe.config.ConfigFactory

import java.io.File

object DevelopmentApp {
  println(s"Spinning up a development system.")

  def run(): Unit = {
    val configFile = new File("src/main/resources/dev.conf")
    val config = ConfigFactory.parseFile(configFile).withFallback(Application.config)
    ClusterDevApp.run(config)
  }
}
