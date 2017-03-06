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
  println(s"Spinning up an acceptance system.")

  def run(): Unit = {
    val acceptConfigPath = getClass().getClassLoader().getResource("accept.conf").getPath
    val acceptConfigFile = new File(acceptConfigPath)
    val acceptConfig = ConfigFactory.parseFile(acceptConfigFile).resolve()
    
    ClusterDevApp.run(acceptConfig withFallback Application.config)
  }
}
