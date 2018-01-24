package com.galois.adapt

import java.nio.ByteBuffer
import java.util.UUID

import com.bbn.tc.schema.avro.cdm18.TCCDMDatum
import com.bbn.tc.schema.avro.{cdm18 => bbnCdm18}
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8
import org.neo4j.graphdb.RelationshipType

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Try}

package object cdm18 {

  trait CDM18

  object CDM18 {
    val values = Seq(Principal, ProvenanceTagNode, TagRunLengthTuple, Value, CryptographicHash, Subject, AbstractObject, FileObject, UnnamedPipeObject, RegistryKeyObject, NetFlowObject, MemoryObject, SrcSinkObject, Event, UnitDependency, TimeMarker)

    object EdgeTypes extends Enumeration {
      type EdgeTypes = Value
      val localPrincipal, subject, host, predicateObject, predicateObject2, parameterTagId, flowObject, prevTagId, parentSubject, dependentUnit, unit, tag, tagId, source, sink = Value

      implicit def conv(rt: EdgeTypes) = new RelationshipType() {
        def name = rt.toString
      }
    }

    def readData(filePath: String, limit: Option[Int] = None): Try[(InstrumentationSource, Iterator[Try[CDM18]])] = {
      val fileContents = readAvroFile(filePath)

      fileContents map {
        case (source, data) =>
          val croppedData = limit.fold(data)(data take _)
          (source, croppedData.map(CDM18.parse))
      }
    }

    def readAvroAsTCCDMDatum(filePath: String): Iterator[TCCDMDatum] = {
      val tcDatumReader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm18.TCCDMDatum])
      val tcFileReader: DataFileReader[com.bbn.tc.schema.avro.cdm18.TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      tcFileReader.iterator().asScala
    }

    def readAvroFile(filePath: String): Try[(InstrumentationSource, Iterator[RawCDM18Type])] = Try {
      val tcIterator = readAvroAsTCCDMDatum(filePath)

      val cdm = tcIterator.next()
      val first: RawCDM18Type = {
        if (cdm.getCDMVersion.toString != "18")
          throw new Exception(s"Expected CDM18, but received CDM${cdm.CDMVersion.toString}")
        new RawCDM18Type(cdm.getDatum)
      }
      (cdm.getSource, Iterator(first) ++ tcIterator.map(cdm => new RawCDM18Type(cdm.getDatum)))
    }

    def parse(cdm: RawCDM18Type): Try[CDM18] = cdm.o match {
      case _: Host.RawCDMType => Host.from(cdm)
      case _: Principal.RawCDMType => Principal.from(cdm)
      case _: ProvenanceTagNode.RawCDMType => ProvenanceTagNode.from(cdm)
      case _: TagRunLengthTuple.RawCDMType => TagRunLengthTuple.from(cdm)
      case _: Value.RawCDMType => Value.from(cdm)
      case _: CryptographicHash.RawCDMType => CryptographicHash.from(cdm)
      case _: Subject.RawCDMType => Subject.from(cdm)
      case _: AbstractObject.RawCDMType => AbstractObject.from(cdm)
      case _: FileObject.RawCDMType => FileObject.from(cdm)
      case _: UnnamedPipeObject.RawCDMType => UnnamedPipeObject.from(cdm)
      case _: RegistryKeyObject.RawCDMType => RegistryKeyObject.from(cdm)
      case _: NetFlowObject.RawCDMType => NetFlowObject.from(cdm)
      case _: MemoryObject.RawCDMType => MemoryObject.from(cdm)
      case _: SrcSinkObject.RawCDMType => SrcSinkObject.from(cdm)
      case _: PacketSocketObject.RawCDMType => PacketSocketObject.from(cdm)
      case _: Event.RawCDMType => Event.from(cdm)
      case _: UnitDependency.RawCDMType => UnitDependency.from(cdm)
      case _: TimeMarker.RawCDMType => TimeMarker.from(cdm)
      case _: StartMarker.RawCDMType => StartMarker.from(cdm)
      case _: EndMarker.RawCDMType => StartMarker.from(cdm)
      case x => throw new RuntimeException(s"No deserializer for: $x")
    }
  }


  case object EpochMarker extends CDM18

  trait CDM18Constructor[T <: CDM18] extends CDM18 {
    type RawCDMType <: org.apache.avro.specific.SpecificRecordBase
    implicit def convertRawTypes(r: RawCDM18Type): RawCDMType = r.asType[RawCDMType]
    def from(cdm: RawCDM18Type): Try[T]

  }

  class RawCDM18Type(val o: Object) extends AnyVal {
    def asType[T]: T = o.asInstanceOf[T]
  }

//  type ProvTagNodeValueType = Int with UUID with TagOpCode with IntegrityTag with ConfidentialityTag

  object AvroOpt {
    def listStr(x: => java.util.List[CharSequence]): Option[List[String]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.map(_.toString)) //.asInstanceOf[List[String]])
    def listInt(x: => java.util.List[java.lang.Integer]): Option[Seq[Int]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.asInstanceOf[Seq[Int]])
    def long(x: => java.lang.Long): Option[Long] = Try(Long2long(x)).toOption
    def int(x: => java.lang.Integer): Option[Int] = Try(Integer2int(x)).toOption
    def str(x: => java.lang.CharSequence): Option[String] = Try(x.toString).toOption
    def map(x: => java.util.Map[CharSequence,CharSequence]): Option[Map[String,String]] = Try(Option(x)).toOption.flatten.map(_.asInstanceOf[java.util.HashMap[Utf8,Utf8]].asScala.map{ case (k,v) => k.toString -> v.toString}.toMap)
    def uuid(x: => bbnCdm18.UUID): Option[UUID] = Try(makeJavaUUID(x)).toOption
    def tagOpCode(x: => bbnCdm18.TagOpCode): Option[TagOpCode] = Try(makeTagOpCode(x)).toOption
    def integrityTag(x: => bbnCdm18.IntegrityTag): Option[IntegrityTag] = Try(makeIntegrityTag(x)).toOption
    def confidentialityTag(x: => bbnCdm18.ConfidentialityTag): Option[ConfidentialityTag] = Try(makeConfidentialityTag(x)).toOption
    def value(x: => bbnCdm18.Value): Option[Value] = Value.from(new RawCDM18Type(x)).toOption
    def privilegeLevel(x: => bbnCdm18.PrivilegeLevel): Option[PrivilegeLevel] = Try(makePrivilegeLevel(x)).toOption
    def fixedShort(x: => bbnCdm18.SHORT): Option[FixedShort] = Try(x).map(x => new FixedShort(x.bytes)).toOption
    def byteArr(x: java.nio.ByteBuffer): Option[Array[Byte]] = Try(Option(x)).toOption.flatten.map(_.array)
    def listValue(x: java.util.List[bbnCdm18.Value]): Option[Seq[Value]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Value.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listValue: None.getOrElse"))))
    def listProvTagNode(x: java.util.List[bbnCdm18.ProvenanceTagNode]): Option[Seq[ProvenanceTagNode]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceTagNode.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listProvTagNode: None.getOrElse"))))
    def listCryptographicHash(x: java.util.List[bbnCdm18.CryptographicHash]): Option[Seq[CryptographicHash]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => CryptographicHash.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listCryptographicHash: None.getOrElse"))))
    def listUuid(x: java.util.List[bbnCdm18.UUID]): Option[Seq[UUID]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(makeJavaUUID(_)))
    def listTagRunLengthTuple(x: java.util.List[bbnCdm18.TagRunLengthTuple]): Option[Seq[TagRunLengthTuple]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => TagRunLengthTuple.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listTagRunLengthTuple: None.getOrElse"))))
    def listHostIdentifier(x: java.util.List[bbnCdm18.HostIdentifier]): Option[Seq[HostIdentifier]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => HostIdentifier.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listHostIdentifier: None.getOrElse"))))
    def listInterfaces(x: java.util.List[bbnCdm18.Interface]): Option[Seq[Interface]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Interface.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listInterfaces: None.getOrElse"))))
    def listProvenanceAssertion(x: java.util.List[bbnCdm18.ProvenanceAssertion]): Option[Seq[ProvenanceAssertion]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceAssertion.from(new RawCDM18Type(x)).getOrElse(throw new RuntimeException("listProvenanceAssertion: None.getOrElse"))))
  }

  implicit def makeHostType(s: bbnCdm18.HostType): HostType = HostType.from(s.toString).getOrElse(throw new RuntimeException("makeHostType: None.getOrElse"))
  implicit def makeSubjectType(s: bbnCdm18.SubjectType): SubjectType = SubjectType.from(s.toString).getOrElse(throw new RuntimeException("makeSubjectType: None.getOrElse"))
  implicit def makePrivilegeLevel(s: bbnCdm18.PrivilegeLevel): PrivilegeLevel = PrivilegeLevel.from(s.toString).getOrElse(throw new RuntimeException("makePrivilegeLevel: None.getOrElse"))
  implicit def makeSrcSinkType(s: bbnCdm18.SrcSinkType): SrcSinkType = SrcSinkType.from(s.toString).getOrElse(throw new RuntimeException("makeSrcSinkType: None.getOrElse"))
  implicit def makeSource(s: bbnCdm18.InstrumentationSource): InstrumentationSource = InstrumentationSource.from(s.toString).getOrElse(throw new RuntimeException("makeSource: None.getOrElse"))  // TODO: Use ordinals for faster performance!
  implicit def makePrincipalType(t: bbnCdm18.PrincipalType): PrincipalType = PrincipalType.from(t.toString).getOrElse(throw new RuntimeException("makePrincipalType: None.getOrElse"))
  implicit def makeEventType(e: bbnCdm18.EventType): EventType = EventType.from(e.toString).getOrElse(throw new RuntimeException("makeEventType: None.getOrElse"))
  implicit def makeFileObjectTime(s: bbnCdm18.FileObjectType): FileObjectType = FileObjectType.from(s.toString).getOrElse(throw new RuntimeException("makeFileObjectTime: None.getOrElse"))
  implicit def makeValueType(v: bbnCdm18.ValueType): ValueType = ValueType.from(v.toString).getOrElse(throw new RuntimeException("makeValueType: None.getOrElse"))
  implicit def makeValDataType(d: bbnCdm18.ValueDataType): ValueDataType = ValueDataType.from(d.toString).getOrElse(throw new RuntimeException("makeValDataType: None.getOrElse"))
  implicit def makeTagOpCode(t: bbnCdm18.TagOpCode): TagOpCode = TagOpCode.from(t.toString).getOrElse(throw new RuntimeException("makeTagOpCode: None.getOrElse"))
  implicit def makeIntegrityTag(i: bbnCdm18.IntegrityTag): IntegrityTag = IntegrityTag.from(i.toString).getOrElse(throw new RuntimeException("makeIntegrityTag: None.getOrElse"))
  implicit def makeConfidentialityTag(c: bbnCdm18.ConfidentialityTag): ConfidentialityTag = ConfidentialityTag.from(c.toString).getOrElse(throw new RuntimeException("makeConfidentialityTag: None.getOrElse"))
  implicit def makeCryptoHashType(c: bbnCdm18.CryptoHashType): CryptoHashType = CryptoHashType.from(c.toString).getOrElse(throw new RuntimeException("makeCryptoHashType: None.getOrElse"))

  implicit def makeJavaUUID(u: bbnCdm18.UUID): UUID = {
    val bb = ByteBuffer.wrap(u.bytes)
    new UUID(bb.getLong, bb.getLong)
  }
  implicit def makeByte(s: bbnCdm18.BYTE): FixedByte = new FixedByte(s.bytes)
  implicit def makeString(c: CharSequence): String = c.toString
  implicit def makeStringList(l: java.util.List[CharSequence]): Seq[String] = l.asScala.map(_.toString)
  implicit def makeShort(s: bbnCdm18.SHORT): FixedShort = new FixedShort(s.bytes)
  implicit def makeAbstractObject(o: bbnCdm18.AbstractObject): AbstractObject = AbstractObject.from(new RawCDM18Type(o)).get
  implicit def makeTagRunLength(x: bbnCdm18.TagRunLengthTuple): TagRunLengthTuple = TagRunLengthTuple.from(new RawCDM18Type(x)).get

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
