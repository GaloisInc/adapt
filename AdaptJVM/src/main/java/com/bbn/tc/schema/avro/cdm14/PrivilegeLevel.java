/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;
@SuppressWarnings("all")
/** * Windows allows Subjects (processes) to have the following
     * enumerated privilege levels. */
@org.apache.avro.specific.AvroGenerated
public enum PrivilegeLevel {
  LIMITED, ELEVATED, FULL  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"PrivilegeLevel\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* Windows allows Subjects (processes) to have the following\\n     * enumerated privilege levels.\",\"symbols\":[\"LIMITED\",\"ELEVATED\",\"FULL\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
