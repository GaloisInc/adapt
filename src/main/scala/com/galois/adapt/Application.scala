package com.galois.adapt

import collection.JavaConversions._
import com.typesafe.config.ConfigFactory


object Application extends App {
  val config = ConfigFactory.load()
  val appMode = config.getString("adapt.app")

  val loadPaths = config.getStringList("adapt.ingest.loadfiles").toList.map { path =>
    path.replaceFirst("^~",System.getProperty("user.home")); // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
  }

  val loadLimitOpt = config.getInt("adapt.ingest.loadlimit") match {
    case 0 => None
    case i => Some(i)
  }

  val debug: String => Unit = appMode.toLowerCase match {
    case "dev" => println(_)
    case _     => (x: String) => { }
  }
  
  appMode.toLowerCase match {
    case "accept"  => AcceptanceApp.run()
    case "prod"    => ProductionApp.run()
//    case "dev"     => DevelopmentApp.run()
//    case "cluster" => ClusterDevApp.run(config)
  }

}




object AcceptanceApp {
  println(s"Spinning up an acceptance testing system.")

  def run(): Unit = {
    println(s"\n\n        THE ACCEPTANCE APP IS CURRENTLY BROKEN. \n        Someone please fix this soon.\n\n")
//    ClusterDevApp.run(Application.config.getConfig("accept") withFallback Application.config)
  }
}



/*
 * This is a single-node version of the cluster app.
 *
 * All this object does is load up an alternate Config to pass to 'ClusterDevApp'.
 * That config is in 'src/main/resources/dev.conf'
 */
object DevelopmentApp {
  println(s"Spinning up a development system.")

  def run(): Unit = {
    //val devConfigPath = getClass().getClassLoader().getResource("dev.conf").getPath
    //val devConfigFile = new File(devConfigPath)
    //val devConfig = ConfigFactory.parseFile(devConfigFile).resolve()
    //val devConfig1 = ConfigFactory.load("dev")//.resolve()
    //println("CONFIG: " + devConfig1)
    println(Application.config.getConfig("dev"))


//    ClusterDevApp.run(Application.config.getConfig("dev") withFallback Application.config)
  }
}
