package com.galois.adapt

import collection.JavaConversions._

import com.typesafe.config.ConfigFactory


object Application extends App {
  val config = ConfigFactory.load()
  val appMode = config.getString("adapt.app")

  val loadPaths = config.getStringList("adapt.loadfiles").toList

  val loadLimitOpt = config.getInt("adapt.loadlimit") match {
    case 0 => None
    case i => Some(i)
  }
  val devLocalStorageOpt = config.getString("adapt.dev.localstorage") match {
    case "" | "none" | "None" => None
    case path => Some(path)
  }


  appMode.toLowerCase match {
    case "accept"  => AcceptanceApp.run(loadPaths, loadLimitOpt)
    case "prod"    => ProductionApp.run()
    case "cluster" => ClusterDevApp.run()
    case  _        => DevelopmentApp.run(loadPaths, loadLimitOpt, devLocalStorageOpt)
  }

}
