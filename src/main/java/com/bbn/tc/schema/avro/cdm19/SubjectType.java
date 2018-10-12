/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm19;
@SuppressWarnings("all")
/** * SubjectType enumerates the types of execution contexts supported.
     *
     * SUBJECT_PROCESS,    process
     * SUBJECT_THREAD,     thread within a process
     * SUBJECT_UNIT        so far we only know of TRACE BEEP using this */
@org.apache.avro.specific.AvroGenerated
public enum SubjectType {
  SUBJECT_PROCESS, SUBJECT_THREAD, SUBJECT_UNIT, SUBJECT_BASIC_BLOCK  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"SubjectType\",\"namespace\":\"com.bbn.tc.schema.avro.cdm19\",\"doc\":\"* SubjectType enumerates the types of execution contexts supported.\\n     *\\n     * SUBJECT_PROCESS,    process\\n     * SUBJECT_THREAD,     thread within a process\\n     * SUBJECT_UNIT        so far we only know of TRACE BEEP using this\",\"symbols\":[\"SUBJECT_PROCESS\",\"SUBJECT_THREAD\",\"SUBJECT_UNIT\",\"SUBJECT_BASIC_BLOCK\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}