/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm13;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents a generic source or sink on the host device that is can be a file, memory, or netflow.
     * This is the most basic representation of a source or sink, basically specifying its type only. */
@org.apache.avro.specific.AvroGenerated
public class SrcSinkObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -3133365843277581653L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SrcSinkObject\",\"namespace\":\"com.bbn.tc.schema.avro.cdm13\",\"doc\":\"* Represents a generic source or sink on the host device that is can be a file, memory, or netflow.\\n     * This is the most basic representation of a source or sink, basically specifying its type only.\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which could include sockets, files,\\n     *  memory, and any data in general that can be an input and/or output to an event.\\n     *  This record is intended to be abstract i.e., one should not instantiate an Object\\n     *  but rather instantiate one of its sub types File, NetFlow, of Memory\",\"fields\":[{\"name\":\"source\",\"type\":{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"doc\":\"* SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit\\n * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc\\n     * * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation\\n     * * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM\\n     * * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation\\n     * * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation\\n * * SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS  for CADETS * freebsd instrumentation\\n     * SOURCE_FREEBSD_LOOM_CADETS, * SOURCE_FREEBSD_MACIF_CADETS    for CADETS freebsd instrumentation\\n     * * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation * source\\n     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections * windows events\",\"symbols\":[\"SOURCE_LINUX_AUDIT_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\"]},\"doc\":\"The source that emitted the object, see InstrumentationSource\"},{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"lastTimestampMicros\",\"type\":[\"null\",\"long\"],\"doc\":\"* The timestamp when the object was last modified (Optional).\\n        * A timestamp stores the number of microseconds from the unix epoch, 1 January 1970 00:00:00.000000 UTC.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"Arbitrary key, value pairs describing the entity\",\"default\":null}]},\"doc\":\"The base object attributes\"},{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"SrcSinkType\",\"doc\":\"* There are many types of sources such as sensors\\n     * The type of a sensor could be base (close to hardware) or composite\\n     * See https://source.android.com/devices/sensors/index.html for details\\n     * TODO: camera and GPS ideally should be modeled separately to match the Android model. These are rich subsystems\",\"symbols\":[\"SOURCE_ACCELEROMETER\",\"SOURCE_TEMPERATURE\",\"SOURCE_GYROSCOPE\",\"SOURCE_MAGNETIC_FIELD\",\"SOURCE_HEART_RATE\",\"SOURCE_LIGHT\",\"SOURCE_PROXIMITY\",\"SOURCE_PRESSURE\",\"SOURCE_RELATIVE_HUMIDITY\",\"SOURCE_LINEAR_ACCELERATION\",\"SOURCE_MOTION\",\"SOURCE_STEP_DETECTOR\",\"SOURCE_STEP_COUNTER\",\"SOURCE_TILT_DETECTOR\",\"SOURCE_ROTATION_VECTOR\",\"SOURCE_GRAVITY\",\"SOURCE_GEOMAGNETIC_ROTATION_VECTOR\",\"SOURCE_CAMERA\",\"SOURCE_GPS\",\"SOURCE_AUDIO\",\"SOURCE_SYSTEM_PROPERTY\",\"SOURCE_ENV_VARIABLE\",\"SOURCE_SINK_IPC\",\"SOURCE_UNKNOWN\"]},\"doc\":\"The type of the object\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.cdm13.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject;
  /** The type of the object */
  @Deprecated public com.bbn.tc.schema.avro.cdm13.SrcSinkType type;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public SrcSinkObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param type The type of the object
   */
  public SrcSinkObject(com.bbn.tc.schema.avro.cdm13.UUID uuid, com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject, com.bbn.tc.schema.avro.cdm13.SrcSinkType type) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.type = type;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return type;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.cdm13.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.cdm13.AbstractObject)value$; break;
    case 2: type = (com.bbn.tc.schema.avro.cdm13.SrcSinkType)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return Universally unique identifier for the object
   */
  public com.bbn.tc.schema.avro.cdm13.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * Universally unique identifier for the object
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.cdm13.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'baseObject' field.
   * @return The base object attributes
   */
  public com.bbn.tc.schema.avro.cdm13.AbstractObject getBaseObject() {
    return baseObject;
  }

  /**
   * Sets the value of the 'baseObject' field.
   * The base object attributes
   * @param value the value to set.
   */
  public void setBaseObject(com.bbn.tc.schema.avro.cdm13.AbstractObject value) {
    this.baseObject = value;
  }

  /**
   * Gets the value of the 'type' field.
   * @return The type of the object
   */
  public com.bbn.tc.schema.avro.cdm13.SrcSinkType getType() {
    return type;
  }

  /**
   * Sets the value of the 'type' field.
   * The type of the object
   * @param value the value to set.
   */
  public void setType(com.bbn.tc.schema.avro.cdm13.SrcSinkType value) {
    this.type = value;
  }

  /**
   * Creates a new SrcSinkObject RecordBuilder.
   * @return A new SrcSinkObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder();
  }

  /**
   * Creates a new SrcSinkObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new SrcSinkObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder other) {
    return new com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder(other);
  }

  /**
   * Creates a new SrcSinkObject RecordBuilder by copying an existing SrcSinkObject instance.
   * @param other The existing instance to copy.
   * @return A new SrcSinkObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm13.SrcSinkObject other) {
    return new com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder(other);
  }

  /**
   * RecordBuilder for SrcSinkObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<SrcSinkObject>
    implements org.apache.avro.data.RecordBuilder<SrcSinkObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.cdm13.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder baseObjectBuilder;
    /** The type of the object */
    private com.bbn.tc.schema.avro.cdm13.SrcSinkType type;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder other) {
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
        this.baseObjectBuilder = com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder(other.getBaseObjectBuilder());
      }
      if (isValidValue(fields()[2], other.type)) {
        this.type = data().deepCopy(fields()[2].schema(), other.type);
        fieldSetFlags()[2] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing SrcSinkObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm13.SrcSinkObject other) {
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
      if (isValidValue(fields()[2], other.type)) {
        this.type = data().deepCopy(fields()[2].schema(), other.type);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm13.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder setUuid(com.bbn.tc.schema.avro.cdm13.UUID value) {
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
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'baseObject' field.
      * The base object attributes
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm13.AbstractObject getBaseObject() {
      return baseObject;
    }

    /**
      * Sets the value of the 'baseObject' field.
      * The base object attributes
      * @param value The value of 'baseObject'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder setBaseObject(com.bbn.tc.schema.avro.cdm13.AbstractObject value) {
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
    public com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder getBaseObjectBuilder() {
      if (baseObjectBuilder == null) {
        if (hasBaseObject()) {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder(baseObject));
        } else {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder());
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
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder value) {
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
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'type' field.
      * The type of the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm13.SrcSinkType getType() {
      return type;
    }

    /**
      * Sets the value of the 'type' field.
      * The type of the object
      * @param value The value of 'type'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder setType(com.bbn.tc.schema.avro.cdm13.SrcSinkType value) {
      validate(fields()[2], value);
      this.type = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'type' field has been set.
      * The type of the object
      * @return True if the 'type' field has been set, false otherwise.
      */
    public boolean hasType() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'type' field.
      * The type of the object
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.SrcSinkObject.Builder clearType() {
      type = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public SrcSinkObject build() {
      try {
        SrcSinkObject record = new SrcSinkObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.cdm13.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.cdm13.AbstractObject) defaultValue(fields()[1]);
        }
        record.type = fieldSetFlags()[2] ? this.type : (com.bbn.tc.schema.avro.cdm13.SrcSinkType) defaultValue(fields()[2]);
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
