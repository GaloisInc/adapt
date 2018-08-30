package com.galois.adapt

import java.nio.ByteBuffer
import java.util.UUID

import com.bbn.tc.schema.avro.{cdm19 => bbnCdm19}
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8
import org.neo4j.graphdb.RelationshipType

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Try}

package object cdm19 {

  trait CDM19

  object CDM19 {
    val values = Seq(Principal, ProvenanceTagNode, TagRunLengthTuple, Value, CryptographicHash, Subject, AbstractObject, FileObject, IpcObject, RegistryKeyObject, NetFlowObject, MemoryObject, SrcSinkObject, Event, UnitDependency, TimeMarker)

    object EdgeTypes extends Enumeration {
      type EdgeTypes = Value
      val localPrincipal, subject, /*host,*/ predicateObject, predicateObject2, parameterTagId, flowObject, prevTagId, parentSubject, dependentUnit, unit, tag, tagId, uuid1, uuid2 = Value

      implicit def conv(rt: EdgeTypes) = new RelationshipType() {
        def name = rt.toString
      }
    }

    def readData(filePath: String, limit: Option[Int] = None): Try[(InstrumentationSource, Iterator[Try[CDM19]])] = {
      val fileContents = readAvroFile(filePath)

      fileContents map {
        case (source, data) =>
          val croppedData = limit.fold(data)(data take _)
          (source, croppedData.map(CDM19.parse))
      }
    }

    def readAvroAsTCCDMDatum(filePath: String): Iterator[bbnCdm19.TCCDMDatum] = {
      val tcDatumReader = new SpecificDatumReader(classOf[bbnCdm19.TCCDMDatum])
      val tcFileReader: DataFileReader[bbnCdm19.TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      tcFileReader.iterator().asScala
    }

    def readAvroFile(filePath: String): Try[(InstrumentationSource, Iterator[RawCDM19Type])] = Try {
      val tcIterator = readAvroAsTCCDMDatum(filePath)

      val cdm = tcIterator.next()
      val first: RawCDM19Type = {
        if (cdm.getCDMVersion.toString != "18")
          throw new Exception(s"Expected CDM19, but received CDM${cdm.CDMVersion.toString}")
        new RawCDM19Type(cdm.getDatum, Some(cdm.getHostId))
      }
      (cdm.getSource, Iterator(first) ++ tcIterator.map(cdm => new RawCDM19Type(cdm.getDatum, Some(cdm.getHostId))))
    }

    def parse(cdm: RawCDM19Type): Try[CDM19] = cdm.o match {
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


  case object EpochMarker extends CDM19

  trait CDM19Constructor[T <: CDM19] extends CDM19 {
    type RawCDMType <: org.apache.avro.specific.SpecificRecordBase
    implicit def convertRawTypes(r: RawCDM19Type): RawCDMType = r.asType[RawCDMType]
    def from(cdm: RawCDM19Type): Try[T]

  }

  class RawCDM19Type(val o: Object, val hostIdOpt: Option[bbnCdm19.UUID]) {
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
    def uuid(x: => bbnCdm19.UUID): Option[UUID] = Try(makeJavaUUID(x)).toOption
    def tagOpCode(x: => bbnCdm19.TagOpCode): Option[TagOpCode] = Try(makeTagOpCode(x)).toOption
    def integrityTag(x: => bbnCdm19.IntegrityTag): Option[IntegrityTag] = Try(makeIntegrityTag(x)).toOption
    def confidentialityTag(x: => bbnCdm19.ConfidentialityTag): Option[ConfidentialityTag] = Try(makeConfidentialityTag(x)).toOption
    def value(x: => bbnCdm19.Value): Option[Value] = Value.from(new RawCDM19Type(x, None)).toOption
    def privilegeLevel(x: => bbnCdm19.PrivilegeLevel): Option[PrivilegeLevel] = Try(makePrivilegeLevel(x)).toOption
    def fixedShort(x: => bbnCdm19.SHORT): Option[FixedShort] = Try(x).map(x => new FixedShort(x.bytes)).toOption
    def byteArr(x: java.nio.ByteBuffer): Option[Array[Byte]] = Try(Option(x)).toOption.flatten.map(_.array)
    def listValue(x: java.util.List[bbnCdm19.Value]): Option[Seq[Value]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Value.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listValue: None.getOrElse"))))
    def listProvTagNode(x: java.util.List[bbnCdm19.ProvenanceTagNode]): Option[Seq[ProvenanceTagNode]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceTagNode.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listProvTagNode: None.getOrElse"))))
    def listCryptographicHash(x: java.util.List[bbnCdm19.CryptographicHash]): Option[Seq[CryptographicHash]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => CryptographicHash.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listCryptographicHash: None.getOrElse"))))
    def listUuid(x: java.util.List[bbnCdm19.UUID]): Option[Seq[UUID]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(makeJavaUUID(_)))
    def listTagRunLengthTuple(x: java.util.List[bbnCdm19.TagRunLengthTuple]): Option[Seq[TagRunLengthTuple]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => TagRunLengthTuple.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listTagRunLengthTuple: None.getOrElse"))))
    def listHostIdentifier(x: java.util.List[bbnCdm19.HostIdentifier]): Option[Seq[HostIdentifier]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => HostIdentifier.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listHostIdentifier: None.getOrElse"))))
    def listInterfaces(x: java.util.List[bbnCdm19.Interface]): Option[Seq[Interface]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Interface.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listInterfaces: None.getOrElse"))))
    def listProvenanceAssertion(x: java.util.List[bbnCdm19.ProvenanceAssertion]): Option[Seq[ProvenanceAssertion]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceAssertion.from(new RawCDM19Type(x, None)).getOrElse(throw new RuntimeException("listProvenanceAssertion: None.getOrElse"))))
  }

  implicit def makeHostType(s: bbnCdm19.HostType): HostType = HostType.from(s.toString).getOrElse(throw new RuntimeException("makeHostType: None.getOrElse"))
  implicit def makeSubjectType(s: bbnCdm19.SubjectType): SubjectType = SubjectType.from(s.toString).getOrElse(throw new RuntimeException("makeSubjectType: None.getOrElse"))
  implicit def makePrivilegeLevel(s: bbnCdm19.PrivilegeLevel): PrivilegeLevel = PrivilegeLevel.from(s.toString).getOrElse(throw new RuntimeException("makePrivilegeLevel: None.getOrElse"))
  implicit def makeSrcSinkType(s: bbnCdm19.SrcSinkType): SrcSinkType = SrcSinkType.from(s.toString).getOrElse(throw new RuntimeException("makeSrcSinkType: None.getOrElse"))
  implicit def makeSource(s: bbnCdm19.InstrumentationSource): InstrumentationSource = InstrumentationSource.from(s.toString).getOrElse(throw new RuntimeException("makeSource: None.getOrElse"))  // TODO: Use ordinals for faster performance!
  implicit def makePrincipalType(t: bbnCdm19.PrincipalType): PrincipalType = PrincipalType.from(t.toString).getOrElse(throw new RuntimeException("makePrincipalType: None.getOrElse"))
  implicit def makeEventType(e: bbnCdm19.EventType): EventType = EventType.from(e.toString).getOrElse(throw new RuntimeException("makeEventType: None.getOrElse"))
  implicit def makeFileObjectTime(s: bbnCdm19.FileObjectType): FileObjectType = FileObjectType.from(s.toString).getOrElse(throw new RuntimeException("makeFileObjectTime: None.getOrElse"))
  implicit def makeValueType(v: bbnCdm19.ValueType): ValueType = ValueType.from(v.toString).getOrElse(throw new RuntimeException("makeValueType: None.getOrElse"))
  implicit def makeValDataType(d: bbnCdm19.ValueDataType): ValueDataType = ValueDataType.from(d.toString).getOrElse(throw new RuntimeException("makeValDataType: None.getOrElse"))
  implicit def makeTagOpCode(t: bbnCdm19.TagOpCode): TagOpCode = TagOpCode.from(t.toString).getOrElse(throw new RuntimeException("makeTagOpCode: None.getOrElse"))
  implicit def makeIntegrityTag(i: bbnCdm19.IntegrityTag): IntegrityTag = IntegrityTag.from(i.toString).getOrElse(throw new RuntimeException("makeIntegrityTag: None.getOrElse"))
  implicit def makeConfidentialityTag(c: bbnCdm19.ConfidentialityTag): ConfidentialityTag = ConfidentialityTag.from(c.toString).getOrElse(throw new RuntimeException("makeConfidentialityTag: None.getOrElse"))
  implicit def makeCryptoHashType(c: bbnCdm19.CryptoHashType): CryptoHashType = CryptoHashType.from(c.toString).getOrElse(throw new RuntimeException("makeCryptoHashType: None.getOrElse"))
  implicit def makeIpcObjectType(s: bbnCdm19.IpcObjectType): IpcObjectType = IpcObjectType.from(s.toString).getOrElse(throw new RuntimeException("makeIpcObjectType: None.getOrElse"))

  implicit def makeJavaUUID(u: bbnCdm19.UUID): UUID = {
    val bb = ByteBuffer.wrap(u.bytes)
    new UUID(bb.getLong, bb.getLong)
  }
  implicit def makeByte(s: bbnCdm19.BYTE): FixedByte = new FixedByte(s.bytes)
  implicit def makeString(c: CharSequence): String = c.toString
  implicit def makeStringList(l: java.util.List[CharSequence]): Seq[String] = l.asScala.map(_.toString)
  implicit def makeShort(s: bbnCdm19.SHORT): FixedShort = new FixedShort(s.bytes)
  implicit def makeAbstractObject(o: bbnCdm19.AbstractObject): AbstractObject = AbstractObject.from(new RawCDM19Type(o, None)).get
  implicit def makeTagRunLength(x: bbnCdm19.TagRunLengthTuple): TagRunLengthTuple = TagRunLengthTuple.from(new RawCDM19Type(x, None)).get

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
