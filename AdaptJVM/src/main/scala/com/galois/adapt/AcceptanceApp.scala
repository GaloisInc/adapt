package com.galois.adapt


/*
 * This is a single-node version of the cluster app.
 *
 * All this object does is load up an alternate Config to pass to 'ClusterDevApp'.
 * That config is in 'src/main/resources/accept.conf'
 */
object AcceptanceApp {
  println(s"Spinning up an acceptance system.")

  def run(): Unit = {
    ClusterDevApp.run(Application.config.getConfig("accept") withFallback Application.config)
  }
}
