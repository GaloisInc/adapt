/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm19;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents a page in memory. Instantiates an AbstractObject.
     * TODO: is memory really an object (with permissions and so on) or is it a transient data? */
@org.apache.avro.specific.AvroGenerated
public class MemoryObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -4631032549660114378L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"MemoryObject\",\"namespace\":\"com.bbn.tc.schema.avro.cdm19\",\"doc\":\"* Represents a page in memory. Instantiates an AbstractObject.\\n     * TODO: is memory really an object (with permissions and so on) or is it a transient data?\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"doc\":\"* A host MUST NOT reuse UUIDs at all within their system, even\\n     * across restarts, and definitely not for 2 distinct objects\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which\\n     *  could include sockets, files, memory, and any data in general\\n     *  that can be an input and/or output to an event.  This record\\n     *  is intended to be abstract i.e., one should not instantiate an\\n     *  Object but rather instantiate one of its sub types (ie,\\n     *  encapsulating records) FileObject, UnnamedPipeObject,\\n     *  RegistryKeyObject, NetFlowObject, MemoryObject, or\\n     *  SrcSinkObject.\",\"fields\":[{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"epoch\",\"type\":[\"null\",\"int\"],\"doc\":\"* Used to track when an object is deleted and a new one is\\n         * created with the same identifier. This is useful for when\\n         * UUIDs are based on something not likely to be unique, such\\n         * as file path.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},\"doc\":\"The base object attributes\"},{\"name\":\"memoryAddress\",\"type\":\"long\",\"doc\":\"The memory address\"},{\"name\":\"pageNumber\",\"type\":[\"null\",\"long\"],\"doc\":\"(Optional) decomposed memory addressed into pageNumber and pageOffset\",\"default\":null},{\"name\":\"pageOffset\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"The entry size in bytes (Optional)\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.cdm19.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.cdm19.AbstractObject baseObject;
  /** The memory address */
  @Deprecated public long memoryAddress;
  /** (Optional) decomposed memory addressed into pageNumber and pageOffset */
  @Deprecated public java.lang.Long pageNumber;
  @Deprecated public java.lang.Long pageOffset;
  /** The entry size in bytes (Optional) */
  @Deprecated public java.lang.Long size;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public MemoryObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param memoryAddress The memory address
   * @param pageNumber (Optional) decomposed memory addressed into pageNumber and pageOffset
   * @param pageOffset The new value for pageOffset
   * @param size The entry size in bytes (Optional)
   */
  public MemoryObject(com.bbn.tc.schema.avro.cdm19.UUID uuid, com.bbn.tc.schema.avro.cdm19.AbstractObject baseObject, java.lang.Long memoryAddress, java.lang.Long pageNumber, java.lang.Long pageOffset, java.lang.Long size) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.memoryAddress = memoryAddress;
    this.pageNumber = pageNumber;
    this.pageOffset = pageOffset;
    this.size = size;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return memoryAddress;
    case 3: return pageNumber;
    case 4: return pageOffset;
    case 5: return size;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.cdm19.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.cdm19.AbstractObject)value$; break;
    case 2: memoryAddress = (java.lang.Long)value$; break;
    case 3: pageNumber = (java.lang.Long)value$; break;
    case 4: pageOffset = (java.lang.Long)value$; break;
    case 5: size = (java.lang.Long)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return Universally unique identifier for the object
   */
  public com.bbn.tc.schema.avro.cdm19.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * Universally unique identifier for the object
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.cdm19.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'baseObject' field.
   * @return The base object attributes
   */
  public com.bbn.tc.schema.avro.cdm19.AbstractObject getBaseObject() {
    return baseObject;
  }

  /**
   * Sets the value of the 'baseObject' field.
   * The base object attributes
   * @param value the value to set.
   */
  public void setBaseObject(com.bbn.tc.schema.avro.cdm19.AbstractObject value) {
    this.baseObject = value;
  }

  /**
   * Gets the value of the 'memoryAddress' field.
   * @return The memory address
   */
  public java.lang.Long getMemoryAddress() {
    return memoryAddress;
  }

  /**
   * Sets the value of the 'memoryAddress' field.
   * The memory address
   * @param value the value to set.
   */
  public void setMemoryAddress(java.lang.Long value) {
    this.memoryAddress = value;
  }

  /**
   * Gets the value of the 'pageNumber' field.
   * @return (Optional) decomposed memory addressed into pageNumber and pageOffset
   */
  public java.lang.Long getPageNumber() {
    return pageNumber;
  }

  /**
   * Sets the value of the 'pageNumber' field.
   * (Optional) decomposed memory addressed into pageNumber and pageOffset
   * @param value the value to set.
   */
  public void setPageNumber(java.lang.Long value) {
    this.pageNumber = value;
  }

  /**
   * Gets the value of the 'pageOffset' field.
   * @return The value of the 'pageOffset' field.
   */
  public java.lang.Long getPageOffset() {
    return pageOffset;
  }

  /**
   * Sets the value of the 'pageOffset' field.
   * @param value the value to set.
   */
  public void setPageOffset(java.lang.Long value) {
    this.pageOffset = value;
  }

  /**
   * Gets the value of the 'size' field.
   * @return The entry size in bytes (Optional)
   */
  public java.lang.Long getSize() {
    return size;
  }

  /**
   * Sets the value of the 'size' field.
   * The entry size in bytes (Optional)
   * @param value the value to set.
   */
  public void setSize(java.lang.Long value) {
    this.size = value;
  }

  /**
   * Creates a new MemoryObject RecordBuilder.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder();
  }

  /**
   * Creates a new MemoryObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder other) {
    return new com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder(other);
  }

  /**
   * Creates a new MemoryObject RecordBuilder by copying an existing MemoryObject instance.
   * @param other The existing instance to copy.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm19.MemoryObject other) {
    return new com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder(other);
  }

  /**
   * RecordBuilder for MemoryObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<MemoryObject>
    implements org.apache.avro.data.RecordBuilder<MemoryObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.cdm19.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.cdm19.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.cdm19.AbstractObject.Builder baseObjectBuilder;
    /** The memory address */
    private long memoryAddress;
    /** (Optional) decomposed memory addressed into pageNumber and pageOffset */
    private java.lang.Long pageNumber;
    private java.lang.Long pageOffset;
    /** The entry size in bytes (Optional) */
    private java.lang.Long size;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.baseObject)) {
        this.baseObject = data().deepCopy(fields()[1].schema(), other.baseObject);
        fieldSetFlags()[1] = true;
      }
      if (other.hasBaseObjectBuilder()) {
        this.baseObjectBuilder = com.bbn.tc.schema.avro.cdm19.AbstractObject.newBuilder(other.getBaseObjectBuilder());
      }
      if (isValidValue(fields()[2], other.memoryAddress)) {
        this.memoryAddress = data().deepCopy(fields()[2].schema(), other.memoryAddress);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.pageNumber)) {
        this.pageNumber = data().deepCopy(fields()[3].schema(), other.pageNumber);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.pageOffset)) {
        this.pageOffset = data().deepCopy(fields()[4].schema(), other.pageOffset);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.size)) {
        this.size = data().deepCopy(fields()[5].schema(), other.size);
        fieldSetFlags()[5] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing MemoryObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm19.MemoryObject other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.baseObject)) {
        this.baseObject = data().deepCopy(fields()[1].schema(), other.baseObject);
        fieldSetFlags()[1] = true;
      }
      this.baseObjectBuilder = null;
      if (isValidValue(fields()[2], other.memoryAddress)) {
        this.memoryAddress = data().deepCopy(fields()[2].schema(), other.memoryAddress);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.pageNumber)) {
        this.pageNumber = data().deepCopy(fields()[3].schema(), other.pageNumber);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.pageOffset)) {
        this.pageOffset = data().deepCopy(fields()[4].schema(), other.pageOffset);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.size)) {
        this.size = data().deepCopy(fields()[5].schema(), other.size);
        fieldSetFlags()[5] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm19.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setUuid(com.bbn.tc.schema.avro.cdm19.UUID value) {
      validate(fields()[0], value);
      this.uuid = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'uuid' field has been set.
      * Universally unique identifier for the object
      * @return True if the 'uuid' field has been set, false otherwise.
      */
    public boolean hasUuid() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'baseObject' field.
      * The base object attributes
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm19.AbstractObject getBaseObject() {
      return baseObject;
    }

    /**
      * Sets the value of the 'baseObject' field.
      * The base object attributes
      * @param value The value of 'baseObject'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setBaseObject(com.bbn.tc.schema.avro.cdm19.AbstractObject value) {
      validate(fields()[1], value);
      this.baseObjectBuilder = null;
      this.baseObject = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'baseObject' field has been set.
      * The base object attributes
      * @return True if the 'baseObject' field has been set, false otherwise.
      */
    public boolean hasBaseObject() {
      return fieldSetFlags()[1];
    }

    /**
     * Gets the Builder instance for the 'baseObject' field and creates one if it doesn't exist yet.
     * The base object attributes
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.cdm19.AbstractObject.Builder getBaseObjectBuilder() {
      if (baseObjectBuilder == null) {
        if (hasBaseObject()) {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm19.AbstractObject.newBuilder(baseObject));
        } else {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm19.AbstractObject.newBuilder());
        }
      }
      return baseObjectBuilder;
    }

    /**
     * Sets the Builder instance for the 'baseObject' field
     * The base object attributes
     * @param value The builder instance that must be set.
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm19.AbstractObject.Builder value) {
      clearBaseObject();
      baseObjectBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'baseObject' field has an active Builder instance
     * The base object attributes
     * @return True if the 'baseObject' field has an active Builder instance
     */
    public boolean hasBaseObjectBuilder() {
      return baseObjectBuilder != null;
    }

    /**
      * Clears the value of the 'baseObject' field.
      * The base object attributes
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'memoryAddress' field.
      * The memory address
      * @return The value.
      */
    public java.lang.Long getMemoryAddress() {
      return memoryAddress;
    }

    /**
      * Sets the value of the 'memoryAddress' field.
      * The memory address
      * @param value The value of 'memoryAddress'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setMemoryAddress(long value) {
      validate(fields()[2], value);
      this.memoryAddress = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'memoryAddress' field has been set.
      * The memory address
      * @return True if the 'memoryAddress' field has been set, false otherwise.
      */
    public boolean hasMemoryAddress() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'memoryAddress' field.
      * The memory address
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearMemoryAddress() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'pageNumber' field.
      * (Optional) decomposed memory addressed into pageNumber and pageOffset
      * @return The value.
      */
    public java.lang.Long getPageNumber() {
      return pageNumber;
    }

    /**
      * Sets the value of the 'pageNumber' field.
      * (Optional) decomposed memory addressed into pageNumber and pageOffset
      * @param value The value of 'pageNumber'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setPageNumber(java.lang.Long value) {
      validate(fields()[3], value);
      this.pageNumber = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'pageNumber' field has been set.
      * (Optional) decomposed memory addressed into pageNumber and pageOffset
      * @return True if the 'pageNumber' field has been set, false otherwise.
      */
    public boolean hasPageNumber() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'pageNumber' field.
      * (Optional) decomposed memory addressed into pageNumber and pageOffset
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearPageNumber() {
      pageNumber = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'pageOffset' field.
      * @return The value.
      */
    public java.lang.Long getPageOffset() {
      return pageOffset;
    }

    /**
      * Sets the value of the 'pageOffset' field.
      * @param value The value of 'pageOffset'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setPageOffset(java.lang.Long value) {
      validate(fields()[4], value);
      this.pageOffset = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'pageOffset' field has been set.
      * @return True if the 'pageOffset' field has been set, false otherwise.
      */
    public boolean hasPageOffset() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'pageOffset' field.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearPageOffset() {
      pageOffset = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'size' field.
      * The entry size in bytes (Optional)
      * @return The value.
      */
    public java.lang.Long getSize() {
      return size;
    }

    /**
      * Sets the value of the 'size' field.
      * The entry size in bytes (Optional)
      * @param value The value of 'size'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder setSize(java.lang.Long value) {
      validate(fields()[5], value);
      this.size = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'size' field has been set.
      * The entry size in bytes (Optional)
      * @return True if the 'size' field has been set, false otherwise.
      */
    public boolean hasSize() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'size' field.
      * The entry size in bytes (Optional)
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm19.MemoryObject.Builder clearSize() {
      size = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public MemoryObject build() {
      try {
        MemoryObject record = new MemoryObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.cdm19.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.cdm19.AbstractObject) defaultValue(fields()[1]);
        }
        record.memoryAddress = fieldSetFlags()[2] ? this.memoryAddress : (java.lang.Long) defaultValue(fields()[2]);
        record.pageNumber = fieldSetFlags()[3] ? this.pageNumber : (java.lang.Long) defaultValue(fields()[3]);
        record.pageOffset = fieldSetFlags()[4] ? this.pageOffset : (java.lang.Long) defaultValue(fields()[4]);
        record.size = fieldSetFlags()[5] ? this.size : (java.lang.Long) defaultValue(fields()[5]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}
