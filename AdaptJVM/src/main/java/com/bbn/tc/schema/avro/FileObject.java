/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents a file on the file system. Instantiates an AbstractObject. */
@org.apache.avro.specific.AvroGenerated
public class FileObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -4656027181200555629L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"FileObject\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* Represents a file on the file system. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which\\n     *  could include sockets, files, memory, and any data in general\\n     *  that can be an input and/or output to an event.  This record\\n     *  is intended to be abstract i.e., one should not instantiate an\\n     *  Object but rather instantiate one of its sub types (ie,\\n     *  encapsulating records) FileObject, UnnamedPipeObject,\\n     *  RegistryKeyObject, NetFlowObject, MemoryObject, or\\n     *  SrcSinkObject.\",\"fields\":[{\"name\":\"source\",\"type\":{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"doc\":\"* InstrumentationSource identifies the source reporting provenance information.\\n     *\\n     * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation\\n     * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation\\n     * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM\\n     * SOURCE_FREEBSD_DTRACE_CADETS,      from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_TESLA_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_LOOM_CADETS,        from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_MACIF_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit\\n     * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc\\n     * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation\\n     * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation source\\n     * SOURCE_WINDOWS_DIFT_FAROS,         from FAROS' DIFT module\\n     * SOURCE_WINDOWS_PSA_FAROS,          from FAROS' PSA module\\n     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections windows events\",\"symbols\":[\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_LINUX_AUDIT_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_WINDOWS_PSA_FAROS\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\"]},\"doc\":\"The source that emitted the object, see InstrumentationSource\"},{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"epoch\",\"type\":[\"null\",\"int\"],\"doc\":\"* Used to track when an object is deleted and a new one is\\n         * created with the same identifier. This is useful for when\\n         * UUIDs are based on something not likely to be unique, such\\n         * as file path.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null}]},\"doc\":\"The base object attributes\"},{\"name\":\"fileDescriptor\",\"type\":[\"null\",\"int\"],\"doc\":\"The file descriptor\",\"default\":null},{\"name\":\"localPrincipal\",\"type\":[\"null\",\"UUID\"],\"doc\":\"UUID of local principal that owns this file object.  This\\n         * attribute is optional because there are times when \\n         * the owner of the file may not be known at the time the file\\n         * object is reported (e.g., missed open call). Otherwise,\\n         * the local principal SHOULD be included.\",\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"* The file size in bytes (Optional). This attribute reports\\n         * the file size at the time the FileObject is created. Since records\\n         * are not updated, changes in file size is trackable via the events\\n         * that changed the file size.\",\"default\":null},{\"name\":\"peInfo\",\"type\":[\"null\",\"string\"],\"doc\":\"* portable execution (PE) info for windows (Optional).\\n         * Note from FiveDirections: We will LIKELY change this type for engagement 3\",\"default\":null},{\"name\":\"hashes\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"CryptographicHash\",\"doc\":\"* Cryptographic hash records represent one or more cryptographic hashes for\\n     * an object, typically, a FileObject.\",\"fields\":[{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"CryptoHashType\",\"doc\":\"Cryptographich hash types\",\"symbols\":[\"MD5\",\"SHA1\",\"SHA256\",\"SHA512\",\"AUTHENTIHASH\",\"SSDEEP\",\"IMPHASH\"]},\"doc\":\"The type of hash used\"},{\"name\":\"hash\",\"type\":\"string\",\"doc\":\"The base64 encoded hash value\"}]}}],\"doc\":\"(Optional) Zero or more cryptographic hashes over the FileObject\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.AbstractObject baseObject;
  /** The file descriptor */
  @Deprecated public java.lang.Integer fileDescriptor;
  /** UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included. */
  @Deprecated public com.bbn.tc.schema.avro.UUID localPrincipal;
  /** * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size. */
  @Deprecated public java.lang.Long size;
  /** * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3 */
  @Deprecated public java.lang.CharSequence peInfo;
  /** (Optional) Zero or more cryptographic hashes over the FileObject */
  @Deprecated public java.util.List<com.bbn.tc.schema.avro.CryptographicHash> hashes;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public FileObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param fileDescriptor The file descriptor
   * @param localPrincipal UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
   * @param size * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
   * @param peInfo * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
   * @param hashes (Optional) Zero or more cryptographic hashes over the FileObject
   */
  public FileObject(com.bbn.tc.schema.avro.UUID uuid, com.bbn.tc.schema.avro.AbstractObject baseObject, java.lang.Integer fileDescriptor, com.bbn.tc.schema.avro.UUID localPrincipal, java.lang.Long size, java.lang.CharSequence peInfo, java.util.List<com.bbn.tc.schema.avro.CryptographicHash> hashes) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.fileDescriptor = fileDescriptor;
    this.localPrincipal = localPrincipal;
    this.size = size;
    this.peInfo = peInfo;
    this.hashes = hashes;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return fileDescriptor;
    case 3: return localPrincipal;
    case 4: return size;
    case 5: return peInfo;
    case 6: return hashes;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.AbstractObject)value$; break;
    case 2: fileDescriptor = (java.lang.Integer)value$; break;
    case 3: localPrincipal = (com.bbn.tc.schema.avro.UUID)value$; break;
    case 4: size = (java.lang.Long)value$; break;
    case 5: peInfo = (java.lang.CharSequence)value$; break;
    case 6: hashes = (java.util.List<com.bbn.tc.schema.avro.CryptographicHash>)value$; break;
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
   * Gets the value of the 'fileDescriptor' field.
   * @return The file descriptor
   */
  public java.lang.Integer getFileDescriptor() {
    return fileDescriptor;
  }

  /**
   * Sets the value of the 'fileDescriptor' field.
   * The file descriptor
   * @param value the value to set.
   */
  public void setFileDescriptor(java.lang.Integer value) {
    this.fileDescriptor = value;
  }

  /**
   * Gets the value of the 'localPrincipal' field.
   * @return UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
   */
  public com.bbn.tc.schema.avro.UUID getLocalPrincipal() {
    return localPrincipal;
  }

  /**
   * Sets the value of the 'localPrincipal' field.
   * UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
   * @param value the value to set.
   */
  public void setLocalPrincipal(com.bbn.tc.schema.avro.UUID value) {
    this.localPrincipal = value;
  }

  /**
   * Gets the value of the 'size' field.
   * @return * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
   */
  public java.lang.Long getSize() {
    return size;
  }

  /**
   * Sets the value of the 'size' field.
   * * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
   * @param value the value to set.
   */
  public void setSize(java.lang.Long value) {
    this.size = value;
  }

  /**
   * Gets the value of the 'peInfo' field.
   * @return * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
   */
  public java.lang.CharSequence getPeInfo() {
    return peInfo;
  }

  /**
   * Sets the value of the 'peInfo' field.
   * * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
   * @param value the value to set.
   */
  public void setPeInfo(java.lang.CharSequence value) {
    this.peInfo = value;
  }

  /**
   * Gets the value of the 'hashes' field.
   * @return (Optional) Zero or more cryptographic hashes over the FileObject
   */
  public java.util.List<com.bbn.tc.schema.avro.CryptographicHash> getHashes() {
    return hashes;
  }

  /**
   * Sets the value of the 'hashes' field.
   * (Optional) Zero or more cryptographic hashes over the FileObject
   * @param value the value to set.
   */
  public void setHashes(java.util.List<com.bbn.tc.schema.avro.CryptographicHash> value) {
    this.hashes = value;
  }

  /**
   * Creates a new FileObject RecordBuilder.
   * @return A new FileObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.FileObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.FileObject.Builder();
  }

  /**
   * Creates a new FileObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new FileObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.FileObject.Builder newBuilder(com.bbn.tc.schema.avro.FileObject.Builder other) {
    return new com.bbn.tc.schema.avro.FileObject.Builder(other);
  }

  /**
   * Creates a new FileObject RecordBuilder by copying an existing FileObject instance.
   * @param other The existing instance to copy.
   * @return A new FileObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.FileObject.Builder newBuilder(com.bbn.tc.schema.avro.FileObject other) {
    return new com.bbn.tc.schema.avro.FileObject.Builder(other);
  }

  /**
   * RecordBuilder for FileObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<FileObject>
    implements org.apache.avro.data.RecordBuilder<FileObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.AbstractObject.Builder baseObjectBuilder;
    /** The file descriptor */
    private java.lang.Integer fileDescriptor;
    /** UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included. */
    private com.bbn.tc.schema.avro.UUID localPrincipal;
    /** * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size. */
    private java.lang.Long size;
    /** * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3 */
    private java.lang.CharSequence peInfo;
    /** (Optional) Zero or more cryptographic hashes over the FileObject */
    private java.util.List<com.bbn.tc.schema.avro.CryptographicHash> hashes;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.FileObject.Builder other) {
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
      if (isValidValue(fields()[2], other.fileDescriptor)) {
        this.fileDescriptor = data().deepCopy(fields()[2].schema(), other.fileDescriptor);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.localPrincipal)) {
        this.localPrincipal = data().deepCopy(fields()[3].schema(), other.localPrincipal);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.size)) {
        this.size = data().deepCopy(fields()[4].schema(), other.size);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.peInfo)) {
        this.peInfo = data().deepCopy(fields()[5].schema(), other.peInfo);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.hashes)) {
        this.hashes = data().deepCopy(fields()[6].schema(), other.hashes);
        fieldSetFlags()[6] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing FileObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.FileObject other) {
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
      if (isValidValue(fields()[2], other.fileDescriptor)) {
        this.fileDescriptor = data().deepCopy(fields()[2].schema(), other.fileDescriptor);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.localPrincipal)) {
        this.localPrincipal = data().deepCopy(fields()[3].schema(), other.localPrincipal);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.size)) {
        this.size = data().deepCopy(fields()[4].schema(), other.size);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.peInfo)) {
        this.peInfo = data().deepCopy(fields()[5].schema(), other.peInfo);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.hashes)) {
        this.hashes = data().deepCopy(fields()[6].schema(), other.hashes);
        fieldSetFlags()[6] = true;
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
    public com.bbn.tc.schema.avro.FileObject.Builder setUuid(com.bbn.tc.schema.avro.UUID value) {
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
    public com.bbn.tc.schema.avro.FileObject.Builder clearUuid() {
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
    public com.bbn.tc.schema.avro.FileObject.Builder setBaseObject(com.bbn.tc.schema.avro.AbstractObject value) {
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
    public com.bbn.tc.schema.avro.FileObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.Builder value) {
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
    public com.bbn.tc.schema.avro.FileObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'fileDescriptor' field.
      * The file descriptor
      * @return The value.
      */
    public java.lang.Integer getFileDescriptor() {
      return fileDescriptor;
    }

    /**
      * Sets the value of the 'fileDescriptor' field.
      * The file descriptor
      * @param value The value of 'fileDescriptor'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder setFileDescriptor(java.lang.Integer value) {
      validate(fields()[2], value);
      this.fileDescriptor = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'fileDescriptor' field has been set.
      * The file descriptor
      * @return True if the 'fileDescriptor' field has been set, false otherwise.
      */
    public boolean hasFileDescriptor() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'fileDescriptor' field.
      * The file descriptor
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder clearFileDescriptor() {
      fileDescriptor = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'localPrincipal' field.
      * UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
      * @return The value.
      */
    public com.bbn.tc.schema.avro.UUID getLocalPrincipal() {
      return localPrincipal;
    }

    /**
      * Sets the value of the 'localPrincipal' field.
      * UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
      * @param value The value of 'localPrincipal'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder setLocalPrincipal(com.bbn.tc.schema.avro.UUID value) {
      validate(fields()[3], value);
      this.localPrincipal = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'localPrincipal' field has been set.
      * UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
      * @return True if the 'localPrincipal' field has been set, false otherwise.
      */
    public boolean hasLocalPrincipal() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'localPrincipal' field.
      * UUID of local principal that owns this file object.  This
         * attribute is optional because there are times when 
         * the owner of the file may not be known at the time the file
         * object is reported (e.g., missed open call). Otherwise,
         * the local principal SHOULD be included.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder clearLocalPrincipal() {
      localPrincipal = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'size' field.
      * * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
      * @return The value.
      */
    public java.lang.Long getSize() {
      return size;
    }

    /**
      * Sets the value of the 'size' field.
      * * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
      * @param value The value of 'size'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder setSize(java.lang.Long value) {
      validate(fields()[4], value);
      this.size = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'size' field has been set.
      * * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
      * @return True if the 'size' field has been set, false otherwise.
      */
    public boolean hasSize() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'size' field.
      * * The file size in bytes (Optional). This attribute reports
         * the file size at the time the FileObject is created. Since records
         * are not updated, changes in file size is trackable via the events
         * that changed the file size.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder clearSize() {
      size = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'peInfo' field.
      * * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
      * @return The value.
      */
    public java.lang.CharSequence getPeInfo() {
      return peInfo;
    }

    /**
      * Sets the value of the 'peInfo' field.
      * * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
      * @param value The value of 'peInfo'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder setPeInfo(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.peInfo = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'peInfo' field has been set.
      * * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
      * @return True if the 'peInfo' field has been set, false otherwise.
      */
    public boolean hasPeInfo() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'peInfo' field.
      * * portable execution (PE) info for windows (Optional).
         * Note from FiveDirections: We will LIKELY change this type for engagement 3
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder clearPeInfo() {
      peInfo = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'hashes' field.
      * (Optional) Zero or more cryptographic hashes over the FileObject
      * @return The value.
      */
    public java.util.List<com.bbn.tc.schema.avro.CryptographicHash> getHashes() {
      return hashes;
    }

    /**
      * Sets the value of the 'hashes' field.
      * (Optional) Zero or more cryptographic hashes over the FileObject
      * @param value The value of 'hashes'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder setHashes(java.util.List<com.bbn.tc.schema.avro.CryptographicHash> value) {
      validate(fields()[6], value);
      this.hashes = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'hashes' field has been set.
      * (Optional) Zero or more cryptographic hashes over the FileObject
      * @return True if the 'hashes' field has been set, false otherwise.
      */
    public boolean hasHashes() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'hashes' field.
      * (Optional) Zero or more cryptographic hashes over the FileObject
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.FileObject.Builder clearHashes() {
      hashes = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    @Override
    public FileObject build() {
      try {
        FileObject record = new FileObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.AbstractObject) defaultValue(fields()[1]);
        }
        record.fileDescriptor = fieldSetFlags()[2] ? this.fileDescriptor : (java.lang.Integer) defaultValue(fields()[2]);
        record.localPrincipal = fieldSetFlags()[3] ? this.localPrincipal : (com.bbn.tc.schema.avro.UUID) defaultValue(fields()[3]);
        record.size = fieldSetFlags()[4] ? this.size : (java.lang.Long) defaultValue(fields()[4]);
        record.peInfo = fieldSetFlags()[5] ? this.peInfo : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.hashes = fieldSetFlags()[6] ? this.hashes : (java.util.List<com.bbn.tc.schema.avro.CryptographicHash>) defaultValue(fields()[6]);
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
