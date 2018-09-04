package com.galois.adapt.cdm19

import com.galois.adapt.cdm18
import com.galois.adapt.cdm19
import java.util.UUID

// This object provides implicit conversions from CDM17 to CDM19 Scala case classes
object Cdm18to19 {

  // Enumerations
  implicit def subjectType(st: cdm18.SubjectType): cdm19.SubjectType = cdm19.SubjectType.from(st.toString).get
  implicit def privilegeLevel(pl: cdm18.PrivilegeLevel): cdm19.PrivilegeLevel = cdm19.PrivilegeLevel.from(pl.toString).get
  implicit def srcSinkType(pl: cdm18.SrcSinkType): cdm19.SrcSinkType = cdm19.SrcSinkType.from(pl.toString).get
  // TODO instrumentation sources
  implicit def principalType(pt: cdm18.PrincipalType): cdm19.PrincipalType = cdm19.PrincipalType.from(pt.toString).get
  implicit def eventType(e: cdm18.EventType): cdm19.EventType = cdm19.EventType.from(e.toString).get
  implicit def FileObjectType(fo: cdm18.FileObjectType): cdm19.FileObjectType = cdm19.FileObjectType.from(fo.toString).get
  implicit def ValueType(v: cdm18.ValueType): cdm19.ValueType = cdm19.ValueType.from(v.toString).get
  implicit def ValueDataType(v: cdm18.ValueDataType): cdm19.ValueDataType = cdm19.ValueDataType.from(v.toString).get
  implicit def tagOpCode(t: cdm18.TagOpCode): cdm19.TagOpCode = cdm19.TagOpCode.from(t.toString).get
  implicit def integrityTag(i: cdm18.IntegrityTag): cdm19.IntegrityTag = cdm19.IntegrityTag.from(i.toString).get
  implicit def confidentialityTag(c: cdm18.ConfidentialityTag): cdm19.ConfidentialityTag = cdm19.ConfidentialityTag.from(c.toString).get
  implicit def cryptoHashType(c: cdm18.CryptoHashType): cdm19.CryptoHashType = cdm19.CryptoHashType.from(c.toString).get

  // Value types
  implicit def fixedShort(f: cdm18.FixedShort): cdm19.FixedShort = cdm19.FixedShort(f.bytes)
  implicit def abstractObject(a: cdm18.AbstractObject): cdm19.AbstractObject = cdm19.AbstractObject(
    a.permission.map(fixedShort),
    a.epoch,
    a.properties
  )
  implicit def cryptographicHash(c: cdm18.CryptographicHash): cdm19.CryptographicHash = cdm19.CryptographicHash(
    c.cryptoType,
    c.hash
  )
  implicit def tagRunLengthTuple(t: cdm18.TagRunLengthTuple): cdm19.TagRunLengthTuple = cdm19.TagRunLengthTuple(
    t.numValueElements,
    t.tagId
  )
  implicit def value(v: cdm18.Value): cdm19.Value = cdm19.Value(
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
  implicit def event(e: cdm18.Event): cdm19.Event = cdm19.Event(
    e.uuid,
    e.sequence,
    e.eventType,
    e.threadId,
    e.host,
    e.subjectUuid,
    e.timestampNanos,
    e.predicateObject,
    e.predicateObjectPath,
    e.predicateObject2,
    e.predicateObject2Path,
    e.name.toList,
    e.parameters.map(_.map(value)),
    e.location,
    e.size,
    e.programPoint,
    e.properties
  )
  implicit def fileObject(f: cdm18.FileObject)(implicit dummyHost: UUID): cdm19.FileObject = cdm19.FileObject(
    f.uuid,
    dummyHost,
    f.baseObject,
    f.fileObjectType,
    f.fileDescriptor,
    f.localPrincipal,
    f.size,
    f.peInfo,
    f.hashes.map(_.map(cryptographicHash))
  )
  implicit def memoryObject(m: cdm18.MemoryObject)(implicit dummyHost: UUID): cdm19.MemoryObject = cdm19.MemoryObject(
    m.uuid,
    dummyHost,
    m.baseObject,
    m.memoryAddress,
    m.pageNumber,
    m.pageOffset,
    m.size
  )
  implicit def netFlowObject(n: cdm18.NetFlowObject)(implicit dummyHost: UUID): cdm19.NetFlowObject = cdm19.NetFlowObject(
    n.uuid: UUID,
    dummyHost,
    n.baseObject,
    Some(n.localAddress),
    Some(n.localPort),
    Some(n.remoteAddress),
    Some(n.remotePort),
    n.ipProtocol,
    n.fileDescriptor
  )
  implicit def principal(p: cdm18.Principal)(implicit dummyHost: UUID): cdm19.Principal = cdm19.Principal(
    p.uuid,
    p.userId,
    p.groupIds,
    p.principalType,
    dummyHost,
    p.username,
    p.properties
  )
  implicit def provenanceTagNode(p: cdm18.ProvenanceTagNode)(implicit dummyHost: UUID): cdm19.ProvenanceTagNode = cdm19.ProvenanceTagNode(
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
  implicit def registryKeyObject(r: cdm18.RegistryKeyObject)(implicit dummyHost: UUID): cdm19.RegistryKeyObject = cdm19.RegistryKeyObject(
    r.uuid,
    dummyHost,
    r.baseObject,
    r.key,
    r.value.map(value),
    r.size
  )
  implicit def srcSinkObject(s: cdm18.SrcSinkObject)(implicit dummyHost: UUID): cdm19.SrcSinkObject = cdm19.SrcSinkObject(
    s.uuid,
    dummyHost,
    s.baseObject,
    s.srcSinkType,
    s.fileDescriptor
  )
  implicit def subject(s: cdm18.Subject): cdm19.Subject = cdm19.Subject(
    s.uuid,
    s.subjectType,
    s.cid,
    Some(s.localPrincipal),
    Some(s.startTimestampNanos),
    s.parentSubject,
    s.host,
    s.unitId,
    s.iteration,
    s.count,
    s.cmdLine,
    s.privilegeLevel.map(privilegeLevel),
    s.importedLibraries,
    s.exportedLibraries,
    s.properties
  )
  implicit def timeMarker(t: cdm18.TimeMarker)(implicit dummyHost: UUID): cdm19.TimeMarker = cdm19.TimeMarker(
    t.timestampNanos,
    dummyHost
  )
  implicit def unitDependency(u: cdm18.UnitDependency)(implicit dummyHost: UUID): cdm19.UnitDependency = cdm19.UnitDependency(
    u.unit,
    u.dependentUnit,
    dummyHost
  )
  implicit def ipcObject(u: cdm18.UnnamedPipeObject)(implicit dummyHost: UUID): cdm19.IpcObject = cdm19.IpcObject(
    u.uuid,
    dummyHost,
    u.baseObject,
    IPC_OBJECT_PIPE_UNNAMED,
    u.source,
    u.sink,
    u.sourceFileDescriptor,
    u.sinkFileDescriptor
  )
  
}
