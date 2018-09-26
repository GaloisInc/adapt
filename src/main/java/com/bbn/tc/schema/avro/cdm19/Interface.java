/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm19;

import org.apache.avro.specific.SpecificData;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@SuppressWarnings("all")
/** Interface name and addresses */
@org.apache.avro.specific.AvroGenerated
public class Interface extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1774841784080413569L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Interface\",\"namespace\":\"com.bbn.tc.schema.avro.cdm19\",\"doc\":\"Interface name and addresses\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"macAddress\",\"type\":\"string\"},{\"name\":\"ipAddresses\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"string\"}],\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<Interface> ENCODER =
      new BinaryMessageEncoder<Interface>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Interface> DECODER =
      new BinaryMessageDecoder<Interface>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   */
  public static BinaryMessageDecoder<Interface> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   */
  public static BinaryMessageDecoder<Interface> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<Interface>(MODEL$, SCHEMA$, resolver);
  }

  /** Serializes this Interface to a ByteBuffer. */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /** Deserializes a Interface from a ByteBuffer. */
  public static Interface fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  @Deprecated public java.lang.CharSequence name;
  @Deprecated public java.lang.CharSequence macAddress;
  @Deprecated public java.util.List<java.lang.CharSequence> ipAddresses;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Interface() {}

  /**
   * All-args constructor.
   * @param name The new value for name
   * @param macAddress The new value for macAddress
   * @param ipAddresses The new value for ipAddresses
   */
  public Interface(java.lang.CharSequence name, java.lang.CharSequence macAddress, java.util.List<java.lang.CharSequence> ipAddresses) {
    this.name = name;
    this.macAddress = macAddress;
    this.ipAddresses = ipAddresses;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return name;
    case 1: return macAddress;
    case 2: return ipAddresses;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: name = (java.lang.CharSequence)value$; break;
    case 1: macAddress = (java.lang.CharSequence)value$; break;
    case 2: ipAddresses = (java.util.List<java.lang.CharSequence>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'name' field.
   * @return The value of the 'name' field.
   */
  public java.lang.CharSequence getName() {
    return name;
  }

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  public void setName(java.lang.CharSequence value) {
    this.name = value;
  }

  /**
   * Gets the value of the 'macAddress' field.
   * @return The value of the 'macAddress' field.
   */
  public java.lang.CharSequence getMacAddress() {
    return macAddress;
  }

  /**
   * Sets the value of the 'macAddress' field.
   * @param value the value to set.
   */
  public void setMacAddress(java.lang.CharSequence value) {
    this.macAddress = value;
  }

  /**
   * Gets the value of the 'ipAddresses' field.
   * @return The value of the 'ipAddresses' field.
   */
  public java.util.List<java.lang.CharSequence> getIpAddresses() {
    return ipAddresses;
  }

  /**
   * Sets the value of the 'ipAddresses' field.
   * @param value the value to set.
   */
  public void setIpAddresses(java.util.List<java.lang.CharSequence> value) {
    this.ipAddresses = value;
  }

  /**
   * Creates a new Interface RecordBuilder.
   * @return A new Interface RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.Interface.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.cdm19.Interface.Builder();
  }

  /**
   * Creates a new Interface RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Interface RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.Interface.Builder newBuilder(com.bbn.tc.schema.avro.cdm19.Interface.Builder other) {
    return new com.bbn.tc.schema.avro.cdm19.Interface.Builder(other);
  }

  /**
   * Creates a new Interface RecordBuilder by copying an existing Interface instance.
   * @param other The existing instance to copy.
   * @return A new Interface RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.Interface.Builder newBuilder(com.bbn.tc.schema.avro.cdm19.Interface other) {
    return new com.bbn.tc.schema.avro.cdm19.Interface.Builder(other);
  }

  /**
   * RecordBuilder for Interface instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Interface>
    implements org.apache.avro.data.RecordBuilder<Interface> {

    private java.lang.CharSequence name;
    private java.lang.CharSequence macAddress;
    private java.util.List<java.lang.CharSequence> ipAddresses;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm19.Interface.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.name)) {
        this.name = data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.macAddress)) {
        this.macAddress = data().deepCopy(fields()[1].schema(), other.macAddress);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.ipAddresses)) {
        this.ipAddresses = data().deepCopy(fields()[2].schema(), other.ipAddresses);
        fieldSetFlags()[2] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing Interface instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm19.Interface other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.name)) {
        this.name = data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.macAddress)) {
        this.macAddress = data().deepCopy(fields()[1].schema(), other.macAddress);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.ipAddresses)) {
        this.ipAddresses = data().deepCopy(fields()[2].schema(), other.ipAddresses);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'name' field.
      * @return The value.
      */
    public java.lang.CharSequence getName() {
      return name;
    }

    /**
      * Sets the value of the 'name' field.
      * @param value The value of 'name'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder setName(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.name = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'name' field has been set.
      * @return True if the 'name' field has been set, false otherwise.
      */
    public boolean hasName() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'name' field.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder clearName() {
      name = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'macAddress' field.
      * @return The value.
      */
    public java.lang.CharSequence getMacAddress() {
      return macAddress;
    }

    /**
      * Sets the value of the 'macAddress' field.
      * @param value The value of 'macAddress'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder setMacAddress(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.macAddress = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'macAddress' field has been set.
      * @return True if the 'macAddress' field has been set, false otherwise.
      */
    public boolean hasMacAddress() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'macAddress' field.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder clearMacAddress() {
      macAddress = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'ipAddresses' field.
      * @return The value.
      */
    public java.util.List<java.lang.CharSequence> getIpAddresses() {
      return ipAddresses;
    }

    /**
      * Sets the value of the 'ipAddresses' field.
      * @param value The value of 'ipAddresses'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder setIpAddresses(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[2], value);
      this.ipAddresses = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'ipAddresses' field has been set.
      * @return True if the 'ipAddresses' field has been set, false otherwise.
      */
    public boolean hasIpAddresses() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'ipAddresses' field.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.Interface.Builder clearIpAddresses() {
      ipAddresses = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Interface build() {
      try {
        Interface record = new Interface();
        record.name = fieldSetFlags()[0] ? this.name : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.macAddress = fieldSetFlags()[1] ? this.macAddress : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.ipAddresses = fieldSetFlags()[2] ? this.ipAddresses : (java.util.List<java.lang.CharSequence>) defaultValue(fields()[2]);
        return record;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Interface>
    WRITER$ = (org.apache.avro.io.DatumWriter<Interface>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Interface>
    READER$ = (org.apache.avro.io.DatumReader<Interface>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}
