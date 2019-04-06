/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm20;
@SuppressWarnings("all")
/** * The tag opcode describes the provenance relation i.e., how multiple sources are combined to
     * produce the output. We identify the following provenance relations */
@org.apache.avro.specific.AvroGenerated
public enum TagOpCode {
  TAG_OP_UNION, TAG_OP_ENCODE, TAG_OP_STRONG, TAG_OP_MEDIUM, TAG_OP_WEAK  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"TagOpCode\",\"namespace\":\"com.bbn.tc.schema.avro.cdm20\",\"doc\":\"* The tag opcode describes the provenance relation i.e., how multiple sources are combined to\\n     * produce the output. We identify the following provenance relations\",\"symbols\":[\"TAG_OP_UNION\",\"TAG_OP_ENCODE\",\"TAG_OP_STRONG\",\"TAG_OP_MEDIUM\",\"TAG_OP_WEAK\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
