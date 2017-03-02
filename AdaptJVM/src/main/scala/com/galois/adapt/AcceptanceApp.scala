package com.galois.adapt

import com.typesafe.config.ConfigFactory

import java.io.File

/*
 * This is a single-node version of the cluster app.
 *
 * All this object does is load up an alternate Config to pass to 'ClusterDevApp'.
 * That config is in 'src/main/resources/accept.conf'
 */
object AcceptanceApp {
  println(s"Spinning up a development system.")

  def run(): Unit = {
    val acceptConfigFile = new File("src/main/resources/accept.conf")
    val acceptConfig = ConfigFactory.parseFile(acceptConfigFile).resolve()
    
    ClusterDevApp.run(acceptConfig withFallback Application.config)
  }
}
