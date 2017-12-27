/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm18;
@SuppressWarnings("all")
/** * A value data type is one of the primitive data types. A string is treated as a char array */
@org.apache.avro.specific.AvroGenerated
public enum ValueDataType {
  VALUE_DATA_TYPE_BYTE, VALUE_DATA_TYPE_BOOL, VALUE_DATA_TYPE_CHAR, VALUE_DATA_TYPE_SHORT, VALUE_DATA_TYPE_INT, VALUE_DATA_TYPE_FLOAT, VALUE_DATA_TYPE_LONG, VALUE_DATA_TYPE_DOUBLE, VALUE_DATA_TYPE_POINTER32, VALUE_DATA_TYPE_POINTER64, VALUE_DATA_TYPE_COMPLEX  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"ValueDataType\",\"namespace\":\"com.bbn.tc.schema.avro.cdm18\",\"doc\":\"* A value data type is one of the primitive data types. A string is treated as a char array\",\"symbols\":[\"VALUE_DATA_TYPE_BYTE\",\"VALUE_DATA_TYPE_BOOL\",\"VALUE_DATA_TYPE_CHAR\",\"VALUE_DATA_TYPE_SHORT\",\"VALUE_DATA_TYPE_INT\",\"VALUE_DATA_TYPE_FLOAT\",\"VALUE_DATA_TYPE_LONG\",\"VALUE_DATA_TYPE_DOUBLE\",\"VALUE_DATA_TYPE_POINTER32\",\"VALUE_DATA_TYPE_POINTER64\",\"VALUE_DATA_TYPE_COMPLEX\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
