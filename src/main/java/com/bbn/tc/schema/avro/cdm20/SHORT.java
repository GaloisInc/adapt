/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm20;
@SuppressWarnings("all")
@org.apache.avro.specific.FixedSize(2)
@org.apache.avro.specific.AvroGenerated
public class SHORT extends org.apache.avro.specific.SpecificFixed {
  private static final long serialVersionUID = -3827378019340639114L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"fixed\",\"name\":\"SHORT\",\"namespace\":\"com.bbn.tc.schema.avro.cdm20\",\"size\":2}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  /** Creates a new SHORT */
  public SHORT() {
    super();
  }

  /**
   * Creates a new SHORT with the given bytes.
   * @param bytes The bytes to create the new SHORT.
   */
  public SHORT(byte[] bytes) {
    super(bytes);
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter<SHORT>(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, org.apache.avro.specific.SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader<SHORT>(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, org.apache.avro.specific.SpecificData.getDecoder(in));
  }

}
