package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.util.UUID

import akka.NotUsed
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Source
import com.galois.adapt.adm.{ADM, EdgeAdm2Adm}
import com.galois.adapt.cdm17.CDM17
import com.galois.adapt.cdm18.{CDM18, Cdm17to18}
import com.galois.adapt.cdm19.{CDM19, Cdm18to19, InstrumentationSource, RawCDM19Type}
import com.galois.adapt.{cdm17 => cdm17types}
import com.galois.adapt.{cdm18 => cdm18types}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import shapeless.Lazy

import scala.util.{Failure, Success, Try}

object AdaptConfig {
  import pureconfig._

  case class HostName(hostname: String)
  type Namespace = String
  type KakfaTopicName = String
  type FilePath = String

  sealed trait DataModelProduction
  case object ProduceAdm extends DataModelProduction
  case object ProduceCdm extends DataModelProduction
  case object ProduceCdmAndAdm extends DataModelProduction


//  case class IngestUnit(provider: String, files: List[String])
//  case class IngestConfig(data: List[IngestUnit], startatoffset: Long, loadlimit: Option[Long], quitafteringest: Boolean, logduplicates: Boolean, produceadm: Boolean, producecdm: Boolean)
  case class RuntimeConfig(webinterface: String, port: Int, apitimeout: Int, dbkeyspace: String, neo4jkeyspace: String, neo4jfile: String, systemname: String, quitonerror: Boolean, logfile: String)
  case class EnvironmentConfig(ta1: String, ta1kafkatopic: String, ta1kafkatopics: List[String], theiaresponsetopic: String)
  case class AdmConfig(maxtimejumpsecs: Long, cdmexpiryseconds: Int, cdmexpirycount: Long, maxeventsmerged: Int, eventexpirysecs: Int, eventexpirycount: Int, dedupEdgeCacheSize: Int, uuidRemapperShards: Int, cdm2cdmlrucachesize: Long = 10000000L, cdm2admlrucachesize: Long = 30000000L, ignoreeventremaps: Boolean, mapdb: String, mapdbbypasschecksum: Boolean, mapdbtransactions: Boolean)
  case class PpmConfigComponents(events: String, everything: String, pathnodes: String, pathnodeuses: String, releasequeue: String)
  case class PpmConfig(saveintervalseconds: Option[Long], pluckingdelay: Int, basedir: String, eventtypemodelsdir: String, loadfilesuffix: String, savefilesuffix: String, shouldload: Boolean, shouldsave: Boolean, rotatescriptpath: String, components: PpmConfigComponents, iforestfreqminutes: Int, iforesttrainingfile: String, iforesttrainingsavefile: String, iforestenabled: Boolean) {
    require(saveintervalseconds.forall(_ => shouldsave), "`saveintervalseconds` cannot be honored unless `shouldsave` is true")
  }

  implicit val h1 = ProductHint[RuntimeConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})
  implicit val h2 = ProductHint[EnvironmentConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})
  implicit val h3 = ProductHint[AdmConfig](new ConfigFieldMapping {def apply(fieldName: String) = fieldName})
  implicit val h4: CoproductHint[IngestConfig] = ??? // CoproductHint.default[IngestConfig]
  implicit val h5 = new EnumCoproductHint[DataModelProduction]

  val kafkaConsumerJavaConfig = com.typesafe.config.ConfigFactory.load().getConfig("akka.kafka.consumer")
  val ingestConfig: IngestConfig = ??? // loadConfigOrThrow[IngestConfig]("adapt.ingest")
  val runFlow = loadConfigOrThrow[String]("adapt.runflow")
  val runtimeConfig = loadConfigOrThrow[RuntimeConfig]("adapt.runtime")
  val envConfig = loadConfigOrThrow[EnvironmentConfig]("adapt.env")
  val admConfig = loadConfigOrThrow[AdmConfig]("adapt.adm")
  val ppmConfig = loadConfigOrThrow[PpmConfig]("adapt.ppm")
  val testWebUi = loadConfigOrThrow[Boolean]("adapt.test.web-ui")




  trait ErrorHandler {
    def handleError(offset: Long, error: Throwable): Unit
  }
  object ErrorHandler {
    val print: ErrorHandler = new ErrorHandler {
      def handleError(offset: Long, error: Throwable): Unit =
        println(s"Couldn't read binary data at offset: $offset (${error.getMessage})")
    }
  }

  case class IngestConfig(
      hosts: Set[IngestHost],
      quitafteringest: Boolean,
      logduplicates: Boolean,
      produce: DataModelProduction
  ) {
    // TODO: phase this out
    def asSingleHost: IngestHost = if (hosts.size != 1) {
      throw new Exception(s"Only a single host was expected! Found instead: $hosts")
    } else {
      println("WARNING: you used 'asSingleHost' and it is a temporary hack.")
      hosts.head
    }

    def toCdmSource(handler: ErrorHandler = ErrorHandler.print): Source[(Namespace,CDM19), NotUsed] = hosts
      .toList
      .foldLeft(Source.empty[(Namespace,CDM19)])((acc, h: IngestHost) => acc.merge(h.toCdmSource(handler)))
  }

  case class IngestHost(
      ta1: InstrumentationSource,           // who is producing this data.  TODO do we really need this? we could try to infer from data
      hostName: HostName,
      parallelIngests: Set[LinearIngest],

      loadlimit: Option[Long] = None
  ) {
    def isWindows: Boolean = ta1.toString.contains("WINDOWS")
    def simpleTa1Name: String = ta1.toString.split('_').last.toLowerCase

    def toCdmSource(handler: ErrorHandler = ErrorHandler.print): Source[(Namespace,CDM19), NotUsed] = parallelIngests
      .toList
      .foldLeft(Source.empty[(Namespace,CDM19)])((acc, li: LinearIngest) => acc.merge(li.toCdmSource(handler)))
      .take(loadlimit.getOrElse(Long.MaxValue))
  }

  case class Range(
      startInclusive: Long = 0,
      endExclusive:   Long = Long.MaxValue
  ) {
    def applyToSource[Out, Mat](source: Source[Out, Mat]): Source[Out, Mat] = source
      .take(endExclusive)
      .drop(startInclusive)

    /// Variant of [applyToSource] that prints out helpful "Skipping past" messages
    def applyToSourceMessage[Out, Mat](source: Source[Out, Mat], every: Long = 100000): Source[Out, Mat] = source
      .take(endExclusive)
      .statefulMapConcat[Out] { () =>  // This drops while counting live.
        var counter: Long = 0L
        var doneDiscarding: Boolean = startInclusive <= counter

        {
          case cdm if doneDiscarding => List(cdm)
          case cdm =>
            if (counter % every == 0) print(s"\rSkipping past: $counter")
            counter += 1
            doneDiscarding = startInclusive <= counter
            Nil
        }
      }
  }

  // Largest sequential ingest
  case class LinearIngest(
    range: Range,
    sequentialUnits: List[IngestUnit]
  ) {
    def toCdmSource(handler: ErrorHandler): Source[(Namespace,CDM19), NotUsed] = {
      val croppedRange: Source[Lazy[(Try[CDM19], Namespace)], NotUsed] = range.applyToSourceMessage(
        sequentialUnits.foldLeft(Source.empty[Lazy[(Try[CDM19], Namespace)]])((acc, iu) => acc.concat(iu.toCdmSourceTry))
      )

      // Only now do we actually force the parsing of the CDM19 (ie. purge the `Lazy`)
      croppedRange.statefulMapConcat { () =>
        var counter: Long = 0L

        (cdmTry: Lazy[(Try[CDM19], Namespace)]) => {
          counter += 1
          cdmTry.value match {
            case (Success(cdm), namespace) => List(namespace -> cdm)
            case (Failure(f), namespace) => handler.handleError(counter, f); Nil
          }
        }
      }
    }
  }

  sealed trait IngestUnit {
    val namespace: Namespace
    val range: Range

    // The `Lazy` is so that we can skip past records without actually parsing them.
    def toCdmSourceTry: Source[Lazy[(Try[CDM19], Namespace)], _]
  }

  case class FileIngestUnit(
      paths: List[FilePath],
      namespace: Namespace,
      range: Range
  ) extends IngestUnit {

    // Falls back on old CDM parsers
    override def toCdmSourceTry: Source[Lazy[(Try[CDM19], Namespace)], _] = range.applyToSource {

      val pathsExpanded: List[FilePath] = if (paths.length == 1 && new File(paths.head).isDirectory) {
        new File(paths.head).listFiles().toList.collect {
          case f if !f.isHidden => f.getCanonicalPath
        }
      } else {
        // This is an ugly hack to handle paths like ~/Documents/file.avro
        paths.map(_.replaceFirst("^~", System.getProperty("user.home")))
      }

      pathsExpanded.foldLeft(Source.empty[Lazy[(Try[CDM19], Namespace)]]) { case (acc, path) =>
        readCdm19(path) orElse readCdm18(path) orElse readCdm17(path) match {
          case Failure(_) =>
            println(s"Failed to read file $path as CDM19, CDM18, or CDM17. Skipping it for now.")
            acc

          case Success(source: Source[Lazy[Try[CDM19]], NotUsed]) =>
            acc.concat(source.map(lazyTryCdm => lazyTryCdm.map(_ -> namespace)))
        }
      }
    }
  }

  case class KafkaTopicIngestUnit(
      topicName: KakfaTopicName,
      namespace: Namespace,
      range: Range
  ) extends IngestUnit {

    // Only tries the newest CDM version
    override def toCdmSourceTry: Source[Lazy[(Try[CDM19], Namespace)], _] = range.applyToSource {
      Consumer
        .plainSource(
          ConsumerSettings(kafkaConsumerJavaConfig, new ByteArrayDeserializer, new ByteArrayDeserializer),
          Subscriptions.assignmentWithOffset(new TopicPartition(topicName, 0), offset = 0)
        )
        .map(cr => Lazy { (kafkaCdm19Parser(cr), namespace) })
      }
  }

  val reader19 = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm19.TCCDMDatum])

  // Parse a `CDM18` from a kafka record
  def kafkaCdm19Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM19] = Try {
    val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
    val offset = msg.offset()   // msg.record.offset()
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val datum = reader19.read(null, decoder)
    val cdm = new RawCDM19Type(datum.getDatum, Some(datum.getHostId))
    CDM19.parse(cdm)
  }.flatten

  case class CouldNotConvert(cdm: AnyRef, targetCdm: String) extends Exception(s"Could not convert $cdm to $targetCdm")

  // Try to make a CDM19 record from a CDM18 one
  def cdm18ascdm19(c: CDM18, dummyHost: UUID): Try[CDM19] = {
    implicit val dummy: UUID = dummyHost
    c match {
      case e: cdm18types.Event => Success(Cdm18to19.event(e))
      case f: cdm18types.FileObject => Success(Cdm18to19.fileObject(f))
      case m: cdm18types.MemoryObject => Success(Cdm18to19.memoryObject(m))
      case n: cdm18types.NetFlowObject => Success(Cdm18to19.netFlowObject(n))
      case p: cdm18types.Principal => Success(Cdm18to19.principal(p))
      case p: cdm18types.ProvenanceTagNode => Success(Cdm18to19.provenanceTagNode(p))
      case r: cdm18types.RegistryKeyObject => Success(Cdm18to19.registryKeyObject(r))
      case s: cdm18types.SrcSinkObject => Success(Cdm18to19.srcSinkObject(s))
      case s: cdm18types.Subject => Success(Cdm18to19.subject(s))
      case t: cdm18types.TimeMarker => Success(Cdm18to19.timeMarker(t))
      case u: cdm18types.UnitDependency => Success(Cdm18to19.unitDependency(u))
      case u: cdm18types.UnnamedPipeObject => Success(Cdm18to19.ipcObject(u))
      case _ =>
        println(s"couldn't find a way to convert $c")
        Failure(throw CouldNotConvert(c, "CDM19"))
    }
  }

  // Try to make a CDM18 record from a CDM17 one
  def cdm17ascdm18(c: CDM17, dummyHost: UUID): Try[CDM18] = {
    implicit val dummy: UUID = dummyHost
    c match {
      case e: cdm17types.Event => Success(Cdm17to18.event(e))
      case f: cdm17types.FileObject => Success(Cdm17to18.fileObject(f))
      case m: cdm17types.MemoryObject => Success(Cdm17to18.memoryObject(m))
      case n: cdm17types.NetFlowObject => Success(Cdm17to18.netFlowObject(n))
      case p: cdm17types.Principal => Success(Cdm17to18.principal(p))
      case p: cdm17types.ProvenanceTagNode => Success(Cdm17to18.provenanceTagNode(p))
      case r: cdm17types.RegistryKeyObject => Success(Cdm17to18.registryKeyObject(r))
      case s: cdm17types.SrcSinkObject => Success(Cdm17to18.srcSinkObject(s))
      case s: cdm17types.Subject => Success(Cdm17to18.subject(s))
      case t: cdm17types.TimeMarker => Success(Cdm17to18.timeMarker(t))
      case u: cdm17types.UnitDependency => Success(Cdm17to18.unitDependency(u))
      case u: cdm17types.UnnamedPipeObject => Success(Cdm17to18.unnamedPipeObject(u))
      case _ =>
        println(s"couldn't find a way to convert $c")
        Failure(throw CouldNotConvert(c, "CDM18"))
    }
  }

  val dummyHost: UUID = new java.util.UUID(0L,1L)

  // Read a CDM19 file in
  def readCdm19(path: FilePath): Try[Source[Lazy[Try[CDM19]], NotUsed]] =
    CDM19.readData(path).map { case (_, iterator) => Source.fromIterator(() => iterator) }

  // Read a CDM18 file in and, if it works convert it to CDM19
  def readCdm18(path: FilePath): Try[Source[Lazy[Try[CDM19]], NotUsed]] =
    CDM18.readData(path).map { case (_, iterator) => Source.fromIterator(() => iterator.map { cdm18LazyTry =>
      cdm18LazyTry.map((cdm18Try: Try[CDM18]) => cdm18Try.flatMap(cdm18 => cdm18ascdm19(cdm18, dummyHost)))
    }) }

  // Read a CDM17 file in and, if it works convert it to CDM18 then CDM19
  def readCdm17(path: FilePath): Try[Source[Lazy[Try[CDM19]], NotUsed]] =
    CDM17.readData(path).map { case (_, iterator) => Source.fromIterator(() => iterator.map { cdm17LazyTry =>
      cdm17LazyTry.map((cdm17Try: Try[CDM17]) => cdm17Try.flatMap(cdm17 => cdm17ascdm18(cdm17, dummyHost).flatMap(cdm18 => cdm18ascdm19(cdm18, dummyHost))))
    }) }
}
