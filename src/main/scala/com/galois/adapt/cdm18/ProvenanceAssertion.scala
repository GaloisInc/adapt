package com.galois.adapt.cdm18

import com.galois.adapt.DBWritable
import java.util.UUID

import com.bbn.tc.schema.avro.cdm18

import scala.util.Try

// TODO: How do we represent this (this is similar to 'Value's)

// An assertion about the provenance of information
case class ProvenanceAssertion(
  asserter: UUID, // Which Subject is making this assertion?
  sources: Option[Seq[UUID]] = None, // Object(s) that this Value's data came from.
  
  // Further provenance assertions within this assertion.
  // For example, to describe a situation in which X asserts that
  // Y asserts that Z asserts that V came from {p,q}:
  //
  // ```
  // Event {
  //   subject = X,
  //   parameters = [
  //     Value (V) {
  //         provenance = [
  //         ProvenanceAssertion {
  //           asserter = UUID of X,
  //           sources = [ UUID of p, UUID of q ],
  //           provenance = [
  //             ProvenanceAssertion {
  //               asserter = UUID of Y,
  //               provenance = [
  //                 ProvenanceAssertion {
  //                   asserter = UUID of Z,
  //                 },
  //               ],
  //             },
  //           ],
  //         },
  //       ],
  //     },
  //   ],
  // }
  // ```
  //
  // Z should have a provenance assertion. e.g. "X asserts that Y asserts that Z comes from {p,q}".,
  provenance: Option[Seq[ProvenanceAssertion]] = None
) extends CDM18 {

}

case object ProvenanceAssertion extends CDM18Constructor[ProvenanceAssertion] {
  type RawCDMType = cdm18.ProvenanceAssertion

  def from(cdm: RawCDM18Type): Try[ProvenanceAssertion] = Try {
    ProvenanceAssertion(
      cdm.getAsserter,
      AvroOpt.listUuid(cdm.getSources),
      AvroOpt.listProvenanceAssertion(cdm.getProvenance)
    )
  }
}
