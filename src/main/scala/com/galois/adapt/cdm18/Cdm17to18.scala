package com.galois.adapt.cdm18

import com.galois.adapt.cdm17
import com.galois.adapt.cdm18
import java.util.UUID

// This object provides implicit conversions from CDM17 to CDM18 Scala case classes
object Cdm17to18 {

  // Enumerations
  implicit def subjectType(st: cdm17.SubjectType): cdm18.SubjectType = cdm18.SubjectType.from(st.toString).get
  implicit def privilegeLevel(pl: cdm17.PrivilegeLevel): cdm18.PrivilegeLevel = cdm18.PrivilegeLevel.from(pl.toString).get
  implicit def srcSinkType(pl: cdm17.SrcSinkType): cdm18.SrcSinkType = cdm18.SrcSinkType.from(pl.toString).get
  // TODO instrumentation sources
  implicit def principalType(pt: cdm17.PrincipalType): cdm18.PrincipalType = cdm18.PrincipalType.from(pt.toString).get
  implicit def eventType(e: cdm17.EventType): cdm18.EventType = e match {
    case cdm17.EVENT_FNCTL => cdm18.EVENT_FCNTL
    case _ => cdm18.EventType.from(e.toString).get
  }
  implicit def FileObjectType(fo: cdm17.FileObjectType): cdm18.FileObjectType = cdm18.FileObjectType.from(fo.toString).get
  implicit def ValueType(v: cdm17.ValueType): cdm18.ValueType = cdm18.ValueType.from(v.toString).get
  implicit def ValueDataType(v: cdm17.ValueDataType): cdm18.ValueDataType = cdm18.ValueDataType.from(v.toString).get
  implicit def tagOpCode(t: cdm17.TagOpCode): cdm18.TagOpCode = cdm18.TagOpCode.from(t.toString).get
  implicit def integrityTag(i: cdm17.IntegrityTag): cdm18.IntegrityTag = cdm18.IntegrityTag.from(i.toString).get
  implicit def confidentialityTag(c: cdm17.ConfidentialityTag): cdm18.ConfidentialityTag = cdm18.ConfidentialityTag.from(c.toString).get
  implicit def cryptoHashType(c: cdm17.CryptoHashType): cdm18.CryptoHashType = cdm18.CryptoHashType.from(c.toString).get

  // Value types
  implicit def fixedShort(f: cdm17.FixedShort): cdm18.FixedShort = cdm18.FixedShort(f.bytes)
  implicit def abstractObject(a: cdm17.AbstractObject): cdm18.AbstractObject = cdm18.AbstractObject(
    None,   //a.permission.map(fixedShort),
    a.epoch,
    a.properties
  )
  implicit def cryptographicHash(c: cdm17.CryptographicHash): cdm18.CryptographicHash = cdm18.CryptographicHash(
    c.cryptoType,
    c.hash
  )
  implicit def tagRunLengthTuple(t: cdm17.TagRunLengthTuple): cdm18.TagRunLengthTuple = cdm18.TagRunLengthTuple(
    t.numValueElements,
    t.tagId
  )
  implicit def value(v: cdm17.Value): cdm18.Value = cdm18.Value(
    v.size,
    v.valueType,
    v.valueDataType,
    v.isNull,
    v.name,
    v.runtimeDataType,
    v.valueBytes,
    None,
    v.tagRunLengthTuples.map(s => s.map(tagRunLengthTuple)),
    v.components.map(_.map(value))
  )


  // Node types
  implicit def event(e: cdm17.Event)(implicit dummyHost: UUID): cdm18.Event = cdm18.Event(
    e.uuid,
    Some(e.sequence),
    e.eventType,
    Some(e.threadId),
    dummyHost,
    Some(e.subject.target),
    e.timestampNanos,
    e.predicateObject.map(_.target),
    e.predicateObjectPath,
    e.predicateObject2.map(_.target),
    e.predicateObject2Path,
    e.name,
    e.parameters.map(_.map(value)),
    e.location,
    e.size,
    e.programPoint,
    e.properties
  )
  implicit def fileObject(f: cdm17.FileObject): cdm18.FileObject = cdm18.FileObject(
    f.uuid,
    f.baseObject,
    f.fileObjectType,
    f.fileDescriptor,
    f.localPrincipal.map(_.target),
    f.size,
    f.peInfo,
    f.hashes.map(_.map(cryptographicHash))
  )
  implicit def memoryObject(m: cdm17.MemoryObject): cdm18.MemoryObject = cdm18.MemoryObject(
    m.uuid,
    m.baseObject,
    m.memoryAddress,
    m.pageNumber,
    m.pageOffset,
    m.size
  )
  implicit def netFlowObject(n: cdm17.NetFlowObject): cdm18.NetFlowObject = cdm18.NetFlowObject(
    n.uuid: UUID,
    n.baseObject,
    n.localAddress,
    n.localPort,
    n.remoteAddress,
    n.remotePort,
    n.ipProtocol,
    n.fileDescriptor
  )
  implicit def principal(p: cdm17.Principal)(implicit dummyHost: UUID): cdm18.Principal = cdm18.Principal(
    p.uuid,
    p.userId,
    p.groupIds,
    p.principalType,
    dummyHost,
    p.username,
    p.properties
  )
  implicit def provenanceTagNode(p: cdm17.ProvenanceTagNode)(implicit dummyHost: UUID): cdm18.ProvenanceTagNode = cdm18.ProvenanceTagNode(
    p.tagIdUuid,
    p.subjectUuid,
    p.flowObject,
    dummyHost,
    p.systemCall,
    p.programPoint,
    p.prevTagId,
    p.opcode.map(tagOpCode),
    p.tagIds,
    p.itag.map(integrityTag),
    p.ctag.map(confidentialityTag),
    p.properties
  )
  implicit def registryKeyObject(r: cdm17.RegistryKeyObject): cdm18.RegistryKeyObject = cdm18.RegistryKeyObject(
    r.uuid,
    r.baseObject,
    r.key,
    r.value.map(value),
    r.size
  )
  implicit def srcSinkObject(s: cdm17.SrcSinkObject): cdm18.SrcSinkObject = cdm18.SrcSinkObject(
    s.uuid,
    s.baseObject,
    s.srcSinkType,
    s.fileDescriptor
  )
  implicit def subject(s: cdm17.Subject)(implicit dummyHost: UUID): cdm18.Subject = cdm18.Subject(
    s.uuid,
    s.subjectType,
    s.pid,
    s.localPrincipal,
    s.startTimestampNanos,
    s.parentSubject,
    dummyHost,
    s.unitId,
    s.iteration,
    s.count,
    s.cmdLine,
    s.privilegeLevel.map(privilegeLevel),
    s.importedLibraries,
    s.exportedLibraries,
    s.properties
  )
  implicit def timeMarker(t: cdm17.TimeMarker): cdm18.TimeMarker = cdm18.TimeMarker(
    t.timestampNanos
  )
  implicit def unitDependency(u: cdm17.UnitDependency): cdm18.UnitDependency = cdm18.UnitDependency(
    u.unit,
    u.dependentUnit
  )
  implicit def unnamedPipeObject(u: cdm17.UnnamedPipeObject): cdm18.UnnamedPipeObject = cdm18.UnnamedPipeObject(
    u.uuid,
    u.baseObject,
    Some(u.sourceFileDescriptor),
    None,
    Some(u.sinkFileDescriptor),
    None
  )



  
}
