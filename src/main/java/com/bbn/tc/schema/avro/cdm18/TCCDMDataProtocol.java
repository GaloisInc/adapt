/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm18;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public interface TCCDMDataProtocol {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"TCCDMDataProtocol\",\"namespace\":\"com.bbn.tc.schema.avro.cdm18\",\"types\":[{\"type\":\"fixed\",\"name\":\"BYTE\",\"size\":1},{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2},{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},{\"type\":\"enum\",\"name\":\"HostType\",\"doc\":\"* HostType enumerates the host roles or device types\",\"symbols\":[\"HOST_MOBILE\",\"HOST_SERVER\",\"HOST_DESKTOP\"]},{\"type\":\"enum\",\"name\":\"SubjectType\",\"doc\":\"* SubjectType enumerates the types of execution contexts supported.\\n     *\\n     * SUBJECT_PROCESS,    process\\n     * SUBJECT_THREAD,     thread within a process\\n     * SUBJECT_UNIT        so far we only know of TRACE BEEP using this\",\"symbols\":[\"SUBJECT_PROCESS\",\"SUBJECT_THREAD\",\"SUBJECT_UNIT\",\"SUBJECT_BASIC_BLOCK\"]},{\"type\":\"enum\",\"name\":\"PrivilegeLevel\",\"doc\":\"* Windows allows Subjects (processes) to have the following\\n     * enumerated privilege levels.\",\"symbols\":[\"LIMITED\",\"ELEVATED\",\"FULL\"]},{\"type\":\"enum\",\"name\":\"SrcSinkType\",\"doc\":\"* There are many types of sources such as sensors.  The type of a\\n     * sensor could be base (close to hardware) or composite.  This is\\n     * mostly (only?) applicable to the Android platform.  See\\n     * https://source.android.com/devices/sensors/index.html for\\n     * details.\",\"symbols\":[\"SRCSINK_ACCELEROMETER\",\"SRCSINK_TEMPERATURE\",\"SRCSINK_GYROSCOPE\",\"SRCSINK_MAGNETIC_FIELD\",\"SRCSINK_HEART_RATE\",\"SRCSINK_LIGHT\",\"SRCSINK_PROXIMITY\",\"SRCSINK_PRESSURE\",\"SRCSINK_RELATIVE_HUMIDITY\",\"SRCSINK_LINEAR_ACCELERATION\",\"SRCSINK_MOTION\",\"SRCSINK_STEP_DETECTOR\",\"SRCSINK_STEP_COUNTER\",\"SRCSINK_TILT_DETECTOR\",\"SRCSINK_ROTATION_VECTOR\",\"SRCSINK_GRAVITY\",\"SRCSINK_GEOMAGNETIC_ROTATION_VECTOR\",\"SRCSINK_GPS\",\"SRCSINK_AUDIO\",\"SRCSINK_SYSTEM_PROPERTY\",\"SRCSINK_ENV_VARIABLE\",\"SRCSINK_ACCESSIBILITY_SERVICE\",\"SRCSINK_ACTIVITY_MANAGEMENT\",\"SRCSINK_ALARM_SERVICE\",\"SRCSINK_ANDROID_TV\",\"SRCSINK_AUDIO_IO\",\"SRCSINK_BACKUP_MANAGER\",\"SRCSINK_BINDER\",\"SRCSINK_BLUETOOTH\",\"SRCSINK_BOOT_EVENT\",\"SRCSINK_BROADCAST_RECEIVER_MANAGEMENT\",\"SRCSINK_CAMERA\",\"SRCSINK_CLIPBOARD\",\"SRCSINK_COMPONENT_MANAGEMENT\",\"SRCSINK_CONTENT_PROVIDER\",\"SRCSINK_CONTENT_PROVIDER_MANAGEMENT\",\"SRCSINK_DATABASE\",\"SRCSINK_DEVICE_ADMIN\",\"SRCSINK_DEVICE_SEARCH\",\"SRCSINK_DEVICE_USER\",\"SRCSINK_DISPLAY\",\"SRCSINK_DROPBOX\",\"SRCSINK_EMAIL\",\"SRCSINK_EXPERIMENTAL\",\"SRCSINK_FILE\",\"SRCSINK_FILE_SYSTEM\",\"SRCSINK_FILE_SYSTEM_MANAGEMENT\",\"SRCSINK_FINGERPRINT\",\"SRCSINK_FLASHLIGHT\",\"SRCSINK_GATEKEEPER\",\"SRCSINK_HDMI\",\"SRCSINK_IDLE_DOCK_SCREEN\",\"SRCSINK_IMS\",\"SRCSINK_INFRARED\",\"SRCSINK_INSTALLED_PACKAGES\",\"SRCSINK_JSSE_TRUST_MANAGER\",\"SRCSINK_KEYCHAIN\",\"SRCSINK_KEYGUARD\",\"SRCSINK_LOCATION\",\"SRCSINK_MACHINE_LEARNING\",\"SRCSINK_MEDIA\",\"SRCSINK_MEDIA_CAPTURE\",\"SRCSINK_MEDIA_LOCAL_MANAGEMENT\",\"SRCSINK_MEDIA_LOCAL_PLAYBACK\",\"SRCSINK_MEDIA_NETWORK_CONNECTION\",\"SRCSINK_MEDIA_REMOTE_PLAYBACK\",\"SRCSINK_MIDI\",\"SRCSINK_NATIVE\",\"SRCSINK_NETWORK\",\"SRCSINK_NETWORK_MANAGEMENT\",\"SRCSINK_NFC\",\"SRCSINK_NOTIFICATION\",\"SRCSINK_PAC_PROXY\",\"SRCSINK_PERMISSIONS\",\"SRCSINK_PERSISTANT_DATA\",\"SRCSINK_POSIX\",\"SRCSINK_POWER_MANAGEMENT\",\"SRCSINK_PRINT_SERVICE\",\"SRCSINK_PROCESS_MANAGEMENT\",\"SRCSINK_RECEIVER_MANAGEMENT\",\"SRCSINK_RPC\",\"SRCSINK_SCREEN_AUDIO_CAPTURE\",\"SRCSINK_SERIAL_PORT\",\"SRCSINK_SERVICE_CONNECTION\",\"SRCSINK_SERVICE_MANAGEMENT\",\"SRCSINK_SMS_MMS\",\"SRCSINK_SPEECH_INTERACTION\",\"SRCSINK_STATUS_BAR\",\"SRCSINK_SYNC_FRAMEWORK\",\"SRCSINK_TELEPHONY\",\"SRCSINK_TEST\",\"SRCSINK_TEXT_SERVICES\",\"SRCSINK_THREADING\",\"SRCSINK_TIME_EVENT\",\"SRCSINK_UI\",\"SRCSINK_UID_EVENT\",\"SRCSINK_UI_AUTOMATION\",\"SRCSINK_UI_MODE\",\"SRCSINK_UI_RPC\",\"SRCSINK_USAGE_STATS\",\"SRCSINK_USB\",\"SRCSINK_USER_ACCOUNTS_MANAGEMENT\",\"SRCSINK_USER_INPUT\",\"SRCSINK_VIBRATOR\",\"SRCSINK_WAKE_LOCK\",\"SRCSINK_WALLPAPER_MANAGER\",\"SRCSINK_WAP\",\"SRCSINK_WEB_BROWSER\",\"SRCSINK_WIDGETS\",\"SRCSINK_IPC\",\"SRCSINK_UNKNOWN\"]},{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"doc\":\"* InstrumentationSource identifies the source reporting provenance information.\",\"symbols\":[\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_LINUX_SYSCALL_TRACE\",\"SOURCE_LINUX_NETFILTER_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_WINDOWS_PSA_FAROS\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\",\"SOURCE_WINDOWS_MARPLE\"]},{\"type\":\"enum\",\"name\":\"PrincipalType\",\"doc\":\"* PrincipalType identifies the type of user: either local to the\\n     * host, or remote users/systems.\",\"symbols\":[\"PRINCIPAL_LOCAL\",\"PRINCIPAL_REMOTE\"]},{\"type\":\"enum\",\"name\":\"EventType\",\"doc\":\"* EventType enumerates the most common system calls. Since there\\n     * are hundreds of possible system calls, enumerating all of them\\n     * and managing the list across OS versions is a\\n     * challenge. EVENT_OTHER is the catch all for events not enumerated here. Any events\\n     * that are expected to be of importance, should be included in this list.\",\"symbols\":[\"EVENT_ACCEPT\",\"EVENT_ADD_OBJECT_ATTRIBUTE\",\"EVENT_BIND\",\"EVENT_BLIND\",\"EVENT_BOOT\",\"EVENT_CHANGE_PRINCIPAL\",\"EVENT_CHECK_FILE_ATTRIBUTES\",\"EVENT_CLONE\",\"EVENT_CLOSE\",\"EVENT_CONNECT\",\"EVENT_CREATE_OBJECT\",\"EVENT_CREATE_THREAD\",\"EVENT_DUP\",\"EVENT_EXECUTE\",\"EVENT_EXIT\",\"EVENT_FLOWS_TO\",\"EVENT_FCNTL\",\"EVENT_FORK\",\"EVENT_LINK\",\"EVENT_LOADLIBRARY\",\"EVENT_LOGCLEAR\",\"EVENT_LOGIN\",\"EVENT_LOGOUT\",\"EVENT_LSEEK\",\"EVENT_MMAP\",\"EVENT_MODIFY_FILE_ATTRIBUTES\",\"EVENT_MODIFY_PROCESS\",\"EVENT_MOUNT\",\"EVENT_MPROTECT\",\"EVENT_OPEN\",\"EVENT_OTHER\",\"EVENT_READ\",\"EVENT_READ_SOCKET_PARAMS\",\"EVENT_RECVFROM\",\"EVENT_RECVMSG\",\"EVENT_RENAME\",\"EVENT_SENDTO\",\"EVENT_SENDMSG\",\"EVENT_SERVICEINSTALL\",\"EVENT_SHM\",\"EVENT_SIGNAL\",\"EVENT_STARTSERVICE\",\"EVENT_TRUNCATE\",\"EVENT_UMOUNT\",\"EVENT_UNIT\",\"EVENT_UNLINK\",\"EVENT_UPDATE\",\"EVENT_WAIT\",\"EVENT_WRITE\",\"EVENT_WRITE_SOCKET_PARAMS\"]},{\"type\":\"enum\",\"name\":\"FileObjectType\",\"doc\":\"* These types enumerate the types of FileObjects\",\"symbols\":[\"FILE_OBJECT_BLOCK\",\"FILE_OBJECT_CHAR\",\"FILE_OBJECT_DIR\",\"FILE_OBJECT_FILE\",\"FILE_OBJECT_LINK\",\"FILE_OBJECT_NAMED_PIPE\",\"FILE_OBJECT_PEFILE\",\"FILE_OBJECT_UNIX_SOCKET\"]},{\"type\":\"enum\",\"name\":\"ValueType\",\"doc\":\"* A value type is either source, sink, or control This is for\\n     * Event parameters to distinguish source/sink values vs control\\n     * parameters (such as a file descriptor).\",\"symbols\":[\"VALUE_TYPE_SRC\",\"VALUE_TYPE_SINK\",\"VALUE_TYPE_CONTROL\"]},{\"type\":\"enum\",\"name\":\"ValueDataType\",\"doc\":\"* A value data type is one of the primitive data types. A string is treated as a char array\",\"symbols\":[\"VALUE_DATA_TYPE_BYTE\",\"VALUE_DATA_TYPE_BOOL\",\"VALUE_DATA_TYPE_CHAR\",\"VALUE_DATA_TYPE_SHORT\",\"VALUE_DATA_TYPE_INT\",\"VALUE_DATA_TYPE_FLOAT\",\"VALUE_DATA_TYPE_LONG\",\"VALUE_DATA_TYPE_DOUBLE\",\"VALUE_DATA_TYPE_POINTER32\",\"VALUE_DATA_TYPE_POINTER64\",\"VALUE_DATA_TYPE_COMPLEX\"]},{\"type\":\"enum\",\"name\":\"TagOpCode\",\"doc\":\"* The tag opcode describes the provenance relation i.e., how multiple sources are combined to\\n     * produce the output. We identify the following provenance relations\",\"symbols\":[\"TAG_OP_UNION\",\"TAG_OP_ENCODE\",\"TAG_OP_STRONG\",\"TAG_OP_MEDIUM\",\"TAG_OP_WEAK\"]},{\"type\":\"enum\",\"name\":\"IntegrityTag\",\"doc\":\"* The integrity tag may be used to specify the initial integrity of an entity,\\n     * or to endorse its content after performing appropriate checking/sanitization.\",\"symbols\":[\"INTEGRITY_UNTRUSTED\",\"INTEGRITY_BENIGN\",\"INTEGRITY_INVULNERABLE\"]},{\"type\":\"enum\",\"name\":\"ConfidentialityTag\",\"doc\":\"* The confidentiality tag may be used to specify the initial confidentiality of an entity,\\n     * or to declassify its content after performing appropriate checking/sanitization.\",\"symbols\":[\"CONFIDENTIALITY_SECRET\",\"CONFIDENTIALITY_SENSITIVE\",\"CONFIDENTIALITY_PRIVATE\",\"CONFIDENTIALITY_PUBLIC\"]},{\"type\":\"enum\",\"name\":\"CryptoHashType\",\"doc\":\"Cryptographich hash types\",\"symbols\":[\"MD5\",\"SHA1\",\"SHA256\",\"SHA512\",\"AUTHENTIHASH\",\"SSDEEP\",\"IMPHASH\"]},{\"type\":\"record\",\"name\":\"HostIdentifier\",\"doc\":\"Host identifier, such as serial number, IMEI number\",\"fields\":[{\"name\":\"idType\",\"type\":\"string\"},{\"name\":\"idValue\",\"type\":\"string\"}]},{\"type\":\"record\",\"name\":\"Interface\",\"doc\":\"Interface name and addresses\",\"fields\":[{\"name\":\"name\",\"type","\":\"string\"},{\"name\":\"macAddress\",\"type\":\"string\"},{\"name\":\"ipAddresses\",\"type\":{\"type\":\"array\",\"items\":\"string\"}}]},{\"type\":\"record\",\"name\":\"Host\",\"doc\":\"* Hosts represent a host/machine/node in a network.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"universally unique identifier for the host\"},{\"name\":\"hostName\",\"type\":\"string\",\"doc\":\"hostname or machine name\"},{\"name\":\"hostIdentifiers\",\"type\":{\"type\":\"array\",\"items\":\"HostIdentifier\"},\"doc\":\"list of identifiers, such as serial number, IMEI number\"},{\"name\":\"osDetails\",\"type\":\"string\",\"doc\":\"OS level details revealed by tools such as uname -a\"},{\"name\":\"hostType\",\"type\":\"HostType\",\"doc\":\"host's role or device type, such as mobile, server, desktop\"},{\"name\":\"interfaces\",\"type\":{\"type\":\"array\",\"items\":\"Interface\"},\"doc\":\"names and addresses of network interfaces\"}]},{\"type\":\"record\",\"name\":\"Principal\",\"doc\":\"* A principal is a local user\\n     * TODO: extend to include remote principals\\n     * TODO: what happens when the user information changes (are we tracking versions?)\\n     * TODO: Authentication mechanisms: are TA1s providing that information and how?\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"A unique id for the principal\"},{\"name\":\"type\",\"type\":\"PrincipalType\",\"doc\":\"The type of the principal, local by default\",\"default\":\"PRINCIPAL_LOCAL\"},{\"name\":\"hostId\",\"type\":\"UUID\",\"doc\":\"Host where principal exists\"},{\"name\":\"userId\",\"type\":\"string\",\"doc\":\"The operating system identifier associated with the user\"},{\"name\":\"username\",\"type\":[\"null\",\"string\"],\"doc\":\"Human-readable string identifier, such as username (Optional)\",\"default\":null},{\"name\":\"groupIds\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"doc\":\"The ids of the groups which this user is part of\"},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},{\"type\":\"record\",\"name\":\"ProvenanceTagNode\",\"doc\":\"* A provenance tag defines source dependence on specific data sources (inputs).\\n     * A tag identifier is typically bound to a source and used by the tracking system to\\n     * capture dependence on this source input.\\n     *\\n     * ProvenanceTagNode defines one step of provenance for a value\\n     * (i.e., one read from a source or write to a sink), a reference\\n     * to the previous provenance of the value (if any), and the tag\\n     * operation that resulted the tagId of this ProvenanceTagNode\",\"fields\":[{\"name\":\"tagId\",\"type\":\"UUID\",\"doc\":\"Tag ID for this node *\"},{\"name\":\"flowObject\",\"type\":[\"null\",\"UUID\"],\"doc\":\"* The UUID of the source or sink object associated with this\\n         * tag. (Optional)\\n         *\\n         * This attribute is optional because if the\\n         * ProvenanceTagNode is simply joining two existing\\n         * provenances (e.g., when two values are added together), there\\n         * is no flow object associated with that definition.\\n         *\",\"default\":null},{\"name\":\"hostId\",\"type\":\"UUID\",\"doc\":\"Host on which the src/sink action is occuring\"},{\"name\":\"subject\",\"type\":\"UUID\",\"doc\":\"Subject that is performing the src/sink action *\"},{\"name\":\"systemCall\",\"type\":[\"null\",\"string\"],\"doc\":\"System call that read/wrote the data *\",\"default\":null},{\"name\":\"programPoint\",\"type\":[\"null\",\"string\"],\"doc\":\"The program point where the event was triggered (e.g., executable and line number), (Optional)\",\"default\":null},{\"name\":\"prevTagId\",\"type\":[\"null\",\"UUID\"],\"doc\":\"The previous tag for this value *\",\"default\":null},{\"name\":\"opcode\",\"type\":[\"null\",\"TagOpCode\"],\"doc\":\"Tag operation that resulted in the tagId of this ProvenanceTagNode *\",\"default\":null},{\"name\":\"tagIds\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"UUID\"}],\"default\":null},{\"name\":\"itag\",\"type\":[\"null\",\"IntegrityTag\"],\"doc\":\"The integrity tag may be used to specify the intial\\n         *  integrity of an entity, or to endorse it content after\\n         *  performing appropriate checking/sanitization.\",\"default\":null},{\"name\":\"ctag\",\"type\":[\"null\",\"ConfidentialityTag\"],\"doc\":\"* The confidentiality tag may be used to specify the initial\\n         * confidentiality of an entity, or to declassify its content\\n         * after performing appropriate checking/sanitization.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},{\"type\":\"record\",\"name\":\"TagRunLengthTuple\",\"doc\":\"* This record is a single tuple in a run length encoding of tags\",\"fields\":[{\"name\":\"numValueElements\",\"type\":\"int\",\"default\":0},{\"name\":\"tagId\",\"type\":\"UUID\"}]},{\"type\":\"record\",\"name\":\"ProvenanceAssertion\",\"doc\":\"* An assertion about the provenance of information\",\"fields\":[{\"name\":\"asserter\",\"type\":\"UUID\",\"doc\":\"Which Subject is making this assertion?\"},{\"name\":\"sources\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"UUID\"}],\"doc\":\"Object(s) that this Value's data came from.\",\"default\":null},{\"name\":\"provenance\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"ProvenanceAssertion\"}],\"doc\":\"* Further provenance assertions within this assertion.\\n\\t * For example, to describe a situation in which X asserts that\\n\\t * Y asserts that Z asserts that V came from {p,q}:\\n\\t *\\n\\t * ```\\n\\t * Event {\\n\\t *   subject = X,\\n\\t *   parameters = [\\n\\t *     Value (V) {\\n\\t *       provenance = [\\n\\t *         ProvenanceAssertion {\\n\\t *           asserter = UUID of X,\\n\\t *           sources = [ UUID of p, UUID of q ],\\n\\t *           provenance = [\\n\\t *             ProvenanceAssertion {\\n\\t *               asserter = UUID of Y,\\n\\t *               provenance = [\\n\\t *                 ProvenanceAssertion {\\n\\t *                   asserter = UUID of Z,\\n\\t *                 },\\n\\t *               ],\\n\\t *             },\\n\\t *           ],\\n\\t *         },\\n\\t *       ],\\n\\t *     },\\n\\t *   ],\\n\\t * }\\n\\t * ```\\n\\t * Z should have a provenance assertion\\n\\t * e.g.,\\n         * \\\"X asserts that Y asserts that Z comes from {p,q}\\\".\",\"default\":null}]},{\"type\":\"record\",\"name\":\"Value\",\"doc\":\"* Values represent transient data, mainly parameters to\\n     * events. Values are created and used once within an event's\\n     * execution and are relevant mainly during fine-grained tracking\\n     * (such as with tag/taint propagation).  Values have tags\\n     * describing their provenance. Sometimes the actual value's value\\n     * is reported in addition to the value's metadata\\n     *\\n     * The size of the value is the number of elements of type\\n     * valueDataType. This should be -1 for primitive and complex\\n     * types.  For arrays, the size is the array length. i.e., if\\n     * size >= 0, then this value is an array.  A complex value (such as\\n     * an object) can contain other values (primitives or other\\n     * complex values) within it, as components.\\n     *\\n     * Examples: <br>\\n     *\\n     * an integer will have size=-1 and valueDataType=INT, and\\n     * valueBytes.length=4 bytes <br>\\n     *\\n     * an int[4] will have size=4 and valueDataType=INT, and\\n     * valueBytes.length=16 bytes (4*4) <br>\\n     *\\n     * a string s=\\\"abc\\\" has size=3 and valueDataType=CHAR, and\\n     * valueBytes.length=12 bytes (UTF32_BE encoding; 4 bytes per\\n     * char) <br>\\n     *\\n     * an MyClass obj has size=-1, valueDataType=COMPLEX,\\n     * runtimeDataType=\\\"MyClass\\\", valueBytes=<pointer> <br>\",\"fields\":[{\"name\":\"size\",\"type\":\"int\",\"doc\":\"The size of the value: the number of elements of type valueDataType; -1 for non-arrays\",\"default\":-1},{\"name\":\"type\",\"type\":\"ValueType\",\"doc\":\"The type indicates whether it's a source, sink, or control value\"},{\"name\":\"valueDataType\",\"type\":\"ValueDataType\",\"doc\":\"The actual datatype of the value elements, e.g., int, double, byte, etc. (Optional)\\n         *  Strings are treated as char[] so type=CHAR\\n         *  String[] is a COMPLEX"," value whose components are the string values (each modeled as a char[])\\n         *  Complex composite objects comprising of primitive values use the COMPLEX type\"},{\"name\":\"isNull\",\"type\":\"boolean\",\"doc\":\"Whether this value is null, needed to indicate null objects (default: false)\",\"default\":false},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"doc\":\"the name of the Value, string. (Optional)\",\"default\":null},{\"name\":\"runtimeDataType\",\"type\":[\"null\",\"string\"],\"doc\":\"The runtime data type of the value (Optional); For example, an object of dataType=COMPLEX, can have\\n         *  a runtime data type of say \\\"MyClass\\\"\",\"default\":null},{\"name\":\"valueBytes\",\"type\":[\"null\",\"bytes\"],\"doc\":\"* The actual bytes of the value in Big Endian format, e.g.,\\n         * an int is converted to a 4 byte buffer (Optional)\\n         *\\n         * Strings are represented as an array of UTF32_BE encoded\\n         * characters (i.e., 4 bytes per char)\",\"default\":null},{\"name\":\"provenance\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"ProvenanceAssertion\"}],\"doc\":\"* Assertions about the provenance of this value\\n         * (e.g., the file that data is claimed to come from).\\n         *\\n         * This is a direct assertion about provenance for systems that don't\\n         * use tags to track data flows.\",\"default\":null},{\"name\":\"tag\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"TagRunLengthTuple\"}],\"doc\":\"* The value's tag expression describing its provenance (Optional)\\n         * Since value could be an array, the tag can use run length encoding if needed.\",\"default\":null},{\"name\":\"components\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"Value\"}],\"doc\":\"A complex value might comprise other component values if needed (Optional)\",\"default\":null}]},{\"type\":\"record\",\"name\":\"CryptographicHash\",\"doc\":\"* Cryptographic hash records represent one or more cryptographic hashes for\\n     * an object, typically, a FileObject.\",\"fields\":[{\"name\":\"type\",\"type\":\"CryptoHashType\",\"doc\":\"The type of hash used\"},{\"name\":\"hash\",\"type\":\"string\",\"doc\":\"The base64 encoded hash value\"}]},{\"type\":\"record\",\"name\":\"Subject\",\"doc\":\"* Subjects represent execution contexts and include mainly threads and processes. They can be more granular and\\n     * can represent other execution boundaries such as units and blocks if needed.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"universally unique identifier for the subject\"},{\"name\":\"type\",\"type\":\"SubjectType\",\"doc\":\"the subject type\"},{\"name\":\"cid\",\"type\":\"int\",\"doc\":\"Context ID: OS process id for type process, thread id for threads\"},{\"name\":\"parentSubject\",\"type\":[\"null\",\"UUID\"],\"doc\":\"* parent subject's UUID. For a process, this is a parent\\n         * process. For a thread, this is the process that created the\\n         * thread. Only optional because in some cases the parent may not\\n         * be known; null value indicates that the parent is unknown.\",\"default\":null},{\"name\":\"hostId\",\"type\":\"UUID\",\"doc\":\"Host where subject is executing\"},{\"name\":\"localPrincipal\",\"type\":\"UUID\",\"doc\":\"UUID of local principal that owns this subject\"},{\"name\":\"startTimestampNanos\",\"type\":\"long\",\"doc\":\"* The start time of the subject\\n         * A timestamp stores the number of nanoseconds from the unix epoch, 1 January 1970 00:00:00.000000 UTC.\"},{\"name\":\"unitId\",\"type\":[\"null\",\"int\"],\"doc\":\"unit id for unit based instrumentation (Optional)\",\"default\":null},{\"name\":\"iteration\",\"type\":[\"null\",\"int\"],\"doc\":\"iteration and count are used for distinguishing individual \\\"units\\\" of execution (Optional)\",\"default\":null},{\"name\":\"count\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"cmdLine\",\"type\":[\"null\",\"string\"],\"doc\":\"Process command line arguments including process name (Optional)\",\"default\":null},{\"name\":\"privilegeLevel\",\"type\":[\"null\",\"PrivilegeLevel\"],\"doc\":\"Windows allows processes to have different privilege levels (Optional)\",\"default\":null},{\"name\":\"importedLibraries\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"string\"}],\"doc\":\"* imported libraries. (Optional). Lists the libraries that\\n         * are expected to be loaded, but may not necessarily\\n         * correspond 1-to-1 with actual load library events because\\n         * some libraries may already be loaded when this event\\n         * occurs.\",\"default\":null},{\"name\":\"exportedLibraries\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"string\"}],\"doc\":\"exported libraries. (Optional)\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which\\n     *  could include sockets, files, memory, and any data in general\\n     *  that can be an input and/or output to an event.  This record\\n     *  is intended to be abstract i.e., one should not instantiate an\\n     *  Object but rather instantiate one of its sub types (ie,\\n     *  encapsulating records) FileObject, UnnamedPipeObject,\\n     *  RegistryKeyObject, NetFlowObject, MemoryObject, or\\n     *  SrcSinkObject.\",\"fields\":[{\"name\":\"hostId\",\"type\":\"UUID\",\"doc\":\"Host where object exists\"},{\"name\":\"permission\",\"type\":[\"null\",\"SHORT\"],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"epoch\",\"type\":[\"null\",\"int\"],\"doc\":\"* Used to track when an object is deleted and a new one is\\n         * created with the same identifier. This is useful for when\\n         * UUIDs are based on something not likely to be unique, such\\n         * as file path.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},{\"type\":\"record\",\"name\":\"FileObject\",\"doc\":\"* Represents a file on the file system. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"type\",\"type\":\"FileObjectType\",\"doc\":\"The type of FileObject\"},{\"name\":\"fileDescriptor\",\"type\":[\"null\",\"int\"],\"doc\":\"The file descriptor (Optional)\",\"default\":null},{\"name\":\"localPrincipal\",\"type\":[\"null\",\"UUID\"],\"doc\":\"UUID of local principal that owns this file object.  This\\n         * attribute is optional because there are times when \\n         * the owner of the file may not be known at the time the file\\n         * object is reported (e.g., missed open call). Otherwise,\\n         * the local principal SHOULD be included.\",\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"* The file size in bytes (Optional). This attribute reports\\n         * the file size at the time the FileObject is created. Since records\\n         * are not updated, changes in file size is trackable via the events\\n         * that changed the file size.\",\"default\":null},{\"name\":\"peInfo\",\"type\":[\"null\",\"string\"],\"doc\":\"* portable execution (PE) info for windows (Optional).\\n         * Note from FiveDirections: We will LIKELY change this type for engagement 3\",\"default\":null},{\"name\":\"hashes\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"CryptographicHash\"}],\"doc\":\"(Optional) Zero or more cryptographic hashes over the FileObject\",\"default\":null}]},{\"type\":\"record\",\"name\":\"UnnamedPipeObject\",\"doc\":\"* Represents an unnamed pipe. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"sourceFileDescriptor\",\"type\":[\"null\",\"int\"],\"doc\":\"* Although file descriptor and UUID src/sink pairs are\\n         * individually optional, at least one pair MUST be used.\",\"default\":null},{\"name\":\"sinkFileDescriptor\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"sourceUUID\",\"type\":[\"null\",\"UU","ID\"],\"default\":null},{\"name\":\"sinkUUID\",\"type\":[\"null\",\"UUID\"],\"default\":null}]},{\"type\":\"record\",\"name\":\"RegistryKeyObject\",\"doc\":\"* Represents a registry key. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"key\",\"type\":\"string\",\"doc\":\"The registry key/path\"},{\"name\":\"value\",\"type\":[\"null\",\"Value\"],\"doc\":\"The value of the key\",\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"The entry size in bytes (Optional)\",\"default\":null}]},{\"type\":\"record\",\"name\":\"PacketSocketObject\",\"doc\":\"* Represents a packet socket. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"proto\",\"type\":\"SHORT\",\"doc\":\"Physical-layer protocol\"},{\"name\":\"ifIndex\",\"type\":\"int\",\"doc\":\"Interface number\"},{\"name\":\"haType\",\"type\":\"SHORT\",\"doc\":\"ARP hardware type\"},{\"name\":\"pktType\",\"type\":\"BYTE\",\"doc\":\"Packet type\"},{\"name\":\"addr\",\"type\":\"bytes\",\"doc\":\"Physical-layer address\"}]},{\"type\":\"record\",\"name\":\"NetFlowObject\",\"doc\":\"* Represents a network flow object. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"localAddress\",\"type\":\"string\",\"doc\":\"The local IP address for this flow\"},{\"name\":\"localPort\",\"type\":\"int\",\"doc\":\"The local network port for this flow\"},{\"name\":\"remoteAddress\",\"type\":\"string\",\"doc\":\"The remote IP address for this flow\"},{\"name\":\"remotePort\",\"type\":\"int\",\"doc\":\"The remote network port for this flow\"},{\"name\":\"ipProtocol\",\"type\":[\"null\",\"int\"],\"doc\":\"The IP protocol number e.g., TCP=6\",\"default\":null},{\"name\":\"fileDescriptor\",\"type\":[\"null\",\"int\"],\"doc\":\"The file descriptor (Optional)\",\"default\":null}]},{\"type\":\"record\",\"name\":\"MemoryObject\",\"doc\":\"* Represents a page in memory. Instantiates an AbstractObject.\\n     * TODO: is memory really an object (with permissions and so on) or is it a transient data?\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"memoryAddress\",\"type\":\"long\",\"doc\":\"The memory address\"},{\"name\":\"pageNumber\",\"type\":[\"null\",\"long\"],\"doc\":\"(Optional) decomposed memory addressed into pageNumber and pageOffset\",\"default\":null},{\"name\":\"pageOffset\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"The entry size in bytes (Optional)\",\"default\":null}]},{\"type\":\"record\",\"name\":\"SrcSinkObject\",\"doc\":\"* Represents a generic source or sink on the host device that is can be a file, memory, or netflow.\\n     * This is the most basic representation of a source or sink, basically specifying its type only.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":\"AbstractObject\",\"doc\":\"The base object attributes\"},{\"name\":\"type\",\"type\":\"SrcSinkType\",\"doc\":\"The type of the object\"},{\"name\":\"fileDescriptor\",\"type\":[\"null\",\"int\"],\"doc\":\"The file descriptor (Optional)\",\"default\":null}]},{\"type\":\"record\",\"name\":\"Event\",\"doc\":\"* Events represent actions executed by subjects on data objects\\n     * or other subjects.  Events are generally system calls, but\\n     * could also include function calls, instruction executions, or\\n     * even more abstract notions. Events are the core entity in the\\n     * data model and they are the main abstraction for representing\\n     * information flow between data objects and subjects.\",\"fields\":[{\"name\":\"uuid\",\"type\":\"UUID\",\"doc\":\"A universally unique identifier for the event\"},{\"name\":\"sequence\",\"type\":[\"null\",\"long\"],\"doc\":\"* A logical sequence number for ordering events relative to\\n         * each other within a subject's execution context\\n         *\\n         * This attribute is only optional for inferred events, such\\n         * as an object's attribute change that was observed without\\n         * an explicit event or system call.\",\"default\":null},{\"name\":\"type\",\"type\":\"EventType\",\"doc\":\"The type of the event\"},{\"name\":\"threadId\",\"type\":[\"null\",\"int\"],\"doc\":\"* The thread id to which this event belongs.  Required for\\n         * all events, except the EVENT_ADD_OBJECT_ATTRIBUTE and\\n         * EVENT_FLOWS_TO event.\",\"default\":null},{\"name\":\"hostId\",\"type\":\"UUID\",\"doc\":\"Host where event occurred\"},{\"name\":\"subject\",\"type\":[\"null\",\"UUID\"],\"doc\":\"* UUID of Subject that generated this event.  The subject is\\n         * required for all events, except the\\n         * EVENT_ADD_OBJECT_ATTRIBUTE and EVENT_FLOWS_TO event.\",\"default\":null},{\"name\":\"predicateObject\",\"type\":[\"null\",\"UUID\"],\"doc\":\"* UUID of Object/Subject this event acts on. For events that\\n         * have two arguments, this attribute contains the first\\n         * argument (following the argument order in the underlying\\n         * system call). This attribute is optional because it may not\\n         * be relevant for some events.\",\"default\":null},{\"name\":\"predicateObjectPath\",\"type\":[\"null\",\"string\"],\"doc\":\"If applicable, the object's absolute file path (Optional)\",\"default\":null},{\"name\":\"predicateObject2\",\"type\":[\"null\",\"UUID\"],\"doc\":\"* Optional UUID of Object/Subject for events that take two\\n         * arguments (e.g., link, rename, etc). This attribute\\n         * contains the second argument (following the argument order\\n         * in the underlying system call).\",\"default\":null},{\"name\":\"predicateObject2Path\",\"type\":[\"null\",\"string\"],\"doc\":\"If applicable, the second object's absolute file path (Optional)\",\"default\":null},{\"name\":\"timestampNanos\",\"type\":\"long\",\"doc\":\"* The time at which the event occurred. Timestamps allow\\n         * reasoning about order of events on a host when the same\\n         * clock is used. A timestamp stores the number of nanoseconds\\n         * from the unix epoch, 1 January 1970 00:00:00.000000 UTC.\\n         *\\n         * TODO: When different clocks are used on a host or across\\n         * hosts, we need to also define a clock source\\n         *\\n         * NOTE: When an object update is inferred without an explicit\\n         * system call event, TA1s may not have an accurate timestamp\\n         * of when the object was updated. In that case, the timestamp\\n         * used in the update event is the same as the timestamp used\\n         * in the next system call event that uses the updated object.\"},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"doc\":\"Event name (Optional)\",\"default\":null},{\"name\":\"parameters\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"Value\"}],\"doc\":\"Event parameters represented as values, see Value (Optional)\",\"default\":null},{\"name\":\"location\",\"type\":[\"null\",\"long\"],\"doc\":\"Location refers to the location of the data affecting the event\\n         *  (e.g., the read offset in the file for the read system call event). (Optional)\",\"default\":null},{\"name\":\"size\",\"type\":[\"null\",\"long\"],\"doc\":\"Size refers to the size of the data affecting the event\\n         *  (e.g., the number of bytes read from the file for the read system call event). (Optional)\",\"default\":null},{\"name\":\"programPoint\",\"type\":[\"null\",\"string\"],\"doc\":\"The program point where the event was triggered (e.g., executable and line number). (Optional)\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null,\"order\":\"ignore\"}]},{\"type\":\"record\",\"name\":\"UnitDependency\",\"doc\":\"* This record captures a relationship edge between two units, one\\n     * dependent on the other. This relationship is inferred from a\\n     * combination of underlying events.\",\"fields\":[{\"name\":\"unit\",\"type\":\"UUID\"},{\"name\":\"dependentUnit\",\"type\":\"UUID\"}]},{\"type\":\"record\",\"name\":\"TimeMarker\",","\"doc\":\"* TimeMarker records are used to delineate time periods in a data\\n     * stream to help consumers know their current read position in the\\n     * data stream.\",\"fields\":[{\"name\":\"tsNanos\",\"type\":\"long\",\"doc\":\"Timestamp in nanoseconds\"}]},{\"type\":\"record\",\"name\":\"StartMarker\",\"doc\":\"* StartMarker records delineate system (re)starts in a data stream.\",\"fields\":[{\"name\":\"sessionNumber\",\"type\":\"int\",\"doc\":\"sequence number that monotonically increases each time the system is started\"}]},{\"type\":\"record\",\"name\":\"EndMarker\",\"doc\":\"* EndMarker records marks the end of a data stream.\",\"fields\":[{\"name\":\"sessionNumber\",\"type\":\"int\",\"doc\":\"session number in the corresponding StartMarker\"},{\"name\":\"recordCounts\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"doc\":\"* Reports countc of each record type that has been published\\n         * since the the start of the data stream.\",\"order\":\"ignore\"}]},{\"type\":\"enum\",\"name\":\"TheiaQueryType\",\"doc\":\"* TheiaQueryType used to indicate the type of TheiaQuery\",\"symbols\":[\"BACKWARD\",\"FORWARD\",\"POINT_TO_POINT\"]},{\"type\":\"record\",\"name\":\"TheiaQuery\",\"doc\":\"* TheiaQuery record used to request a fine-grained analysis to THEIA system\",\"fields\":[{\"name\":\"queryId\",\"type\":\"UUID\"},{\"name\":\"type\",\"type\":\"TheiaQueryType\"},{\"name\":\"sourceId\",\"type\":[\"null\",\"UUID\"],\"default\":null},{\"name\":\"sinkId\",\"type\":[\"null\",\"UUID\"],\"default\":null},{\"name\":\"startTimestamp\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"endTimestamp\",\"type\":[\"null\",\"long\"],\"default\":null}]},{\"type\":\"record\",\"name\":\"TheiaQueryResult\",\"doc\":\"* TheiaQueryResult used to indicate that the fine-grained analysis created\\n     * by a query has finished\",\"fields\":[{\"name\":\"queryId\",\"type\":\"UUID\"},{\"name\":\"tagIds\",\"type\":{\"type\":\"array\",\"items\":\"UUID\"}}]},{\"type\":\"record\",\"name\":\"TCCDMDatum\",\"doc\":\"* A record representing either a vertex or an edge. This is the top level record that allows\\n     * us to easily mix the vertices and edges defined above on the wire.\",\"fields\":[{\"name\":\"datum\",\"type\":[\"Host\",\"Principal\",\"ProvenanceTagNode\",\"Subject\",\"FileObject\",\"UnnamedPipeObject\",\"RegistryKeyObject\",\"PacketSocketObject\",\"NetFlowObject\",\"MemoryObject\",\"SrcSinkObject\",\"Event\",\"UnitDependency\",\"TimeMarker\",\"StartMarker\",\"EndMarker\"]},{\"name\":\"CDMVersion\",\"type\":\"string\",\"default\":\"18\"},{\"name\":\"source\",\"type\":\"InstrumentationSource\",\"doc\":\"What source generated this record?\"}]}],\"messages\":{}}");

  @SuppressWarnings("all")
  public interface Callback extends TCCDMDataProtocol {
    public static final org.apache.avro.Protocol PROTOCOL = com.bbn.tc.schema.avro.cdm18.TCCDMDataProtocol.PROTOCOL;
  }
}