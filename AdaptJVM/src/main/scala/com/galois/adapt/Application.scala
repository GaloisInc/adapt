package com.galois.adapt

import com.typesafe.config.ConfigFactory


object Application extends App {
  val config = ConfigFactory.load()
  val appMode = config.getString("adapt.app")

  val loadFileOpt = config.getString("adapt.loadfile") match {
    case "" | "None" | "none" => None
    case path => Some(path)
  }
  val loadLimitOpt = config.getInt("adapt.loadlimit") match {
    case 0 => None
    case i => Some(i)
  }
  val devLocalStorageOpt = config.getString("adapt.dev.localstorage") match {
    case "" | "none" | "None" => None
    case path => Some(path)
  }


  appMode.toLowerCase match {
    case "accept" => AcceptanceApp.run(loadFileOpt.get, loadLimitOpt)
    case "prod"   => ProductionApp.run()
    case  _       => DevelopmentApp.run(loadFileOpt, loadLimitOpt, devLocalStorageOpt)
  }

}
