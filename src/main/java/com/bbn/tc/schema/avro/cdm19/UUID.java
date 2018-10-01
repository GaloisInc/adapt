/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm19;
@SuppressWarnings("all")
/** * A host MUST NOT reuse UUIDs at all within their system, even
     * across restarts, and definitely not for 2 distinct objects */
@org.apache.avro.specific.FixedSize(16)
@org.apache.avro.specific.AvroGenerated
public class UUID extends org.apache.avro.specific.SpecificFixed {
  private static final long serialVersionUID = -5281404243241447767L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"fixed\",\"name\":\"UUID\",\"namespace\":\"com.bbn.tc.schema.avro.cdm19\",\"doc\":\"* A host MUST NOT reuse UUIDs at all within their system, even\\n     * across restarts, and definitely not for 2 distinct objects\",\"size\":16}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  /** Creates a new UUID */
  public UUID() {
    super();
  }

  /**
   * Creates a new UUID with the given bytes.
   * @param bytes The bytes to create the new UUID.
   */
  public UUID(byte[] bytes) {
    super(bytes);
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter<UUID>(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, org.apache.avro.specific.SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader<UUID>(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, org.apache.avro.specific.SpecificData.getDecoder(in));
  }

}
