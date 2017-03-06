package com.galois.adapt

import com.typesafe.config.ConfigFactory

import java.io.File

/*
 * This is a single-node version of the cluster app.
 *
 * All this object does is load up an alternate Config to pass to 'ClusterDevApp'.
 * That config is in 'src/main/resources/dev.conf'
 */
object DevelopmentApp {
  println(s"Spinning up a development system.")

  def run(): Unit = {
    val devConfigPath = getClass().getClassLoader().getResource("dev.conf").getPath
    val devConfigFile = new File(devConfigPath)
    val devConfig = ConfigFactory.parseFile(devConfigFile).resolve()
    
    ClusterDevApp.run(devConfig withFallback Application.config)
  }
}
