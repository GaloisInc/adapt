package com.galois.adapt

import java.nio.ByteBuffer
import java.util.UUID

import com.bbn.tc.schema.avro.{cdm20 => bbnCdm20}
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8
import shapeless.Lazy

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Try}

package object cdm20 {

  trait CDM20 extends CdmVersion

  object CDM20 {
    val values = Seq(Principal, ProvenanceTagNode, TagRunLengthTuple, Value, CryptographicHash, Subject, AbstractObject, FileObject, IpcObject, RegistryKeyObject, NetFlowObject, MemoryObject, SrcSinkObject, Event, UnitDependency, TimeMarker)

    object EdgeTypes extends Enumeration {
      type EdgeTypes = Value
      val localPrincipal, subject, /*host,*/ predicateObject, predicateObject2, parameterTagId, flowObject, prevTagId, parentSubject, dependentUnit, unit, tag, tagId, uuid1, uuid2 = Value

//      implicit def conv(rt: EdgeTypes) = new RelationshipType() {
//        def name = rt.toString
//      }
    }

    def readData(filePath: String, limit: Option[Int] = None): Try[(InstrumentationSource, Iterator[Lazy[Try[CDM20]]])] = {
      val fileContents = readAvroFile(filePath)

      fileContents map {
        case (source, data) =>
          val croppedData = limit.fold(data)(data take _)
          (source, croppedData.map(cdm => Lazy(CDM20.parse(cdm))))
      }
    }

    def readAvroAsTCCDMDatum(filePath: String): Iterator[bbnCdm20.TCCDMDatum] = {
      val tcDatumReader = new SpecificDatumReader(classOf[bbnCdm20.TCCDMDatum])
      val tcFileReader: DataFileReader[bbnCdm20.TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      tcFileReader.iterator().asScala
    }

    def readAvroFile(filePath: String): Try[(InstrumentationSource, Iterator[RawCDM20Type])] = Try {
      val tcIterator = readAvroAsTCCDMDatum(filePath)

      val cdm = tcIterator.next()
      val first: RawCDM20Type = {
        if (cdm.getCDMVersion.toString != "20")
          throw new Exception(s"Expected CDM20, but received CDM${cdm.getCDMVersion.toString}")
        new RawCDM20Type(cdm.getDatum, Some(cdm.getHostId))
      }
      (cdm.getSource, Iterator(first) ++ tcIterator.map(cdm => new RawCDM20Type(cdm.getDatum, Some(cdm.getHostId))))
    }

    def parse(cdm: RawCDM20Type): Try[CDM20] = cdm.o match {
      case _: Host.RawCDMType => Host.from(cdm)
      case _: Principal.RawCDMType => Principal.from(cdm)
      case _: ProvenanceTagNode.RawCDMType => ProvenanceTagNode.from(cdm)
      case _: Subject.RawCDMType => Subject.from(cdm)
      case _: FileObject.RawCDMType => FileObject.from(cdm)
      case _: IpcObject.RawCDMType => IpcObject.from(cdm)
      case _: RegistryKeyObject.RawCDMType => RegistryKeyObject.from(cdm)
      case _: PacketSocketObject.RawCDMType => PacketSocketObject.from(cdm)
      case _: NetFlowObject.RawCDMType => NetFlowObject.from(cdm)
      case _: MemoryObject.RawCDMType => MemoryObject.from(cdm)
      case _: SrcSinkObject.RawCDMType => SrcSinkObject.from(cdm)
      case _: Event.RawCDMType => Event.from(cdm)
      case _: UnitDependency.RawCDMType => UnitDependency.from(cdm)
      case _: TimeMarker.RawCDMType => TimeMarker.from(cdm)
      case _: EndMarker.RawCDMType => EndMarker.from(cdm)

      // these should not be top-level
//      case _: Value.RawCDMType => Value.from(cdm)
//      case _: CryptographicHash.RawCDMType => CryptographicHash.from(cdm)
//      case _: AbstractObject.RawCDMType => AbstractObject.from(cdm)

      case x => throw new RuntimeException(s"No deserializer for: $x")
    }
  }


  case object EpochMarker extends CDM20

  trait CDM20Constructor[T <: CDM20] extends CDM20 {
    type RawCDMType <: org.apache.avro.specific.SpecificRecordBase
    implicit def convertRawTypes(r: RawCDM20Type): RawCDMType = r.asType[RawCDMType]
    def from(cdm: RawCDM20Type): Try[T]

  }

  class RawCDM20Type(val o: Object, val hostIdOpt: Option[bbnCdm20.UUID]) {
    def asType[T]: T = o.asInstanceOf[T]
    def getHostId: Option[UUID] = hostIdOpt.flatMap(AvroOpt.uuid(_))
  }

//  type ProvTagNodeValueType = Int with UUID with TagOpCode with IntegrityTag with ConfidentialityTag

  object AvroOpt {
    def listStr(x: => java.util.List[CharSequence]): Option[List[String]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.map(_.toString)) //.asInstanceOf[List[String]])
    def listInt(x: => java.util.List[java.lang.Integer]): Option[Seq[Int]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.asInstanceOf[Seq[Int]])
    def long(x: => java.lang.Long): Option[Long] = Try(Long2long(x)).toOption
    def int(x: => java.lang.Integer): Option[Int] = Try(Integer2int(x)).toOption
    def str(x: => java.lang.CharSequence): Option[String] = Try(x.toString).toOption
    def map(x: => java.util.Map[CharSequence,CharSequence]): Option[Map[String,String]] = Try(Option(x)).toOption.flatten.map(_.asInstanceOf[java.util.HashMap[Utf8,Utf8]].asScala.map{ case (k,v) => k.toString -> v.toString}.toMap)
    def uuid(x: => bbnCdm20.UUID): Option[UUID] = Try(makeJavaUUID(x)).toOption
    def tagOpCode(x: => bbnCdm20.TagOpCode): Option[TagOpCode] = Try(makeTagOpCode(x)).toOption
    def integrityTag(x: => bbnCdm20.IntegrityTag): Option[IntegrityTag] = Try(makeIntegrityTag(x)).toOption
    def confidentialityTag(x: => bbnCdm20.ConfidentialityTag): Option[ConfidentialityTag] = Try(makeConfidentialityTag(x)).toOption
    def value(x: => bbnCdm20.Value): Option[Value] = Value.from(new RawCDM20Type(x, None)).toOption
    def privilegeLevel(x: => bbnCdm20.PrivilegeLevel): Option[PrivilegeLevel] = Try(makePrivilegeLevel(x)).toOption
    def fixedShort(x: => bbnCdm20.SHORT): Option[FixedShort] = Try(x).map(x => new FixedShort(x.bytes)).toOption
    def byteArr(x: java.nio.ByteBuffer): Option[Array[Byte]] = Try(Option(x)).toOption.flatten.map(_.array)
    def listValue(x: java.util.List[bbnCdm20.Value]): Option[Seq[Value]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Value.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listValue: None.getOrElse"))))
    def listProvTagNode(x: java.util.List[bbnCdm20.ProvenanceTagNode]): Option[Seq[ProvenanceTagNode]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceTagNode.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listProvTagNode: None.getOrElse"))))
    def listCryptographicHash(x: java.util.List[bbnCdm20.CryptographicHash]): Option[Seq[CryptographicHash]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => CryptographicHash.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listCryptographicHash: None.getOrElse"))))
    def listUuid(x: java.util.List[bbnCdm20.UUID]): Option[Seq[UUID]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(makeJavaUUID(_)))
    def listTagRunLengthTuple(x: java.util.List[bbnCdm20.TagRunLengthTuple]): Option[Seq[TagRunLengthTuple]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => TagRunLengthTuple.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listTagRunLengthTuple: None.getOrElse"))))
    def listHostIdentifier(x: java.util.List[bbnCdm20.HostIdentifier]): Option[Seq[HostIdentifier]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => HostIdentifier.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listHostIdentifier: None.getOrElse"))))
    def listInterfaces(x: java.util.List[bbnCdm20.Interface]): Option[Seq[Interface]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Interface.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listInterfaces: None.getOrElse"))))
    def listProvenanceAssertion(x: java.util.List[bbnCdm20.ProvenanceAssertion]): Option[Seq[ProvenanceAssertion]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceAssertion.from(new RawCDM20Type(x, None)).getOrElse(throw new RuntimeException("listProvenanceAssertion: None.getOrElse"))))
  }

  implicit def makeHostType(s: bbnCdm20.HostType): HostType = HostType.from(s.toString).getOrElse(throw new RuntimeException(s"makeHostType: $s"))
  implicit def makeSubjectType(s: bbnCdm20.SubjectType): SubjectType = SubjectType.from(s.toString).getOrElse(throw new RuntimeException(s"makeSubjectType: $s"))
  implicit def makePrivilegeLevel(s: bbnCdm20.PrivilegeLevel): PrivilegeLevel = PrivilegeLevel.from(s.toString).getOrElse(throw new RuntimeException(s"makePrivilegeLevel: $s"))
  implicit def makeSrcSinkType(s: bbnCdm20.SrcSinkType): SrcSinkType = SrcSinkType.from(s.toString).getOrElse(throw new RuntimeException(s"makeSrcSinkType: $s"))
  implicit def makeSource(s: bbnCdm20.InstrumentationSource): InstrumentationSource = InstrumentationSource.from(s.toString).getOrElse(throw new RuntimeException(s"makeSource: $s"))  // TODO: Use ordinals for faster performance!
  implicit def makePrincipalType(t: bbnCdm20.PrincipalType): PrincipalType = PrincipalType.from(t.toString).getOrElse(throw new RuntimeException(s"makePrincipalType: $t"))
  implicit def makeEventType(e: bbnCdm20.EventType): EventType = EventType.from(e.toString).getOrElse(throw new RuntimeException(s"makeEventType: $e"))
  implicit def makeFileObjectTime(s: bbnCdm20.FileObjectType): FileObjectType = FileObjectType.from(s.toString).getOrElse(throw new RuntimeException(s"makeFileObjectTime: $s"))
  implicit def makeValueType(v: bbnCdm20.ValueType): ValueType = ValueType.from(v.toString).getOrElse(throw new RuntimeException(s"makeValueType: $v"))
  implicit def makeValDataType(d: bbnCdm20.ValueDataType): ValueDataType = ValueDataType.from(d.toString).getOrElse(throw new RuntimeException(s"makeValDataType: $d"))
  implicit def makeTagOpCode(t: bbnCdm20.TagOpCode): TagOpCode = TagOpCode.from(t.toString).getOrElse(throw new RuntimeException(s"makeTagOpCode: $t"))
  implicit def makeIntegrityTag(i: bbnCdm20.IntegrityTag): IntegrityTag = IntegrityTag.from(i.toString).getOrElse(throw new RuntimeException(s"makeIntegrityTag: $i"))
  implicit def makeConfidentialityTag(c: bbnCdm20.ConfidentialityTag): ConfidentialityTag = ConfidentialityTag.from(c.toString).getOrElse(throw new RuntimeException(s"makeConfidentialityTag: $c"))
  implicit def makeCryptoHashType(c: bbnCdm20.CryptoHashType): CryptoHashType = CryptoHashType.from(c.toString).getOrElse(throw new RuntimeException(s"makeCryptoHashType: $c"))
  implicit def makeIpcObjectType(s: bbnCdm20.IpcObjectType): IpcObjectType = IpcObjectType.from(s.toString).getOrElse(throw new RuntimeException(s"makeIpcObjectType: $s"))

  implicit def makeJavaUUID(u: bbnCdm20.UUID): UUID = {
    val bb = ByteBuffer.wrap(u.bytes)
    new UUID(bb.getLong, bb.getLong)
  }
  implicit def makeByte(s: bbnCdm20.BYTE): FixedByte = new FixedByte(s.bytes)
  implicit def makeString(c: CharSequence): String = c.toString
  implicit def makeStringList(l: java.util.List[CharSequence]): Seq[String] = l.asScala.map(_.toString)
  implicit def makeShort(s: bbnCdm20.SHORT): FixedShort = new FixedShort(s.bytes)
  implicit def makeAbstractObject(o: bbnCdm20.AbstractObject): AbstractObject = AbstractObject.from(new RawCDM20Type(o, None)).get
  implicit def makeTagRunLength(x: bbnCdm20.TagRunLengthTuple): TagRunLengthTuple = TagRunLengthTuple.from(new RawCDM20Type(x, None)).get

  object DBOpt {
    // Flattens out nested "properties":
    def fromKeyValMap(mapOpt: Option[Map[String,String]]): List[(String, Any)] = mapOpt.fold[List[(String, Any)]](List.empty)(aMap =>
      if (aMap.isEmpty) List.empty
      else aMap.toList.flatMap {
        case ("key", value) => List(("keyFromProperties", Try(value.toLong).getOrElse(value)))
        case ("size", value) => List(("sizeFromProperties", Try(value.toLong).getOrElse(value)))
        case (k,value) => List((k.toString, Try(value.toLong).getOrElse(value)))
      }
    )
  }

}
