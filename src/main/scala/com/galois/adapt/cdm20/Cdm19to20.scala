package com.galois.adapt.cdm20

import com.galois.adapt.cdm19
import com.galois.adapt.cdm20
import java.util.UUID

// This object provides implicit conversions from CDM17 to CDM20 Scala case classes
object Cdm19to20 {

  // Enumerations
  implicit def subjectType(st: cdm19.SubjectType): cdm20.SubjectType = cdm20.SubjectType.from(st.toString).get
  implicit def privilegeLevel(pl: cdm19.PrivilegeLevel): cdm20.PrivilegeLevel = cdm20.PrivilegeLevel.from(pl.toString).get
  implicit def srcSinkType(pl: cdm19.SrcSinkType): cdm20.SrcSinkType = cdm20.SrcSinkType.from(pl.toString).get
  implicit def instrumentationSource(is: cdm19.InstrumentationSource): cdm20.InstrumentationSource = cdm20.InstrumentationSource.from(is.toString).get
  implicit def principalType(pt: cdm19.PrincipalType): cdm20.PrincipalType = cdm20.PrincipalType.from(pt.toString).get
  implicit def eventType(e: cdm19.EventType): cdm20.EventType = cdm20.EventType.from(e.toString).get
  implicit def FileObjectType(fo: cdm19.FileObjectType): cdm20.FileObjectType = cdm20.FileObjectType.from(fo.toString).get
  implicit def ValueType(v: cdm19.ValueType): cdm20.ValueType = cdm20.ValueType.from(v.toString).get
  implicit def ValueDataType(v: cdm19.ValueDataType): cdm20.ValueDataType = cdm20.ValueDataType.from(v.toString).get
  implicit def tagOpCode(t: cdm19.TagOpCode): cdm20.TagOpCode = cdm20.TagOpCode.from(t.toString).get
  implicit def integrityTag(i: cdm19.IntegrityTag): cdm20.IntegrityTag = cdm20.IntegrityTag.from(i.toString).get
  implicit def confidentialityTag(c: cdm19.ConfidentialityTag): cdm20.ConfidentialityTag = cdm20.ConfidentialityTag.from(c.toString).get
  implicit def cryptoHashType(c: cdm19.CryptoHashType): cdm20.CryptoHashType = cdm20.CryptoHashType.from(c.toString).get
  implicit def hostType(c: cdm19.HostType): cdm20.HostType = cdm20.HostType.from(c.toString).get
  implicit def ipcType(c: cdm19.IpcObjectType): cdm20.IpcObjectType = cdm20.IpcObjectType.from(c.toString).get
  implicit def fixedBytes(c: cdm19.FixedByte): cdm20.FixedByte = cdm20.FixedByte(c.bytes)

  // Value types
  implicit def fixedShort(f: cdm19.FixedShort): cdm20.FixedShort = cdm20.FixedShort(f.bytes)
  implicit def abstractObject(a: cdm19.AbstractObject): cdm20.AbstractObject = cdm20.AbstractObject(
    a.permission.map(fixedShort),
    a.epoch,
    a.properties
  )
  implicit def cryptographicHash(c: cdm19.CryptographicHash): cdm20.CryptographicHash = cdm20.CryptographicHash(
    c.cryptoType,
    c.hash
  )
  implicit def tagRunLengthTuple(t: cdm19.TagRunLengthTuple): cdm20.TagRunLengthTuple = cdm20.TagRunLengthTuple(
    t.numValueElements,
    t.tagId
  )
  implicit def value(v: cdm19.Value): cdm20.Value = cdm20.Value(
    v.size,
    v.valueType,
    v.valueDataType,
    v.isNull,
    v.name,
    v.runtimeDataType,
    v.valueBytes,
    v.provenance.map(_.map(provenanceAssertion(_))),
    v.tagRunLengthTuples.map(_.map(tagRunLengthTuple)),
    v.components.map(_.map(value))
  )
  implicit def provenanceAssertion(p: cdm19.ProvenanceAssertion): cdm20.ProvenanceAssertion = cdm20.ProvenanceAssertion(
    p.asserter,
    p.sources,
    p.provenance.map(_.map(provenanceAssertion(_)))
  )


  // Node types
  implicit def event(e: cdm19.Event): cdm20.Event = cdm20.Event(
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
    e.names,
    e.parameters.map(_.map(x => value(x))),
    e.location,
    e.size,
    e.programPoint,
    e.properties
  )
  implicit def fileObject(f: cdm19.FileObject): cdm20.FileObject = cdm20.FileObject(
    f.uuid,
    f.host,
    f.baseObject,
    f.fileObjectType,
    f.fileDescriptor,
    f.localPrincipal,
    f.size,
    f.peInfo,
    f.hashes.map(_.map(cryptographicHash))
  )
  implicit def memoryObject(m: cdm19.MemoryObject): cdm20.MemoryObject = cdm20.MemoryObject(
    m.uuid,
    m.host,
    m.baseObject,
    m.memoryAddress,
    m.pageNumber,
    m.pageOffset,
    m.size
  )
  implicit def netFlowObject(n: cdm19.NetFlowObject): cdm20.NetFlowObject = cdm20.NetFlowObject(
    n.uuid: UUID,
    n.host,
    n.baseObject,
    n.localAddress,
    n.localPort,
    n.remoteAddress,
    n.remotePort,
    n.ipProtocol,
    None,
    n.fileDescriptor
  )
  implicit def principal(p: cdm19.Principal): cdm20.Principal = cdm20.Principal(
    p.uuid,
    p.userId,
    p.groupIds,
    p.principalType,
    p.host,
    p.username,
    p.properties
  )
  implicit def provenanceTagNode(p: cdm19.ProvenanceTagNode): cdm20.ProvenanceTagNode = cdm20.ProvenanceTagNode(
    p.tagIdUuid,
    p.subjectUuid,
    p.flowObject,
    p.host,
    p.systemCall,
    p.programPoint,
    p.prevTagId,
    p.opcode.map(tagOpCode),
    p.tagIds,
    p.itag.map(integrityTag),
    p.ctag.map(confidentialityTag),
    p.properties
  )
  implicit def registryKeyObject(r: cdm19.RegistryKeyObject): cdm20.RegistryKeyObject = cdm20.RegistryKeyObject(
    r.uuid,
    r.host,
    r.baseObject,
    r.key,
    r.value.map(value),
    r.size
  )
  implicit def srcSinkObject(s: cdm19.SrcSinkObject): cdm20.SrcSinkObject = cdm20.SrcSinkObject(
    s.uuid,
    s.host,
    s.baseObject,
    s.srcSinkType,
    s.fileDescriptor
  )
  implicit def subject(s: cdm19.Subject): cdm20.Subject = cdm20.Subject(
    s.uuid,
    s.subjectType,
    s.cid,
    s.localPrincipal,
    s.startTimestampNanos,
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
  implicit def timeMarker(t: cdm19.TimeMarker): cdm20.TimeMarker = cdm20.TimeMarker(
    t.timestampNanos,
    t.host 
  )
  implicit def unitDependency(u: cdm19.UnitDependency): cdm20.UnitDependency = cdm20.UnitDependency(
    u.unit,
    u.dependentUnit,
    u.host
  )
  implicit def ipcObject(u: cdm19.IpcObject): cdm20.IpcObject = cdm20.IpcObject(
    u.uuid,
    u.host,
    u.baseObject,
    u.ipcObjectType,
    u.uuid1,
    u.uuid2,
    u.fd1,
    u.fd2
  )
  implicit def host(h: cdm19.Host): cdm20.Host = cdm20.Host(
    h.uuid,
    h.hostName,
    "",
    h.hostIdentifiers.map(i => cdm20.HostIdentifier(i.idType, i.idValue)),
    h.osDetails,
    h.hostType,
    h.interfaces.map(i => cdm20.Interface(i.name, i.macAddress, i.ipAddresses))
  )
  implicit def packetSocketObject(p: cdm19.PacketSocketObject): cdm20.PacketSocketObject = cdm20.PacketSocketObject(
    p.uuid,
    p.host,
    p.baseObject,
    p.proto,
    p.ifIndex,
    p.haType,
    p.pktType,
    p.addr
  )
}
