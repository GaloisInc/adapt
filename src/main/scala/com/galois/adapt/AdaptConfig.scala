package com.galois.adapt

import java.io.{ByteArrayInputStream, File, FileNotFoundException}
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.cdm17.CDM17
import com.galois.adapt.cdm18.CDM18
import com.galois.adapt.cdm19.CDM19
import com.galois.adapt.cdm20.{CDM20, RawCDM20Type}
import com.galois.adapt.cdm19.{CDM19, RawCDM19Type}
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.ConsumerRecord
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, NoValidCoproductChoiceFound, UnknownKey}
import shapeless.Lazy

import scala.util.{Failure, Success, Try}

object AdaptConfig extends Utils {
  import pureconfig._

  type HostName = String
  type Namespace = String
  type KakfaTopicName = String
  type FilePath = String

  // A horrible hack to work around hanging behaviour around `system.terminate()`
  type SkipActorSystemShutdown = Boolean

  sealed trait DataModelProduction
  case object ProduceAdm extends DataModelProduction
  case object ProduceCdm extends DataModelProduction
  case object ProduceCdmAndAdm extends DataModelProduction


  case class AdaptConfig(
    runflow: String,
    ingest: IngestConfig,
    runtime: RuntimeConfig,
    env: EnvironmentConfig,
    quine: QuineConfig,
    adm: AdmConfig,
    ppm: PpmConfig,
    test: TestConfig,
    alarms: AlarmsConfig,
    skipshutdown: SkipActorSystemShutdown = false
  )


  case class IngestConfig(
      var hosts: List[IngestHost],
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

    def toCdmSource(handler: ErrorHandler = ErrorHandler.print): Source[(Namespace,CDM20), NotUsed] = hosts
      .foldLeft(Source.empty[(Namespace,CDM20)])((acc, h: IngestHost) => acc.merge(h.toCdmSource(handler)))
  }

  case class RuntimeConfig(
    webinterface: String,
    port: Int,
    apitimeout: Int,
    dbkeyspace: String,
    neo4jkeyspace: String,
    neo4jfile: String,
    systemname: String,
    quitonerror: Boolean,
    logfile: String,
    lmdbgigabytes: Int = 4
  )

  case class EnvironmentConfig(
    kafkabootstrap: String,
    truststorepath: String,
    trustpass: String,
    keystorepath: String,
    keypass: String,
    sslkey: String
  )

  type Ip = String
  case class QuineConfig(
    quineactorparallelism: Int,
    thishost: Ip,
    hosts: List[QuineHost],  // Order matters!
    inmemsoftlimit: Int,
    inmemhardlimit: Int,
    ppmobservationbuffer: Int
  )
  case class QuineHost(
    ip: Ip,
    shardcount: Int,
    namespaces: List[String]
  )

  case class AdmConfig(
    maxtimejumpsecs: Long,
    cdmexpiryseconds: Int,
    cdmexpirycount: Long,
    maxeventsmerged: Int,
    eventexpirysecs: Int,
    eventexpirycount: Int,
    dedupEdgeCacheSize: Int,
    uuidRemapperShards: Int,
    cdm2cdmlrucachesize: Long = 10000000L,
    cdm2admlrucachesize: Long = 30000000L,
    ignoreeventremaps: Boolean,
    mapdb: Option[String],
    mapdbbypasschecksum: Boolean,
    mapdbtransactions: Boolean
  )

  case class PpmConfigComponents(
    events: String,
    everything: String,
    pathnodes: String,
    pathnodeuses: String,
    releasequeue: String
  )

  case class PpmConfig(
//    saveintervalseconds: Option[Long],
    pluckingdelay: Int,
    basedir: String,
    eventtypemodelsdir: String,
    loadfilesuffix: String = "",
    savefilesuffix: String = "",

    shouldloadppmtrees: Boolean,
    shouldloadalarms: Boolean,
    shouldloadlocalprobabilitiesfromalarms: Boolean,
    shouldloadppmpartialobservationaccumulators: Boolean,  // stream component state and also partialMap => state for producing a PPM tree observation

    shouldsaveppmtrees: Boolean,
    shouldsavealarms: Boolean,
    shouldsaveppmpartialobservationaccumulators: Boolean,

    rotatescriptpath: String,
    components: PpmConfigComponents,
    iforestfreqminutes: Int,
    iforesttrainingfile: String,
    iforesttrainingsavefile: String,
    iforestenabled: Boolean,
    computethresholdintervalminutes: Int = 0,
    alarmlppercentile: Float = 0.1F
  ) {
//    require(saveintervalseconds.forall(_ => shouldsave), "`saveintervalseconds` cannot be honored unless `shouldsave` is true")
  }

  case class TestConfig(
    `web-ui`: Boolean
  )



  case class GuiConfig(enabled: Boolean)
  case class ConsoleConfig(enabled: Boolean)
  case class LogConfig(enabled: Boolean, fileprefix: String)
  case class SplunkConfig(
    enabled: Boolean,
    token: String,
    host: String,
    port: Int,
    maxbufferlength: Long,
    realtimeReportingPeriodSeconds: Int,
    detailedReportingPeriodSeconds: Int,
    percentProcessInstancesToTake: Float)
  case class AlarmsConfig(
    splunk: SplunkConfig,
    logging: LogConfig,
    console: ConsoleConfig,
    gui: GuiConfig
  ){
  }

  val lowercaseFieldMapping: ConfigFieldMapping = new ConfigFieldMapping {
    def apply(fieldName: String): String = fieldName.toLowerCase
  }

  private implicit val _hint1  = ProductHint[RuntimeConfig](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint2  = ProductHint[EnvironmentConfig](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint3  = ProductHint[AdmConfig](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint4  = new EnumCoproductHint[DataModelProduction]
  private implicit val _hint5  = ProductHint[TestConfig](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint6  = CoproductHint.default[IngestUnit]
  private implicit val _hint7  = ProductHint[LinearIngest](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint8  = ProductHint[Range](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint9  = ProductHint[IngestHost](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint10 = ProductHint[IngestConfig](fieldMapping = lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint11 = ProductHint[AdaptConfig](lowercaseFieldMapping, allowUnknownKeys = false)
  private implicit val _hint12 = new EnumCoproductHint[DataProvider] {
    override def fieldValue(name: String): String = name
  }

  /* This should be deriveable automatically by pureconfig, _but_ there is an issue in Scala < 2.12 which messes with
   * coproduct derivations:
   *
   * ```
   * knownDirectSubclasses of IngestUnit observed before subclass FileIngestUnit registered
   * knownDirectSubclasses of IngestUnit observed before subclass KafkaTopicIngestUnit registered
   * ```
   *
   * See https://github.com/pureconfig/pureconfig/issues/205
   */
  implicit val ingestUnitConfigReader: ConfigReader[IngestUnit] = new ConfigReader[IngestUnit] {
    def from(cur: ConfigCursor): Either[ConfigReaderFailures, IngestUnit] = for {
      typ <- cur.atPath("type").right.flatMap(_.asString)
      iu  <- typ.toLowerCase match {
        case "file" | "files" =>
          for {
            // prevent extra fields
            _ <- cur.asMap.right.flatMap { kvs =>
              kvs.keySet.diff(Set("type", "paths", "namespace")).toList match {
                case Nil => Right(())
                case key :: _ => Left(ConfigReaderFailures(ConvertFailure(UnknownKey(key), cur)))
              }
            }

            // expected fields
            paths     <- cur.atPath("paths").right.flatMap(ConfigReader[List[FilePath]].from)
            namespace <- cur.atPath("namespace").right.flatMap(ConfigReader[Namespace].from)
            range <- cur.atPath("range").right.toOption
              .fold[Either[ConfigReaderFailures, Range]](Right(Range()))(ConfigReader[Range].from)
          } yield FileIngestUnit(paths, namespace, range)

        case "kafka" | "kafkatopic" =>
          for {
            // prevent extra fields
            _ <- cur.asMap.right.flatMap { kvs =>
              kvs.keySet.diff(Set("type", "topicname", "namespace")).toList match {
                case Nil => Right(())
                case key :: _ => Left(ConfigReaderFailures(ConvertFailure(UnknownKey(key), cur)))
              }
            }

            // expected fields
            topicName <- cur.atPath("topicname").right.flatMap(ConfigReader[KakfaTopicName].from)
            namespace <- cur.atPath("namespace").right.flatMap(ConfigReader[Namespace].from)
            range <- cur.atPath("range").right.toOption
              .fold[Either[ConfigReaderFailures, Range]](Right(Range()))(ConfigReader[Range].from)
          } yield KafkaTopicIngestUnit(topicName, namespace, range)

        case _ => Left(ConfigReaderFailures(ConvertFailure(NoValidCoproductChoiceFound(cur.value), cur)))
      }
    } yield iu
  }

  val kafkaConsumerJavaConfig = com.typesafe.config.ConfigFactory.load().getConfig("akka.kafka.consumer")


  // This is here only to disallow extra keys--to prevent typos.
  private val adaptConfig: AdaptConfig = Try(loadConfigOrThrow[AdaptConfig]("adapt")) match {
    case Success(c) => c
    case Failure(f) =>
      println(f.getMessage)
      scala.sys.exit(1)
  }

  var ingestConfig: IngestConfig = adaptConfig.ingest
  val runFlow: String = adaptConfig.runflow
  val runtimeConfig: RuntimeConfig = adaptConfig.runtime
  val envConfig: EnvironmentConfig = adaptConfig.env
  val quineConfig: QuineConfig = adaptConfig.quine
  val admConfig: AdmConfig = adaptConfig.adm
  val ppmConfig: PpmConfig = adaptConfig.ppm
  val testWebUi: TestConfig = adaptConfig.test
  val alarmConfig: AlarmsConfig = adaptConfig.alarms
  val skipshutdown: SkipActorSystemShutdown = adaptConfig.skipshutdown

  trait ErrorHandler {
    def handleError(offset: Long, error: Throwable): Unit
  }
  object ErrorHandler {
    val print: ErrorHandler = new ErrorHandler {
      def handleError(offset: Long, error: Throwable): Unit =
        println(s"Couldn't read binary data at offset: $offset (${error.getMessage})")
    }
  }

  case class IngestHost(
    var ta1: Option[DataProvider] = None,
      /* who is producing this data. We need this because we need to know who is producing
       * data before data actually arrives. That means we can only assert the provider after
       * the fact.
       */

    hostName: HostName,
    parallel: List[LinearIngest],

    startatoffset: Option[Long] = None,
    loadlimit: Option[Long] = None
  ) {
    var filterAst: Option[Filter] = None
    private var filter: Option[Filterable => Boolean] = filterAst.map(FilterCdm.compile)

    def setFilter(newFilter: Option[Filter]): Try[Unit] = Try {
      filter = newFilter.map(FilterCdm.compile)
      filterAst = newFilter
    }

    def isWindows: Boolean = DataProvider.isWindows(
      ta1.getOrElse(throw new Exception("No instrumentation source found - make sure `toCdmSource` has mean called first"))
    )

    def simpleTa1Name: String = ta1
      .getOrElse(throw new Exception("No instrumentation source found - make sure `toCdmSource` has mean called first"))
      .toString
      .toLowerCase


    def toCdmSource(handler: ErrorHandler = ErrorHandler.print): Source[(Namespace,CDM20), NotUsed] = {
      val linearized = parallel.foldLeft(Source.empty[(Namespace,CDM20)])((acc, li: LinearIngest) => acc.merge(li.toCdmSource(handler, updateHost _)))
      val offsetApplied = startatoffset.fold(linearized){offset => println(s"Starting at offset: $offset"); linearized.drop(offset)}
      val limitApplied = loadlimit.fold(offsetApplied){limit => offsetApplied.take(limit)} //.take(loadlimit.getOrElse(Long.MaxValue))
      limitApplied.filter { case (_, cdm: CDM20) => filter.fold(true)(applyFilter(cdm, _)) }
    }

    def updateHost(is: DataProvider): Unit = ta1 match {
      case None => ta1 = Some(is)
      case Some(isOld) => if (is != isOld) {
        assert(is == isOld, s"Inconsistent instrumentation source $is != $isOld!")
      }
    }
  }

  case class Range(
    startInclusive: Long = 0,
    endExclusive:   Long = Long.MaxValue,
    var shouldIngest: Boolean = true
  ) {
    def applyToSource[Out, Mat](source: Source[Out, Mat]): Source[Out, Mat] = source
      .take(endExclusive)
      .drop(startInclusive)
      .takeWhile(_ => shouldIngest)

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
    range: Range = Range(),
    sequential: List[IngestUnit]
  ) {
    def toCdmSource(
        handler: ErrorHandler,
        check: DataProvider => Unit = { _ => }
    ): Source[(Namespace,CDM20), NotUsed] = {
      val croppedRange: Source[Lazy[(Try[CDM20], Namespace)], NotUsed] = range.applyToSourceMessage(
        sequential.foldLeft(Source.empty[Lazy[(Try[CDM20], Namespace)]])((acc, iu) => acc.concat(iu.toCdmSourceTry(check)))
      )

      // Only now do we actually force the parsing of the CDM20 (ie. purge the `Lazy`)
      croppedRange.statefulMapConcat { () =>
        var counter: Long = 0L

        (cdmTry: Lazy[(Try[CDM20], Namespace)]) => {
          counter += 1
          cdmTry.value match {
            case (Success(cdm), namespace) => List(namespace -> cdm)
            case (Failure(f), namespace) => handler.handleError(counter, f); Nil
          }
        }
      }
    }
  }

  sealed trait IngestUnit extends Product {
    val namespace: Namespace
    val range: Range

    // The `Lazy` is so that we can skip past records without actually parsing them.
    def toCdmSourceTry(check: DataProvider => Unit): Source[Lazy[(Try[CDM20], Namespace)], _]
  }

  case class FileIngestUnit(
    paths: List[FilePath],
    namespace: Namespace,
    range: Range = Range()
  ) extends IngestUnit {

    // Falls back on old CDM parsers
    override def toCdmSourceTry(check: DataProvider => Unit): Source[Lazy[(Try[CDM20], Namespace)], _] = range.applyToSource {

      val pathsExpanded: List[FilePath] = if (paths.length == 1 && new File(paths.head).isDirectory) {
        new File(paths.head).listFiles().toList.collect {
          case f if !f.isHidden => f.getCanonicalPath
        }
      } else {
        // This is an ugly hack to handle paths like ~/Documents/file.avro
        paths.map(_.replaceFirst("^~", System.getProperty("user.home")))
      }

      pathsExpanded.foldLeft(Source.empty[Lazy[(Try[CDM20], Namespace)]]) { case (acc, path) =>
        readCdm20(path) orElse readCdm19(path) orElse readCdm18(path) orElse readCdm17(path) match {
          case Failure(_: FileNotFoundException) =>
            println(s"Failed to find a file at $path. Skipping it for now.")
            acc

          case Failure(_) =>
            println(s"Failed to read file $path as CDM20, CDM19, CDM18, or CDM17. Skipping it for now.")
            acc

          case Success((is: DataProvider, source: Source[Lazy[Try[CDM20]], NotUsed])) =>
            check(is)
            acc.concat(source.map(lazyTryCdm => lazyTryCdm.map(_ -> namespace)))
        }
      }
    }
  }

  case class KafkaTopicIngestUnit(
    topicName: KakfaTopicName,
    namespace: Namespace,
    range: Range = Range()
  ) extends IngestUnit {

    import akka.kafka.scaladsl.Consumer
    import akka.kafka.{ConsumerSettings, Subscriptions}
    import org.apache.kafka.common.TopicPartition
    import org.apache.kafka.common.serialization.ByteArrayDeserializer

    // Only tries the newest CDM version, also doesn't check that we were right with the provider
    override def toCdmSourceTry(_check: DataProvider => Unit): Source[Lazy[(Try[CDM20], Namespace)], _] =
      range.applyToSource {
        Consumer
          .plainSource(
            ConsumerSettings(kafkaConsumerJavaConfig, new ByteArrayDeserializer, new ByteArrayDeserializer),
            Subscriptions.assignmentWithOffset(new TopicPartition(topicName, 0), offset = 0)
          )
          .map(cr => Lazy { (KafkaTopicIngestUnit.kafkaCdm20Parser(cr), namespace) })
      }
  }
  object KafkaTopicIngestUnit {

    private val reader20 = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm20.TCCDMDatum])

    // Parse a `CDM20` from a kafka record
    def kafkaCdm20Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM20] = Try {
      import org.apache.avro.io.DecoderFactory

      val bais = new ByteArrayInputStream(msg.value()) // msg.record.value()
      val offset = msg.offset() // msg.record.offset()
      val decoder = DecoderFactory.get.binaryDecoder(bais, null)
      val datum = reader20.read(null, decoder)
      val cdm = new RawCDM20Type(datum.getDatum, Some(datum.getHostId))
      CDM20.parse(cdm)
    }.flatten
  }

  case class CouldNotConvert(cdm: AnyRef, targetCdm: String) extends Exception(s"Could not convert $cdm to $targetCdm")

  // Try to make a CDM20 record from a CDM19 one
  def cdm19ascdm20(c: CDM19): Try[CDM20] = {
    import com.galois.adapt.cdm20.Cdm19to20
    import com.galois.adapt.{cdm19 => cdm19types}

    c match {
      case e: cdm19types.Event => Success(Cdm19to20.event(e))
      case f: cdm19types.FileObject => Success(Cdm19to20.fileObject(f))
      case m: cdm19types.MemoryObject => Success(Cdm19to20.memoryObject(m))
      case n: cdm19types.NetFlowObject => Success(Cdm19to20.netFlowObject(n))
      case p: cdm19types.Principal => Success(Cdm19to20.principal(p))
      case p: cdm19types.ProvenanceTagNode => Success(Cdm19to20.provenanceTagNode(p))
      case r: cdm19types.RegistryKeyObject => Success(Cdm19to20.registryKeyObject(r))
      case s: cdm19types.SrcSinkObject => Success(Cdm19to20.srcSinkObject(s))
      case s: cdm19types.Subject => Success(Cdm19to20.subject(s))
      case t: cdm19types.TimeMarker => Success(Cdm19to20.timeMarker(t))
      case u: cdm19types.UnitDependency => Success(Cdm19to20.unitDependency(u))
      case u: cdm19types.IpcObject => Success(Cdm19to20.ipcObject(u))
      case u: cdm19types.Host => Success(Cdm19to20.host(u))
      case u: cdm19types.PacketSocketObject => Success(Cdm19to20.packetSocketObject(u))
      case _ =>
        println(s"couldn't find a way to convert $c")
        Failure(throw CouldNotConvert(c, "CDM20"))
    }
  }

  // Try to make a CDM20 record from a CDM18 one
  def cdm18ascdm19(c: CDM18, dummyHost: UUID): Try[CDM19] = {
    import com.galois.adapt.cdm19.Cdm18to19
    import com.galois.adapt.{cdm18 => cdm18types}

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
    import com.galois.adapt.cdm18.Cdm17to18
    import com.galois.adapt.{cdm17 => cdm17types}

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

  def applyFilter(c: CDM20, f: Filterable => Boolean): Boolean = c match {
    case c: cdm20.Event => f(Filterable.apply(c))
    case c: cdm20.FileObject => f(Filterable.apply(c))
    //      case (s, c: Host) => (s, Left(Filterable.apply(c)))
    case c: cdm20.MemoryObject => f(Filterable.apply(c))
    case c: cdm20.NetFlowObject => f(Filterable.apply(c))
    case c: cdm20.PacketSocketObject => f(Filterable.apply(c))
    case c: cdm20.Principal => f(Filterable.apply(c))
    case c: cdm20.ProvenanceTagNode => f(Filterable.apply(c))
    case c: cdm20.RegistryKeyObject => f(Filterable.apply(c))
    case c: cdm20.SrcSinkObject => f(Filterable.apply(c))
    case c: cdm20.Subject => f(Filterable.apply(c))
    case c: cdm20.TagRunLengthTuple => f(Filterable.apply(c))
    case c: cdm20.UnitDependency => f(Filterable.apply(c))
    case c: cdm20.IpcObject => f(Filterable.apply(c))
    case other => true
  }

  // Read a CDM20 file in
  def readCdm20(path: FilePath): Try[(DataProvider, Source[Lazy[Try[CDM20]], NotUsed])] =
    CDM20.readData(path).map { case (is, iterator) => (
      DataProvider.fromInstrumentationSource(is),
      Source.fromIterator(() => iterator)
    ) }

  // Read a CDM19 file in and, if it works convert it to CDM20
  def readCdm19(path: FilePath): Try[(DataProvider, Source[Lazy[Try[CDM20]], NotUsed])] = {
    import com.galois.adapt.cdm20.Cdm19to20

    CDM19.readData(path).map { case (is, iterator) => (
      DataProvider.fromInstrumentationSource(Cdm19to20.instrumentationSource(is)),
      Source.fromIterator(() => iterator.map { cdm19LazyTry =>
        cdm19LazyTry.map((cdm19Try: Try[CDM19]) => for {
            cdm19 <- cdm19Try
            cdm20 <- cdm19ascdm20(cdm19)
          } yield cdm20
        )
      })
    ) }
  }

  // Read a CDM18 file in and, if it works convert it to CDM19 then CDM20
  def readCdm18(path: FilePath): Try[(DataProvider, Source[Lazy[Try[CDM20]], NotUsed])] = {
    import com.galois.adapt.cdm19.Cdm18to19
    import com.galois.adapt.cdm20.Cdm19to20


    CDM18.readData(path).map { case (is, iterator) => (
      DataProvider.fromInstrumentationSource(Cdm19to20.instrumentationSource(Cdm18to19.instrumentationSource(is))),
      Source.fromIterator(() => iterator.map { cdm18LazyTry =>
        cdm18LazyTry.map((cdm18Try: Try[CDM18]) => for {
            cdm18 <- cdm18Try
            cdm19 <- cdm18ascdm19(cdm18, dummyHost)
            cdm20 <- cdm19ascdm20(cdm19)
          } yield cdm20
        )
      })
    ) }
  }

  // Read a CDM17 file in and, if it works convert it to CDM18, CDM19, then CDM20
  def readCdm17(path: FilePath): Try[(DataProvider, Source[Lazy[Try[CDM20]], NotUsed])] = {
    import com.galois.adapt.cdm18.Cdm17to18
    import com.galois.adapt.cdm19.Cdm18to19
    import com.galois.adapt.cdm20.Cdm19to20

    CDM17.readData(path).map { case (is, iterator) => (
      DataProvider.fromInstrumentationSource(Cdm19to20.instrumentationSource(Cdm18to19.instrumentationSource(Cdm17to18.instrumentationSource(is)))),
      Source.fromIterator(() => iterator.map { cdm17LazyTry =>
        cdm17LazyTry.map((cdm17Try: Try[CDM17]) => for {
            cdm17 <- cdm17Try
            cdm18 <- cdm17ascdm18(cdm17, dummyHost)
            cdm19 <- cdm18ascdm19(cdm18, dummyHost)
            cdm20 <- cdm19ascdm20(cdm19)
          } yield cdm20
        )
      })
    ) }
  }
}

trait Utils {

  // Alec: this should exist in Scala 2.12, but doesn't seem to be present in 2.11
  implicit class EitherOps[A,B](either: Either[A,B]) {
    def map[B1](f: (B) ⇒ B1): Either[A, B1] = either match {
      case Left(a) => Left(a)
      case Right(b) => Right(f(b))
    }

    def flatMap[A1 >: A, B1](f: (B) ⇒ Either[A1, B1]): Either[A1, B1] = either match {
      case Left(a) => Left(a)
      case Right(b) => f(b)
    }
  }
  object EitherOps {
    def either[A,B](either: Either[A,B]): EitherOps[A,B] = new EitherOps(either)
    def option[A,B](option: Option[B], err: => A): EitherOps[A,B] = option match {
      case None => Left(err)
      case Some(a) => Right(a)
    }
  }
}
