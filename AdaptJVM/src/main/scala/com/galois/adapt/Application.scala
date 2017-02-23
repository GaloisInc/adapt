package com.galois.adapt

import collection.JavaConversions._
import com.typesafe.config.ConfigFactory


object Application extends App {
  val config = ConfigFactory.load()
  val appMode = config.getString("adapt.app")

  val loadPaths = config.getStringList("adapt.loadfiles").toList.map { path =>
    path.replaceFirst("^~",System.getProperty("user.home")); // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
  }

  val loadLimitOpt = config.getInt("adapt.loadlimit") match {
    case 0 => None
    case i => Some(i)
  }
  val devLocalStorageOpt = config.getString("adapt.dev.localstorage") match {
    case "" | "none" | "None" => None
    case path => Some(path)
  }

  val debug: String => Unit = appMode.toLowerCase match {
    case "dev" => println(_)
    case _     => (x: String) => { }
  }
  
  appMode.toLowerCase match {
    case "accept"  => AcceptanceApp.run(loadPaths, loadLimitOpt)
    case "prod"    => ProductionApp.run()
    case "cluster" => ClusterDevApp.run()
    //case  _        => DevelopmentApp.run(loadPaths, loadLimitOpt, devLocalStorageOpt)
  }

}
