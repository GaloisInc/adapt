package com.galois.adapt

import scala.language.implicitConversions
import java.util.UUID
import java.nio.ByteBuffer

import com.bbn.tc.schema.avro.cdm17.TCCDMDatum
import com.bbn.tc.schema.avro.{cdm17 => bbnCdm17}
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8

import scala.util.Try
import scala.collection.JavaConverters._
import org.neo4j.graphdb.RelationshipType
import shapeless.Lazy

package object cdm17 {

  trait CDM17 extends CdmVersion

  object CDM17 {
    val values = Seq(Principal, ProvenanceTagNode, TagRunLengthTuple, Value, CryptographicHash, Subject, AbstractObject, FileObject, UnnamedPipeObject, RegistryKeyObject, NetFlowObject, MemoryObject, SrcSinkObject, Event, UnitDependency, TimeMarker)

    object EdgeTypes extends Enumeration {
      type EdgeTypes = Value
      val localPrincipal,
      subject, predicateObject, predicateObject2, parameterTagId, flowObject, prevTagId, parentSubject, dependentUnit, unit, tag, tagId = Value

      implicit def conv(rt: EdgeTypes) = new RelationshipType() {
        def name = rt.toString
      }
    }

    def readData(filePath: String, limit: Option[Int] = None): Try[(InstrumentationSource, Iterator[Lazy[Try[CDM17]]])] = {
      val fileContents = readAvroFile(filePath)

      fileContents map {
        case (source, data) =>
          val croppedData = limit.fold(data)(data take _)
          (source, croppedData.map(cdm => Lazy(CDM17.parse(cdm))))
      }
    }

    def readAvroAsTCCDMDatum(filePath: String): Iterator[TCCDMDatum] = {
      val tcDatumReader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
      val tcFileReader: DataFileReader[com.bbn.tc.schema.avro.cdm17.TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      tcFileReader.iterator().asScala
    }

    def readAvroFile(filePath: String): Try[(InstrumentationSource, Iterator[RawCDM17Type])] = Try {
      val tcIterator = readAvroAsTCCDMDatum(filePath)

      val cdm = tcIterator.next()
      val first: RawCDM17Type = {
        if (cdm.getCDMVersion.toString != "17")
          throw new Exception(s"Expected CDM17, but received CDM${cdm.CDMVersion.toString}")
        new RawCDM17Type(cdm.getDatum)
      }
      (cdm.getSource, Iterator(first) ++ tcIterator.map(cdm => new RawCDM17Type(cdm.getDatum)))
    }

    def parse(cdm: RawCDM17Type) = cdm.o match {
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
      case _: Event.RawCDMType => Event.from(cdm)
      case _: UnitDependency.RawCDMType => UnitDependency.from(cdm)
      case _: TimeMarker.RawCDMType => TimeMarker.from(cdm)
      case _: TheiaQueryResult.RawCDMType => TheiaQueryResult.from(cdm)
      case x => throw new RuntimeException(s"No deserializer for: $x")
    }
  }


  case object EpochMarker extends CDM17


  trait CDM17Constructor[T <: CDM17] extends CDM17 {
    type RawCDMType <: org.apache.avro.specific.SpecificRecordBase
    implicit def convertRawTypes(r: RawCDM17Type): RawCDMType = r.asType[RawCDMType]
    def from(cdm: RawCDM17Type): Try[T]

  }

  class RawCDM17Type(val o: Object) extends AnyVal {
    def asType[T]: T = o.asInstanceOf[T]
  }

  type ProvTagNodeValueType = Int with UUID with TagOpCode with IntegrityTag with ConfidentialityTag

  object AvroOpt {
    def listStr(x: => java.util.List[CharSequence]): Option[List[String]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.map(_.toString)) //.asInstanceOf[List[String]])
    def listInt(x: => java.util.List[java.lang.Integer]): Option[Seq[Int]] = Try(Option(x.asScala.toList)).toOption.flatten.map(_.asInstanceOf[Seq[Int]])
    def long(x: => java.lang.Long): Option[Long] = Try(Long2long(x)).toOption
    def int(x: => java.lang.Integer): Option[Int] = Try(Integer2int(x)).toOption
    def str(x: => java.lang.CharSequence): Option[String] = Try(x.toString).toOption
    def map(x: => java.util.Map[CharSequence,CharSequence]): Option[Map[String,String]] = Try(Option(x)).toOption.flatten.map(_.asInstanceOf[java.util.HashMap[Utf8,Utf8]].asScala.map{ case (k,v) => k.toString -> v.toString}.toMap)
    def uuid(x: => bbnCdm17.UUID): Option[UUID] = Try(makeJavaUUID(x)).toOption
    def tagOpCode(x: => bbnCdm17.TagOpCode): Option[TagOpCode] = Try(makeTagOpCode(x)).toOption
    def integrityTag(x: => bbnCdm17.IntegrityTag): Option[IntegrityTag] = Try(makeIntegrityTag(x)).toOption
    def confidentialityTag(x: => bbnCdm17.ConfidentialityTag): Option[ConfidentialityTag] = Try(makeConfidentialityTag(x)).toOption
    def value(x: => bbnCdm17.Value): Option[Value] = Value.from(new RawCDM17Type(x)).toOption
    def privilegeLevel(x: => bbnCdm17.PrivilegeLevel): Option[PrivilegeLevel] = Try(makePrivilegeLevel(x)).toOption
    def fixedShort(x: => bbnCdm17.SHORT): Option[FixedShort] = Try(x).map(x => new FixedShort(x.bytes)).toOption
    def byteArr(x: java.nio.ByteBuffer): Option[Array[Byte]] = Try(Option(x)).toOption.flatten.map(_.array)
    def listValue(x: java.util.List[bbnCdm17.Value]): Option[List[Value]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Value.from(new RawCDM17Type(x)).get))
    def listProvTagNode(x: java.util.List[bbnCdm17.ProvenanceTagNode]): Option[Seq[ProvenanceTagNode]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceTagNode.from(new RawCDM17Type(x)).get))
    def listCryptographicHash(x: java.util.List[bbnCdm17.CryptographicHash]): Option[Seq[CryptographicHash]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => CryptographicHash.from(new RawCDM17Type(x)).get))
    def listUuid(x: java.util.List[bbnCdm17.UUID]): Option[Seq[UUID]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(makeJavaUUID(_)))
    def listTagRunLengthTuple(x: java.util.List[bbnCdm17.TagRunLengthTuple]): Option[Seq[TagRunLengthTuple]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => TagRunLengthTuple.from(new RawCDM17Type(x)).get))
  }

  implicit def makeSubjectType(s: bbnCdm17.SubjectType): SubjectType = SubjectType.from(s.toString).get
  implicit def makePrivilegeLevel(s: bbnCdm17.PrivilegeLevel): PrivilegeLevel = PrivilegeLevel.from(s.toString).get
  implicit def makeSrcSinkType(s: bbnCdm17.SrcSinkType): SrcSinkType = SrcSinkType.from(s.toString).get
  implicit def makeSource(s: bbnCdm17.InstrumentationSource): InstrumentationSource = InstrumentationSource.from(s.toString).get  // TODO: Use ordinals for faster performance!
  implicit def makePrincipalType(t: bbnCdm17.PrincipalType): PrincipalType = PrincipalType.from(t.toString).get
  implicit def makeEventType(e: bbnCdm17.EventType): EventType = EventType.from(e.toString).get
  implicit def makeFileObjectTime(s: bbnCdm17.FileObjectType): FileObjectType = FileObjectType.from(s.toString).get
  implicit def makeValueType(v: bbnCdm17.ValueType): ValueType = ValueType.from(v.toString).get
  implicit def makeValDataType(d: bbnCdm17.ValueDataType): ValueDataType = ValueDataType.from(d.toString).get
  implicit def makeTagOpCode(t: bbnCdm17.TagOpCode): TagOpCode = TagOpCode.from(t.toString).get
  implicit def makeIntegrityTag(i: bbnCdm17.IntegrityTag): IntegrityTag = IntegrityTag.from(i.toString).get
  implicit def makeConfidentialityTag(c: bbnCdm17.ConfidentialityTag): ConfidentialityTag = ConfidentialityTag.from(c.toString).get
  implicit def makeCryptoHashType(c: bbnCdm17.CryptoHashType): CryptoHashType = CryptoHashType.from(c.toString).get
  implicit def makeJavaUUID(u: bbnCdm17.UUID): UUID = {
    val bb = ByteBuffer.wrap(u.bytes)
    new UUID(bb.getLong, bb.getLong)
  }
  implicit def makeString(c: CharSequence): String = c.toString
  implicit def makeStringList(l: java.util.List[CharSequence]): Seq[String] = l.asScala.map(_.toString)
  implicit def makeShort(s: bbnCdm17.SHORT): FixedShort = new FixedShort(s.bytes)
  implicit def makeAbstractObject(o: bbnCdm17.AbstractObject): AbstractObject = AbstractObject.from(new RawCDM17Type(o)).get
  implicit def makeTagRunLength(x: bbnCdm17.TagRunLengthTuple): TagRunLengthTuple = TagRunLengthTuple.from(new RawCDM17Type(x)).get

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
