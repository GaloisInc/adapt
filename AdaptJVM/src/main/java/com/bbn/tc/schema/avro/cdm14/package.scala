package com.bbn.tc.schema.avro

import com.bbn.tc.schema.{avro => toplevel}

package object cdm14 {
  type AbstractObject        = toplevel.AbstractObject        
  type ConfidentialityTag    = toplevel.ConfidentialityTag
  type CryptoHashType        = toplevel.CryptoHashType
  type CryptographicHash     = toplevel.CryptographicHash
  type Event                 = toplevel.Event
  type EventType             = toplevel.EventType
  type FileObject            = toplevel.FileObject
  type FileObjectType        = toplevel.FileObjectType
  type InstrumentationSource = toplevel.InstrumentationSource
  type IntegrityTag          = toplevel.IntegrityTag
  type MemoryObject          = toplevel.MemoryObject
  type NetFlowObject         = toplevel.NetFlowObject
  type Principal             = toplevel.Principal
  type PrincipalType         = toplevel.PrincipalType
  type PrivilegeLevel        = toplevel.PrivilegeLevel
  type ProvenanceTagNode     = toplevel.ProvenanceTagNode
  type RegistryKeyObject     = toplevel.RegistryKeyObject
  type SHORT                 = toplevel.SHORT
  type SrcSinkObject         = toplevel.SrcSinkObject
  type SrcSinkType           = toplevel.SrcSinkType
  type Subject               = toplevel.Subject
  type SubjectType           = toplevel.SubjectType
  type TCCDMDataProtocol     = toplevel.TCCDMDataProtocol
  type TCCDMDatum            = toplevel.TCCDMDatum
  type TagOpCode             = toplevel.TagOpCode
  type TagRunLengthTuple     = toplevel.TagRunLengthTuple
  type TimeMarker            = toplevel.TimeMarker
  type UUID                  = toplevel.UUID
  type UnnamedPipeObject     = toplevel.UnnamedPipeObject
  type Value                 = toplevel.Value
  type ValueDataType         = toplevel.ValueDataType
  type ValueType             = toplevel.ValueType
}
