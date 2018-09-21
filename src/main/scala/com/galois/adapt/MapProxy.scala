package com.galois.adapt

import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.adm.{AdmUUID, CdmUUID, EdgeAdm2Adm}
import org.mapdb.serializer.SerializerArrayTuple
import org.mapdb.{DB, DBMaker, HTreeMap, Serializer}

import scala.util.Random

/// Manages all of the detail of large internal maps, exposing interfaces from `MapSetUtils`. If the logic requires
/// peristing regularly, or on shutdown, this is where that should happen.
class MapProxy(
    fileDbPath: Option[String],
    fileDbBypassChecksum: Boolean,
    fileDbTransactions: Boolean,

    uuidRemapperShards: Int,
    cdm2cdmLruCacheSize: Long,
    cdm2admLruCacheSize: Long,
    dedupEdgeCacheSize: Int
) {

  // In memory DB
  private val memoryDb: DB = DBMaker.memoryDirectDB().make()

  // File DB
  private val fileDb: DB = fileDbPath match {
    case Some(p) =>
      var maker = DBMaker.fileDB(p).fileMmapEnable()

      if (fileDbBypassChecksum) maker = maker.checksumHeaderBypass()
      if (fileDbTransactions) maker = maker.transactionEnable()

      val db = maker.make()

      // On shutdown, expire everything to the on-disk map
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
        override def run(): Unit = {
          println("Expiring MapDB contents to disk...")
          mapdbCdm2AdmShards.foreach(_.clearWithExpire())
          mapdbCdm2CdmShards.foreach(_.clearWithExpire())
          db.close()
          println("MapDB has been closed.")
        }
      }))

      db

    case _ =>
      val p = "/tmp/map_" + Random.nextLong() + ".db"
      val fDB = DBMaker.fileDB(p).fileMmapEnable().make()

      // On shutdown delete the DB
      new File(p).deleteOnExit()

      fDB
  }

  /***************************************************************************************
   * UUID Remapper maps                                                                  *
   ***************************************************************************************/
  assert(uuidRemapperShards >= 0, "Can't have a negative number of shards")
  private val numShards: Int = Math.max(uuidRemapperShards, 1)

  private val threadPool = Executors.newScheduledThreadPool(1)

  private val mapdbCdm2CdmOverflowShards = Array.tabulate(numShards) { shardId =>
    fileDb.hashMap(s"cdm2cdmOverflowShard$shardId")
      .keySerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .valueSerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      //      .counterEnable()
      .createOrOpen()
  }

  private val mapdbCdm2CdmShards: Array[HTreeMap[Array[AnyRef],Array[AnyRef]]] = Array.tabulate(numShards) { shardId =>
    memoryDb.hashMap(s"cdm2cdmShard$shardId")
      .keySerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .valueSerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .counterEnable()
      .expireOverflow(mapdbCdm2CdmOverflowShards(shardId))
      .expireAfterCreate()
      .expireAfterGet()
      .expireMaxSize(cdm2cdmLruCacheSize)
      .expireExecutor(threadPool)
      .createOrOpen()
  }

  val cdm2cdmMapShards: Array[AlmostMap[CdmUUID,CdmUUID]] = Array.tabulate(numShards) { shardId =>
    MapSetUtils.hashMap[Array[AnyRef], CdmUUID, Array[AnyRef], CdmUUID](
      mapdbCdm2CdmShards(shardId),
      { case CdmUUID(uuid, ns) => Array(ns, uuid) }, { case Array(ns: String, uuid: UUID) => CdmUUID(uuid, ns) },
      { case CdmUUID(uuid, ns) => Array(ns, uuid) }, { case Array(ns: String, uuid: UUID) => CdmUUID(uuid, ns) }
    )
  }


  private val mapdbCdm2AdmOverflowShards = Array.tabulate(numShards) { shardId =>
    fileDb.hashMap(s"cdm2admOverflowShard$shardId")
      .keySerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .valueSerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      //      .counterEnable()
      .createOrOpen()
  }

  private val mapdbCdm2AdmShards: Array[HTreeMap[Array[AnyRef],Array[AnyRef]]] = Array.tabulate(numShards) { shardId =>
    memoryDb.hashMap(s"cdm2admShard$shardId")
      .keySerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .valueSerializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .counterEnable()
      .expireOverflow(mapdbCdm2AdmOverflowShards(shardId))
      .expireAfterCreate()
      .expireAfterGet()
      .expireMaxSize(cdm2admLruCacheSize)
      .expireExecutor(threadPool)
      .createOrOpen()
  }

  val cdm2admMapShards: Array[AlmostMap[CdmUUID,AdmUUID]] = Array.tabulate(numShards) { shardId =>
    MapSetUtils.hashMap[Array[AnyRef], CdmUUID, Array[AnyRef], AdmUUID](
      mapdbCdm2CdmShards(shardId),
      { case CdmUUID(uuid, ns) => Array(ns, uuid) }, { case Array(ns: String, uuid: UUID) => CdmUUID(uuid, ns) },
      { case AdmUUID(uuid, ns) => Array(ns, uuid) }, { case Array(ns: String, uuid: UUID) => AdmUUID(uuid, ns) }
    )
  }

  /***************************************************************************************
   * Seen nodes and seen edges                                                           *
   ***************************************************************************************/

  val seenEdges: AlmostSet[EdgeAdm2Adm] = MapSetUtils.lruCacheSet(
    new java.util.LinkedHashMap[EdgeAdm2Adm, None.type](dedupEdgeCacheSize, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[EdgeAdm2Adm, None.type]): Boolean =
        this.size > dedupEdgeCacheSize
    }
  )

  val seenNodes: AlmostSet[AdmUUID] = {
    val seenNodesSet: java.util.NavigableSet[Array[AnyRef]] = fileDb.treeSet("seenNodes")
      .serializer(new SerializerArrayTuple(Serializer.UUID, Serializer.STRING))
      .counterEnable()
      .createOrOpen()
      .asInstanceOf[java.util.NavigableSet[Array[AnyRef]]]

    MapSetUtils.navigableSet[Array[AnyRef],AdmUUID](
      seenNodesSet,
      { case AdmUUID(uuid, ns) => Array(uuid, ns) }, { case Array(uuid: UUID, ns: String) => AdmUUID(uuid, ns) }
    )
  }

  /* LRU variant

  val dedupNodeCacheSize = config.getInt("adapt.adm.dedupNodeCacheSize")

  val seenNodes: AlmostSet[AdmUUID] = MapSetUtils.lruCacheSet(new util.LinkedHashMap[AdmUUID, None.type](dedupNodeCacheSize, 1F, true) {
    override def removeEldestEntry(eldest: java.util.Map.Entry[AdmUUID, None.type]): Boolean = this.size > dedupNodeCacheSize
  })

  */

  /* Bloom filter variant

  implicit val hasAdmUUID: CanGenerateHashFrom[AdmUUID] = new CanGenerateHashFrom[AdmUUID] {
    override def generateHash(from: AdmUUID): Long = {
      from.uuid.getLeastSignificantBits ^ (3 * from.uuid.getMostSignificantBits) ^ (7 * from.namespace.hashCode)
    }
  }
  implicit val hasEdgeAdm2Adm: CanGenerateHashFrom[EdgeAdm2Adm] = new CanGenerateHashFrom[EdgeAdm2Adm] {
    override def generateHash(from: EdgeAdm2Adm): Long = {
      hasAdmUUID.generateHash(from.tgt) ^ (11 * hasAdmUUID.generateHash(from.tgt)) ^ (13 * from.label.hashCode)
    }
  }
  val seenNodes: AlmostSet[AdmUUID] = MapSetUtils.bloomSet(BloomFilter[AdmUUID](300000000L, 1.0 / 3e12))
  val seenEdges: AlmostSet[EdgeAdm2Adm] = MapSetUtils.bloomSet(BloomFilter[EdgeAdm2Adm](300000000L, 1.0 / 3e12))
  */

  /* Regular Scala set variant

  val seenNodes: AlmostSet[AdmUUID] = MapSetUtils.scalaSet(mutable.Set.empty)
  val seenEdges: AlmostSet[EdgeAdm2Adm] = MapSetUtils.scalaSet(mutable.Set.empty)
  */

  /* MapDB Hash Set variant

  val seenNodes: AlmostSet[AdmUUID] = MapSetUtils.hashSet[Array[AnyRef],AdmUUID](
    fileDb.hashSet("seenNodes")
      .serializer(new SerializerArrayTuple(Serializer.STRING, Serializer.UUID))
      .counterEnable()
      .createOrOpen()
      .asInstanceOf[HTreeMap.KeySet[Array[AnyRef]]],
    { case AdmUUID(uuid,ns) => Array(ns,uuid) }, { case Array(ns: String, uuid: UUID) => AdmUUID(uuid,ns) }
  )
  val seenEdges: AlmostSet[EdgeAdm2Adm] = MapSetUtils.hashSet[Array[AnyRef],EdgeAdm2Adm](
    fileDb.hashSet("seenEdges")
      .serializer(new SerializerArrayTuple(
        Serializer.STRING,
        Serializer.STRING,
        Serializer.STRING,
        Serializer.UUID,
        Serializer.UUID)
      )
      .counterEnable()
      .createOrOpen()
      .asInstanceOf[HTreeMap.KeySet[Array[AnyRef]]],

    {
      case EdgeAdm2Adm(AdmUUID(srcUuid, srcNs), lbl, AdmUUID(tgtUuid, tgtNs)) =>
        Array(srcNs, tgtNs, lbl, srcUuid, tgtUuid)
    },
    {
      case Array(srcNs: String, tgtNs: String, lbl: String, srcUuid: UUID, tgtUuid: UUID) =>
        EdgeAdm2Adm(AdmUUID(srcUuid, srcNs), lbl, AdmUUID(tgtUuid, tgtNs))
    }
  )
  */


}
