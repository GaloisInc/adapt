/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents a registry key. Instantiates an AbstractObject. */
@org.apache.avro.specific.AvroGenerated
public class RegistryKeyObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1240566011831192400L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"RegistryKeyObject\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* Represents a registry key. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which\\n     *  could include sockets, files, memory, and any data in general\\n     *  that can be an input and/or output to an event.  This record\\n     *  is intended to be abstract i.e., one should not instantiate an\\n     *  Object but rather instantiate one of its sub types (ie,\\n     *  encapsulating records) FileObject, UnnamedPipeObject,\\n     *  RegistryKeyObject, NetFlowObject, MemoryObject, or\\n     *  SrcSinkObject.\",\"fields\":[{\"name\":\"source\",\"type\":{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"doc\":\"* InstrumentationSource identifies the source reporting provenance information.\\n     *\\n     * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation\\n     * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation\\n     * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM\\n     * SOURCE_FREEBSD_DTRACE_CADETS,      from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_TESLA_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_LOOM_CADETS,        from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_MACIF_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit\\n     * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc\\n     * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation\\n     * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation source\\n     * SOURCE_WINDOWS_DIFT_FAROS,         from FAROS' DIFT module\\n     * SOURCE_WINDOWS_PSA_FAROS,          from FAROS' PSA module\\n     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections windows events\",\"symbols\":[\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_LINUX_AUDIT_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_WINDOWS_PSA_FAROS\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\"]},\"doc\":\"The source that emitted the object, see InstrumentationSource\"},{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"epoch\",\"type\":[\"null\",\"int\"],\"doc\":\"* Used to track when an object is deleted and a new one is\\n         * created with the same identifier. This is useful for when\\n         * UUIDs are based on something not likely to be unique, such\\n         * as file path.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null}]},\"doc\":\"The base object attributes\"},{\"name\":\"key\",\"type\":\"string\",\"doc\":\"The registry key/path\"},{\"name\":\"value\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"Value\",\"doc\":\"* Values represent transient data, mainly parameters to\\n     * events. Values are created and used once within an event's\\n     * execution and are relevant mainly during fine-grained tracking\\n     * (such as with tag/taint propagation).  Values have tags\\n     * describing their provenance. Sometimes the actual value's value\\n     * is reported in addition to the value's metadata\\n     *\\n     * The size of the value is the number of elements of type\\n     * valueDataType. This should be 0 for primitive and complex\\n     * types.  For arrays, the size is the array length. i.e., if\\n     * size>0, then this value is an array.  A complex value (such as\\n     * an object) can contain other values (primitives or other\\n     * complex values) within it, as components.\\n     *\\n     * isNull indicates whether a complex value is null. runtimeDataType indicates the runtime datatype. E.g., <br>\\n     *  e.g., an integer will have size=0 and valueDataType=INT, and valueBytes.length=4 bytes <br>\\n     *  e.g., an int[4] will have  size=4 and valueDataType=INT, and valueBytes.length=16 bytes (4*4) <br>\\n     *  e.g., a string s=\\\"abc\\\" has size=3 and valueDataType=CHAR, and valueBytes.length=6 bytes (treated as char[]) <br>\\n     *  e.g., an MyClass obj has size=0, valueDataType=COMPLEX, runtimeDataType=\\\"MyClass\\\", valueBytes=<pointer> <br>\",\"fields\":[{\"name\":\"size\",\"type\":\"int\",\"doc\":\"The size of the value: the number of elements of type valueDataType; 0 for non-arrays\",\"default\":0},{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"ValueType\",\"doc\":\"* A value type is either source, sink, or control This is for\\n     * Event parameters to distinguish source/sink values vs control\\n     * parameters (such as a file descriptor).\\n     *\\n     *   VALUE_TYPE_SOURCE   A source value to the event\\n     *   VALUE_TYPE_SINK     A sink value from the event\\n     *   VALUE_TYPE_CONTROL  A control value for the event\",\"symbols\":[\"VALUE_TYPE_SRC\",\"VALUE_TYPE_SINK\",\"VALUE_TYPE_CONTROL\"]},\"doc\":\"The type indicates whether it's a source, sink, or control value\"},{\"name\":\"valueDataType\",\"type\":{\"type\":\"enum\",\"name\":\"ValueDataType\",\"doc\":\"* A value data type is one of the primitive data types. A string is treated as a char array\",\"symbols\":[\"VALUE_DATA_TYPE_BYTE\",\"VALUE_DATA_TYPE_BOOL\",\"VALUE_DATA_TYPE_CHAR\",\"VALUE_DATA_TYPE_SHORT\",\"VALUE_DATA_TYPE_INT\",\"VALUE_DATA_TYPE_FLOAT\",\"VALUE_DATA_TYPE_LONG\",\"VALUE_DATA_TYPE_DOUBLE\",\"VALUE_DATA_TYPE_COMPLEX\"]},\"doc\":\"The actual datatype of the value elements, e.g., int, double, byte, etc. (Optional)\\n         *  Strings are treated as char[] so type=CHAR\\n         *  String[] is a COMPLEX value whose components are the string values (each modeled as a char[])\\n         *  Complex composite objects comprising of primitive values use the COMPLEX type\"},{\"name\":\"isNull\",\"type\":\"boolean\",\"doc\":\"Whether this value is null, needed to indicate null objects (default: false)\",\"default\":false},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"doc\":\"the name of the Value, string. (Optional)\",\"default\":null},{\"name\":\"runtimeDataType\",\"type\":[\"null\",\"string\"],\"doc\":\"The runtime data type of the value (Optional); For example, an object of dataType=COMPLEX, can have\\n         *  a runtime data type of say \\\"MyClass\\\"\",\"default\":null},{\"name\":\"valueBytes\",\"type\":[\"null\",\"bytes\"],\"doc\":\"The actual bytes of the value in Big Endian format, e.g., an int is converted to a 4 byte buffer (Optional)\",\"default\":null},{\"name\":\"tag\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"TagRunLengthTuple\",\"doc\":\"* This record is a single tuple in a run length encoding of tags\",\"fields\":[{\"name\":\"numValueElements\",\"type\":\"int\",\"default\":0},{\"name\":\"tagId\",\"type\":\"UUID\"}]}}],\"doc\":\"* The value's tag expression describing its provenance (Optional)\\n         * Since value could be an array, the tag can use run length encoding if needed.\",\"default\":null},{\"name\":\"components\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"Value\"}],\"doc\":\"A complex value might comprise other component values if needed (Optional)\",\"default\":null}]}],\"doc\":\"The value of the key\",\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"The entry size in bytes (Optional)\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.AbstractObject baseObject;
  /** The registry key/path */
  @Deprecated public java.lang.CharSequence key;
  /** The value of the key */
  @Deprecated public com.bbn.tc.schema.avro.Value value;
  /** The entry size in bytes (Optional) */
  @Deprecated public java.lang.Long size;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public RegistryKeyObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param key The registry key/path
   * @param value The value of the key
   * @param size The entry size in bytes (Optional)
   */
  public RegistryKeyObject(com.bbn.tc.schema.avro.UUID uuid, com.bbn.tc.schema.avro.AbstractObject baseObject, java.lang.CharSequence key, com.bbn.tc.schema.avro.Value value, java.lang.Long size) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.key = key;
    this.value = value;
    this.size = size;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return key;
    case 3: return value;
    case 4: return size;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.AbstractObject)value$; break;
    case 2: key = (java.lang.CharSequence)value$; break;
    case 3: value = (com.bbn.tc.schema.avro.Value)value$; break;
    case 4: size = (java.lang.Long)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return Universally unique identifier for the object
   */
  public com.bbn.tc.schema.avro.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * Universally unique identifier for the object
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'baseObject' field.
   * @return The base object attributes
   */
  public com.bbn.tc.schema.avro.AbstractObject getBaseObject() {
    return baseObject;
  }

  /**
   * Sets the value of the 'baseObject' field.
   * The base object attributes
   * @param value the value to set.
   */
  public void setBaseObject(com.bbn.tc.schema.avro.AbstractObject value) {
    this.baseObject = value;
  }

  /**
   * Gets the value of the 'key' field.
   * @return The registry key/path
   */
  public java.lang.CharSequence getKey() {
    return key;
  }

  /**
   * Sets the value of the 'key' field.
   * The registry key/path
   * @param value the value to set.
   */
  public void setKey(java.lang.CharSequence value) {
    this.key = value;
  }

  /**
   * Gets the value of the 'value' field.
   * @return The value of the key
   */
  public com.bbn.tc.schema.avro.Value getValue() {
    return value;
  }

  /**
   * Sets the value of the 'value' field.
   * The value of the key
   * @param value the value to set.
   */
  public void setValue(com.bbn.tc.schema.avro.Value value) {
    this.value = value;
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
   * Creates a new RegistryKeyObject RecordBuilder.
   * @return A new RegistryKeyObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.RegistryKeyObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.RegistryKeyObject.Builder();
  }

  /**
   * Creates a new RegistryKeyObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new RegistryKeyObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.RegistryKeyObject.Builder newBuilder(com.bbn.tc.schema.avro.RegistryKeyObject.Builder other) {
    return new com.bbn.tc.schema.avro.RegistryKeyObject.Builder(other);
  }

  /**
   * Creates a new RegistryKeyObject RecordBuilder by copying an existing RegistryKeyObject instance.
   * @param other The existing instance to copy.
   * @return A new RegistryKeyObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.RegistryKeyObject.Builder newBuilder(com.bbn.tc.schema.avro.RegistryKeyObject other) {
    return new com.bbn.tc.schema.avro.RegistryKeyObject.Builder(other);
  }

  /**
   * RecordBuilder for RegistryKeyObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<RegistryKeyObject>
    implements org.apache.avro.data.RecordBuilder<RegistryKeyObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.AbstractObject.Builder baseObjectBuilder;
    /** The registry key/path */
    private java.lang.CharSequence key;
    /** The value of the key */
    private com.bbn.tc.schema.avro.Value value;
    private com.bbn.tc.schema.avro.Value.Builder valueBuilder;
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
    private Builder(com.bbn.tc.schema.avro.RegistryKeyObject.Builder other) {
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
        this.baseObjectBuilder = com.bbn.tc.schema.avro.AbstractObject.newBuilder(other.getBaseObjectBuilder());
      }
      if (isValidValue(fields()[2], other.key)) {
        this.key = data().deepCopy(fields()[2].schema(), other.key);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.value)) {
        this.value = data().deepCopy(fields()[3].schema(), other.value);
        fieldSetFlags()[3] = true;
      }
      if (other.hasValueBuilder()) {
        this.valueBuilder = com.bbn.tc.schema.avro.Value.newBuilder(other.getValueBuilder());
      }
      if (isValidValue(fields()[4], other.size)) {
        this.size = data().deepCopy(fields()[4].schema(), other.size);
        fieldSetFlags()[4] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing RegistryKeyObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.RegistryKeyObject other) {
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
      if (isValidValue(fields()[2], other.key)) {
        this.key = data().deepCopy(fields()[2].schema(), other.key);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.value)) {
        this.value = data().deepCopy(fields()[3].schema(), other.value);
        fieldSetFlags()[3] = true;
      }
      this.valueBuilder = null;
      if (isValidValue(fields()[4], other.size)) {
        this.size = data().deepCopy(fields()[4].schema(), other.size);
        fieldSetFlags()[4] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setUuid(com.bbn.tc.schema.avro.UUID value) {
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
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'baseObject' field.
      * The base object attributes
      * @return The value.
      */
    public com.bbn.tc.schema.avro.AbstractObject getBaseObject() {
      return baseObject;
    }

    /**
      * Sets the value of the 'baseObject' field.
      * The base object attributes
      * @param value The value of 'baseObject'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setBaseObject(com.bbn.tc.schema.avro.AbstractObject value) {
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
    public com.bbn.tc.schema.avro.AbstractObject.Builder getBaseObjectBuilder() {
      if (baseObjectBuilder == null) {
        if (hasBaseObject()) {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.newBuilder(baseObject));
        } else {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.newBuilder());
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
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.Builder value) {
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
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'key' field.
      * The registry key/path
      * @return The value.
      */
    public java.lang.CharSequence getKey() {
      return key;
    }

    /**
      * Sets the value of the 'key' field.
      * The registry key/path
      * @param value The value of 'key'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setKey(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.key = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'key' field has been set.
      * The registry key/path
      * @return True if the 'key' field has been set, false otherwise.
      */
    public boolean hasKey() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'key' field.
      * The registry key/path
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder clearKey() {
      key = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'value' field.
      * The value of the key
      * @return The value.
      */
    public com.bbn.tc.schema.avro.Value getValue() {
      return value;
    }

    /**
      * Sets the value of the 'value' field.
      * The value of the key
      * @param value The value of 'value'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setValue(com.bbn.tc.schema.avro.Value value) {
      validate(fields()[3], value);
      this.valueBuilder = null;
      this.value = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'value' field has been set.
      * The value of the key
      * @return True if the 'value' field has been set, false otherwise.
      */
    public boolean hasValue() {
      return fieldSetFlags()[3];
    }

    /**
     * Gets the Builder instance for the 'value' field and creates one if it doesn't exist yet.
     * The value of the key
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.Value.Builder getValueBuilder() {
      if (valueBuilder == null) {
        if (hasValue()) {
          setValueBuilder(com.bbn.tc.schema.avro.Value.newBuilder(value));
        } else {
          setValueBuilder(com.bbn.tc.schema.avro.Value.newBuilder());
        }
      }
      return valueBuilder;
    }

    /**
     * Sets the Builder instance for the 'value' field
     * The value of the key
     * @param value The builder instance that must be set.
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setValueBuilder(com.bbn.tc.schema.avro.Value.Builder value) {
      clearValue();
      valueBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'value' field has an active Builder instance
     * The value of the key
     * @return True if the 'value' field has an active Builder instance
     */
    public boolean hasValueBuilder() {
      return valueBuilder != null;
    }

    /**
      * Clears the value of the 'value' field.
      * The value of the key
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder clearValue() {
      value = null;
      valueBuilder = null;
      fieldSetFlags()[3] = false;
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
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder setSize(java.lang.Long value) {
      validate(fields()[4], value);
      this.size = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'size' field has been set.
      * The entry size in bytes (Optional)
      * @return True if the 'size' field has been set, false otherwise.
      */
    public boolean hasSize() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'size' field.
      * The entry size in bytes (Optional)
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.RegistryKeyObject.Builder clearSize() {
      size = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    public RegistryKeyObject build() {
      try {
        RegistryKeyObject record = new RegistryKeyObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.AbstractObject) defaultValue(fields()[1]);
        }
        record.key = fieldSetFlags()[2] ? this.key : (java.lang.CharSequence) defaultValue(fields()[2]);
        if (valueBuilder != null) {
          record.value = this.valueBuilder.build();
        } else {
          record.value = fieldSetFlags()[3] ? this.value : (com.bbn.tc.schema.avro.Value) defaultValue(fields()[3]);
        }
        record.size = fieldSetFlags()[4] ? this.size : (java.lang.Long) defaultValue(fields()[4]);
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
