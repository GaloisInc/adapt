package com.galois.adapt


object AdaptConfig {
  import pureconfig._

  case class IngestUnit(provider: String, files: List[String], startatoffset: Long)
  case class IngestConfig(data: Set[List[IngestUnit]], /*startatoffset: Long,*/ loadlimit: Option[Long], quitafteringest: Boolean, logduplicates: Boolean, produceadm: Boolean, producecdm: Boolean)
  case class RuntimeConfig(webinterface: String, port: Int, apitimeout: Int, dbkeyspace: String, neo4jkeyspace: String, neo4jfile: String, systemname: String, quitonerror: Boolean, logfile: String)
  case class EnvironmentConfig(ta1: String, ta1kafkatopic: String, ta1kafkatopics: List[String], theiaresponsetopic: String)
  case class AdmConfig(maxtimejumpsecs: Long, cdmexpiryseconds: Int, cdmexpirycount: Long, maxeventsmerged: Int, eventexpirysecs: Int, eventexpirycount: Int, dedupEdgeCacheSize: Int, uuidRemapperShards: Int, cdm2cdmlrucachesize: Long = 10000000L, cdm2admlrucachesize: Long = 30000000L, ignoreeventremaps: Boolean, mapdb: String, mapdbbypasschecksum: Boolean, mapdbtransactions: Boolean)
  case class PpmConfigComponents(events: String, everything: String, pathnodes: String, pathnodeuses: String, releasequeue: String)
  case class PpmConfig(saveintervalseconds: Option[Long], pluckingdelay: Int, basedir: String, eventtypemodelsdir: String, loadfilesuffix: String, savefilesuffix: String, shouldload: Boolean, shouldsave: Boolean, rotatescriptpath: String, components: PpmConfigComponents, iforestfreqminutes: Int, iforesttrainingfile: String, iforesttrainingsavefile: String, iforestenabled: Boolean)

  implicit val h1 = ProductHint[RuntimeConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})
  implicit val h2 = ProductHint[EnvironmentConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})
  implicit val h3 = ProductHint[AdmConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})

  val ingestConfig = loadConfigOrThrow[IngestConfig]("adapt.ingest")
  val runFlow = loadConfigOrThrow[String]("adapt.runflow")
  val runtimeConfig = loadConfigOrThrow[RuntimeConfig]("adapt.runtime")
  val envConfig = loadConfigOrThrow[EnvironmentConfig]("adapt.env")
  val admConfig = loadConfigOrThrow[AdmConfig]("adapt.adm")
  val ppmConfig = loadConfigOrThrow[PpmConfig]("adapt.ppm")
  val testWebUi = loadConfigOrThrow[Boolean]("adapt.test.web-ui")
  val kafkaConsumerJavaConfig = com.typesafe.config.ConfigFactory.load().getConfig("akka.kafka.consumer")
}
