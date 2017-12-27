/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm18;
@SuppressWarnings("all")
/** * The confidentiality tag may be used to specify the initial confidentiality of an entity,
     * or to declassify its content after performing appropriate checking/sanitization. */
@org.apache.avro.specific.AvroGenerated
public enum ConfidentialityTag {
  CONFIDENTIALITY_SECRET, CONFIDENTIALITY_SENSITIVE, CONFIDENTIALITY_PRIVATE, CONFIDENTIALITY_PUBLIC  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"ConfidentialityTag\",\"namespace\":\"com.bbn.tc.schema.avro.cdm18\",\"doc\":\"* The confidentiality tag may be used to specify the initial confidentiality of an entity,\\n     * or to declassify its content after performing appropriate checking/sanitization.\",\"symbols\":[\"CONFIDENTIALITY_SECRET\",\"CONFIDENTIALITY_SENSITIVE\",\"CONFIDENTIALITY_PRIVATE\",\"CONFIDENTIALITY_PUBLIC\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
