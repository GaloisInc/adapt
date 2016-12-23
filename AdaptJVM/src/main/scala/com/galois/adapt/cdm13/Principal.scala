package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try


case class Principal(
  uuid: UUID,
  userId: String,
  groupIds: Seq[String],
  source: InstrumentationSource,
  principalType: PrincipalType = PRINCIPAL_LOCAL,
  properties: Option[Map[String,String]] = None
) extends CDM13

case object Principal extends CDM13Constructor[Principal] {
  type RawCDMType = com.bbn.tc.schema.avro.Principal

  def from(cdm: RawCDM13Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds,
      cdm.getSource,
      cdm.getType,
      AvroOpt.map(cdm.getProperties)
    )
  }
}