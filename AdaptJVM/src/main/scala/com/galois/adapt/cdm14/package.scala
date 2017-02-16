package com.galois.adapt

import scala.language.implicitConversions
import java.util.UUID

import com.bbn.tc.schema.avro.TCCDMDatum
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8
import scala.util.Try
import scala.collection.JavaConverters._


package object cdm14 {

  trait CDM14

  object CDM14 {
    val values = Seq(Principal, ProvenanceTagNode, TagRunLengthTuple, Value, CryptographicHash, Subject, AbstractObject, FileObject, UnnamedPipeObject, RegistryKeyObject, NetFlowObject, MemoryObject, SrcSinkObject, Event, TimeMarker)

    def readData(filePath: String, limit: Option[Int] = None): Try[Iterator[Try[CDM14]]] = readAvroFile(filePath).map { x =>
      val cdmDataIter = x.map(CDM14.parse)
      limit.fold(cdmDataIter)(l => cdmDataIter.take(l))
    }

    def readAvroFile(filePath: String) = Try {
      val tcDatumReader = new SpecificDatumReader(classOf[TCCDMDatum])
      val tcFileReader: DataFileReader[TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      val tcIterator = tcFileReader.iterator.asScala

      val first = {
        val cdm = tcIterator.next
        if (cdm.CDMVersion.toString != "13")
          throw new Exception(s"Expected CDM14, but received CDM${cdm.CDMVersion.toString}")
        new RawCDM14Type(cdm.getDatum)
      }
      Iterator(first) ++ tcFileReader.iterator.asScala.map(cdm => new RawCDM14Type(cdm.getDatum))
    }

  //TODO make these classes
    def parse(cdm: RawCDM14Type) = cdm.o match {
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
      case _: TimeMarker.RawCDMType => TimeMarker.from(cdm)
      case x => throw new RuntimeException(s"No deserializer for: $x")
    }
  }


  case object EpochMarker extends CDM14


  trait CDM14Constructor[T <: CDM14] extends CDM14 {
    type RawCDMType <: org.apache.avro.specific.SpecificRecordBase
    implicit def convertRawTypes(r: RawCDM14Type): RawCDMType = r.asType[RawCDMType]
    def from(cdm: RawCDM14Type): Try[T]

  }

  class RawCDM14Type(val o: Object) extends AnyVal {
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
    def uuid(x: => com.bbn.tc.schema.avro.UUID): Option[UUID] = Try(UUID.nameUUIDFromBytes(x.bytes)).toOption
    def tagOpCode(x: => com.bbn.tc.schema.avro.TagOpCode): Option[TagOpCode] = Try(x).toOption
    def integrityTag(x: => com.bbn.tc.schema.avro.IntegrityTag): Option[IntegrityTag] = Try(x).toOption
    def confidentialityTag(x: => com.bbn.tc.schema.avro.ConfidentialityTag): Option[ConfidentialityTag] = Try(x).toOption
    def value(x: => com.bbn.tc.schema.avro.Value): Option[Value] = Try(x).toOption
    def privilegeLevel(x: => com.bbn.tc.schema.avro.PrivilegeLevel): Option[PrivilegeLevel] = Try(x).toOption
    // TODO def fixedShort(x: => com.bbn.tc.schema.avro.SHORT): Option[FixedShort] = Try(x).map(x => new FixedShort(x.bytes)).toOption
    def byteArr(x: java.nio.ByteBuffer): Option[Array[Byte]] = Try(Option(x)).toOption.flatten.map(_.array)
    def listValue(x: java.util.List[com.bbn.tc.schema.avro.Value]): Option[Seq[Value]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => Value.from(new RawCDM14Type(x)).get))
    def listProvTagNode(x: java.util.List[com.bbn.tc.schema.avro.ProvenanceTagNode]): Option[Seq[ProvenanceTagNode]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => ProvenanceTagNode.from(new RawCDM14Type(x)).get))
    def listCryptographicHash(x: java.util.List[com.bbn.tc.schema.avro.CryptographicHash]): Option[Seq[CryptographicHash]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => CryptographicHash.from(new RawCDM14Type(x)).get))
    def listUuid(x: java.util.List[com.bbn.tc.schema.avro.UUID]): Option[Seq[UUID]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => UUID.nameUUIDFromBytes(x.bytes)))
    def listTagRunLengthTuple(x: java.util.List[com.bbn.tc.schema.avro.TagRunLengthTuple]): Option[Seq[TagRunLengthTuple]] = Try(Option(x)).toOption.flatten.map(
      _.asScala.toList.map(x => UUID.nameUUIDFromBytes(x)))
  }

  implicit def makeSubjectType(s: com.bbn.tc.schema.avro.SubjectType): SubjectType = SubjectType.from(s.toString).get
  implicit def makePrivilegeLevel(s: com.bbn.tc.schema.avro.PrivilegeLevel): PrivilegeLevel = PrivilegeLevel.from(s.toString).get
  implicit def makeSrcSinkType(s: com.bbn.tc.schema.avro.SrcSinkType): SrcSinkType = SrcSinkType.from(s.toString).get
  implicit def makeSource(s: com.bbn.tc.schema.avro.InstrumentationSource): InstrumentationSource = InstrumentationSource.from(s.toString).get  // TODO: Use ordinals for faster performance!
  implicit def makePrincipalType(t: com.bbn.tc.schema.avro.PrincipalType): PrincipalType = PrincipalType.from(t.toString).get
  implicit def makeEventType(e: com.bbn.tc.schema.avro.EventType): EventType = EventType.from(e.toString).get
  implicit def makeFileObjectTime(s: com.bbn.tc.schema.avro.FileObjectType): FileObjectType = FileObjectType.from(s.toString).get
  implicit def makeValueType(v: com.bbn.tc.schema.avro.ValueType): ValueType = ValueType.from(v.toString).get
  implicit def makeValDataType(d: com.bbn.tc.schema.avro.ValueDataType): ValueDataType = ValueDataType.from(d.toString).get
  implicit def makeTagOpCode(t: com.bbn.tc.schema.avro.TagOpCode): TagOpCode = TagOpCode.from(t.toString).get
  implicit def makeIntegrityTag(i: com.bbn.tc.schema.avro.IntegrityTag): IntegrityTag = IntegrityTag.from(i.toString).get
  implicit def makeConfidentialityTag(c: com.bbn.tc.schema.avro.ConfidentialityTag): ConfidentialityTag = ConfidentialityTag.from(c.toString).get
  implicit def makeCryptoHashType(c: com.bbn.tc.schema.avro.CryptoHashType): CryptoHashType = CryptoHashType.from(c.toString).get

//TODO do we need these holdovers?
  implicit def makeJavaUUID(u: com.bbn.tc.schema.avro.UUID): UUID = UUID.nameUUIDFromBytes(u.bytes)
  implicit def makeString(c: CharSequence): String = c.toString
  implicit def makeStringList(l: java.util.List[CharSequence]): Seq[String] = l.asScala.map(_.toString)
  //TODO implicit def makeShort(s: com.bbn.tc.schema.avro.SHORT): FixedShort = new FixedShort(s.bytes)
  implicit def makeAbstractObject(o: com.bbn.tc.schema.avro.AbstractObject): AbstractObject = AbstractObject.from(new RawCDM14Type(o)).get

  object DBOpt {
    // Flattens out nested "properties":
    def fromKeyValMap(mapOpt: Option[Map[String,String]]): List[Any] = mapOpt.fold[List[Any]](List.empty)(aMap =>
      if (aMap.isEmpty) List.empty
      else aMap.toList.flatMap { case (k,value) => List(
        k.toString, Try(value.toLong).getOrElse(value)
      ) }
    )
  }
}