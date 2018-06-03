package com.galois.adapt.cdm18

trait CustomEnum[T] extends CDM18 {
  val values: Seq[T]
  def from(s: String): Option[T] = values.find(_.toString == s)  // TODO: strings will be slow. Use ordinals.
}

// New
case class FixedByte(val bytes: Array[Byte]) extends AnyVal

// No change
case class FixedShort(val bytes: Array[Byte]) extends AnyVal

// No changes
sealed trait SubjectType extends CDM18
case object SubjectType extends CustomEnum[SubjectType] { val values = Seq(SUBJECT_PROCESS, SUBJECT_THREAD, SUBJECT_UNIT, SUBJECT_BASIC_BLOCK) }
case object SUBJECT_PROCESS extends SubjectType
case object SUBJECT_THREAD extends SubjectType
case object SUBJECT_UNIT extends SubjectType
case object SUBJECT_BASIC_BLOCK extends SubjectType

// No changes
sealed trait PrivilegeLevel extends CDM18
case object PrivilegeLevel extends CustomEnum[PrivilegeLevel] { val values = Seq(LIMITED, ELEVATED, FULL) }
case object LIMITED extends PrivilegeLevel
case object ELEVATED extends PrivilegeLevel
case object FULL extends PrivilegeLevel

// No changes
sealed trait SrcSinkType extends CDM18
case object SrcSinkType extends CustomEnum[SrcSinkType] { val values = Seq(SRCSINK_ACCELEROMETER, SRCSINK_TEMPERATURE, SRCSINK_GYROSCOPE, SRCSINK_MAGNETIC_FIELD, SRCSINK_HEART_RATE, SRCSINK_LIGHT, SRCSINK_PROXIMITY, SRCSINK_PRESSURE, SRCSINK_RELATIVE_HUMIDITY, SRCSINK_LINEAR_ACCELERATION, SRCSINK_MOTION, SRCSINK_STEP_DETECTOR, SRCSINK_STEP_COUNTER, SRCSINK_TILT_DETECTOR, SRCSINK_ROTATION_VECTOR, SRCSINK_GRAVITY, SRCSINK_GEOMAGNETIC_ROTATION_VECTOR, SRCSINK_GPS, SRCSINK_AUDIO, SRCSINK_SYSTEM_PROPERTY, SRCSINK_ENV_VARIABLE, SRCSINK_ACCESSIBILITY_SERVICE, SRCSINK_ACTIVITY_MANAGEMENT, SRCSINK_ALARM_SERVICE, SRCSINK_ANDROID_TV, SRCSINK_AUDIO_IO, SRCSINK_BACKUP_MANAGER, SRCSINK_BINDER, SRCSINK_BLUETOOTH, SRCSINK_BOOT_EVENT, SRCSINK_BROADCAST_RECEIVER_MANAGEMENT, SRCSINK_CAMERA, SRCSINK_CLIPBOARD, SRCSINK_COMPONENT_MANAGEMENT, SRCSINK_CONTENT_PROVIDER, SRCSINK_CONTENT_PROVIDER_MANAGEMENT, SRCSINK_DATABASE, SRCSINK_DEVICE_ADMIN, SRCSINK_DEVICE_SEARCH, SRCSINK_DEVICE_USER, SRCSINK_DISPLAY, SRCSINK_DROPBOX, SRCSINK_EMAIL, SRCSINK_EXPERIMENTAL, SRCSINK_FILE, SRCSINK_FILE_SYSTEM, SRCSINK_FILE_SYSTEM_MANAGEMENT, SRCSINK_FINGERPRINT, SRCSINK_FLASHLIGHT, SRCSINK_GATEKEEPER, SRCSINK_HDMI, SRCSINK_IDLE_DOCK_SCREEN, SRCSINK_IMS, SRCSINK_INFRARED, SRCSINK_INSTALLED_PACKAGES, SRCSINK_JSSE_TRUST_MANAGER, SRCSINK_KEYCHAIN, SRCSINK_KEYGUARD, SRCSINK_LOCATION, SRCSINK_MACHINE_LEARNING, SRCSINK_MEDIA, SRCSINK_MEDIA_CAPTURE, SRCSINK_MEDIA_LOCAL_MANAGEMENT, SRCSINK_MEDIA_LOCAL_PLAYBACK, SRCSINK_MEDIA_NETWORK_CONNECTION, SRCSINK_MEDIA_REMOTE_PLAYBACK, SRCSINK_MIDI, SRCSINK_NATIVE, SRCSINK_NETWORK, SRCSINK_NETWORK_MANAGEMENT, SRCSINK_NFC, SRCSINK_NOTIFICATION, SRCSINK_PAC_PROXY, SRCSINK_PERMISSIONS, SRCSINK_PERSISTANT_DATA, SRCSINK_POSIX, SRCSINK_POWER_MANAGEMENT, SRCSINK_PRINT_SERVICE, SRCSINK_PROCESS_MANAGEMENT, SRCSINK_RECEIVER_MANAGEMENT, SRCSINK_RPC, SRCSINK_SCREEN_AUDIO_CAPTURE, SRCSINK_SERIAL_PORT, SRCSINK_SERVICE_CONNECTION, SRCSINK_SERVICE_MANAGEMENT, SRCSINK_SMS_MMS, SRCSINK_SPEECH_INTERACTION, SRCSINK_STATUS_BAR, SRCSINK_SYNC_FRAMEWORK, SRCSINK_TELEPHONY, SRCSINK_TEST, SRCSINK_TEXT_SERVICES, SRCSINK_THREADING, SRCSINK_TIME_EVENT, SRCSINK_UI, SRCSINK_UID_EVENT, SRCSINK_UI_AUTOMATION, SRCSINK_UI_MODE, SRCSINK_UI_RPC, SRCSINK_USAGE_STATS, SRCSINK_USB, SRCSINK_USER_ACCOUNTS_MANAGEMENT, SRCSINK_USER_INPUT, SRCSINK_VIBRATOR, SRCSINK_WAKE_LOCK, SRCSINK_WALLPAPER_MANAGER, SRCSINK_WAP, SRCSINK_WEB_BROWSER, SRCSINK_WIDGETS, SRCSINK_IPC, SRCSINK_UNKNOWN, MEMORY_SRCSINK) }
case object SRCSINK_ACCELEROMETER extends SrcSinkType
case object SRCSINK_TEMPERATURE extends SrcSinkType
case object SRCSINK_GYROSCOPE extends SrcSinkType
case object SRCSINK_MAGNETIC_FIELD extends SrcSinkType
case object SRCSINK_HEART_RATE extends SrcSinkType
case object SRCSINK_LIGHT extends SrcSinkType
case object SRCSINK_PROXIMITY extends SrcSinkType
case object SRCSINK_PRESSURE extends SrcSinkType
case object SRCSINK_RELATIVE_HUMIDITY extends SrcSinkType
case object SRCSINK_LINEAR_ACCELERATION extends SrcSinkType
case object SRCSINK_MOTION extends SrcSinkType
case object SRCSINK_STEP_DETECTOR extends SrcSinkType
case object SRCSINK_STEP_COUNTER extends SrcSinkType
case object SRCSINK_TILT_DETECTOR extends SrcSinkType
case object SRCSINK_ROTATION_VECTOR extends SrcSinkType
case object SRCSINK_GRAVITY extends SrcSinkType
case object SRCSINK_GEOMAGNETIC_ROTATION_VECTOR extends SrcSinkType
case object SRCSINK_GPS extends SrcSinkType
case object SRCSINK_AUDIO extends SrcSinkType
case object SRCSINK_SYSTEM_PROPERTY extends SrcSinkType
case object SRCSINK_ENV_VARIABLE extends SrcSinkType
case object SRCSINK_ACCESSIBILITY_SERVICE extends SrcSinkType
case object SRCSINK_ACTIVITY_MANAGEMENT extends SrcSinkType
case object SRCSINK_ALARM_SERVICE extends SrcSinkType
case object SRCSINK_ANDROID_TV extends SrcSinkType
case object SRCSINK_AUDIO_IO extends SrcSinkType
case object SRCSINK_BACKUP_MANAGER extends SrcSinkType
case object SRCSINK_BINDER extends SrcSinkType
case object SRCSINK_BLUETOOTH extends SrcSinkType
case object SRCSINK_BOOT_EVENT extends SrcSinkType
case object SRCSINK_BROADCAST_RECEIVER_MANAGEMENT extends SrcSinkType
case object SRCSINK_CAMERA extends SrcSinkType
case object SRCSINK_CLIPBOARD extends SrcSinkType
case object SRCSINK_COMPONENT_MANAGEMENT extends SrcSinkType
case object SRCSINK_CONTENT_PROVIDER extends SrcSinkType
case object SRCSINK_CONTENT_PROVIDER_MANAGEMENT extends SrcSinkType
case object SRCSINK_DATABASE extends SrcSinkType
case object SRCSINK_DEVICE_ADMIN extends SrcSinkType
case object SRCSINK_DEVICE_SEARCH extends SrcSinkType
case object SRCSINK_DEVICE_USER extends SrcSinkType
case object SRCSINK_DISPLAY extends SrcSinkType
case object SRCSINK_DROPBOX extends SrcSinkType
case object SRCSINK_EMAIL extends SrcSinkType
case object SRCSINK_EXPERIMENTAL extends SrcSinkType
case object SRCSINK_FILE extends SrcSinkType
case object SRCSINK_FILE_SYSTEM extends SrcSinkType
case object SRCSINK_FILE_SYSTEM_MANAGEMENT extends SrcSinkType
case object SRCSINK_FINGERPRINT extends SrcSinkType
case object SRCSINK_FLASHLIGHT extends SrcSinkType
case object SRCSINK_GATEKEEPER extends SrcSinkType
case object SRCSINK_HDMI extends SrcSinkType
case object SRCSINK_IDLE_DOCK_SCREEN extends SrcSinkType
case object SRCSINK_IMS extends SrcSinkType
case object SRCSINK_INFRARED extends SrcSinkType
case object SRCSINK_INSTALLED_PACKAGES extends SrcSinkType
case object SRCSINK_JSSE_TRUST_MANAGER extends SrcSinkType
case object SRCSINK_KEYCHAIN extends SrcSinkType
case object SRCSINK_KEYGUARD extends SrcSinkType
case object SRCSINK_LOCATION extends SrcSinkType
case object SRCSINK_MACHINE_LEARNING extends SrcSinkType
case object SRCSINK_MEDIA extends SrcSinkType
case object SRCSINK_MEDIA_CAPTURE extends SrcSinkType
case object SRCSINK_MEDIA_LOCAL_MANAGEMENT extends SrcSinkType
case object SRCSINK_MEDIA_LOCAL_PLAYBACK extends SrcSinkType
case object SRCSINK_MEDIA_NETWORK_CONNECTION extends SrcSinkType
case object SRCSINK_MEDIA_REMOTE_PLAYBACK extends SrcSinkType
case object SRCSINK_MIDI extends SrcSinkType
case object SRCSINK_NATIVE extends SrcSinkType
case object SRCSINK_NETWORK extends SrcSinkType
case object SRCSINK_NETWORK_MANAGEMENT extends SrcSinkType
case object SRCSINK_NFC extends SrcSinkType
case object SRCSINK_NOTIFICATION extends SrcSinkType
case object SRCSINK_PAC_PROXY extends SrcSinkType
case object SRCSINK_PERMISSIONS extends SrcSinkType
case object SRCSINK_PERSISTANT_DATA extends SrcSinkType
case object SRCSINK_POSIX extends SrcSinkType
case object SRCSINK_POWER_MANAGEMENT extends SrcSinkType
case object SRCSINK_PRINT_SERVICE extends SrcSinkType
case object SRCSINK_PROCESS_MANAGEMENT extends SrcSinkType
case object SRCSINK_RECEIVER_MANAGEMENT extends SrcSinkType
case object SRCSINK_RPC extends SrcSinkType
case object SRCSINK_SCREEN_AUDIO_CAPTURE extends SrcSinkType
case object SRCSINK_SERIAL_PORT extends SrcSinkType
case object SRCSINK_SERVICE_CONNECTION extends SrcSinkType
case object SRCSINK_SERVICE_MANAGEMENT extends SrcSinkType
case object SRCSINK_SMS_MMS extends SrcSinkType
case object SRCSINK_SPEECH_INTERACTION extends SrcSinkType
case object SRCSINK_STATUS_BAR extends SrcSinkType
case object SRCSINK_SYNC_FRAMEWORK extends SrcSinkType
case object SRCSINK_TELEPHONY extends SrcSinkType
case object SRCSINK_TEST extends SrcSinkType
case object SRCSINK_TEXT_SERVICES extends SrcSinkType
case object SRCSINK_THREADING extends SrcSinkType
case object SRCSINK_TIME_EVENT extends SrcSinkType
case object SRCSINK_UI extends SrcSinkType
case object SRCSINK_UID_EVENT extends SrcSinkType
case object SRCSINK_UI_AUTOMATION extends SrcSinkType
case object SRCSINK_UI_MODE extends SrcSinkType
case object SRCSINK_UI_RPC extends SrcSinkType
case object SRCSINK_USAGE_STATS extends SrcSinkType
case object SRCSINK_USB extends SrcSinkType
case object SRCSINK_USER_ACCOUNTS_MANAGEMENT extends SrcSinkType
case object SRCSINK_USER_INPUT extends SrcSinkType
case object SRCSINK_VIBRATOR extends SrcSinkType
case object SRCSINK_WAKE_LOCK extends SrcSinkType
case object SRCSINK_WALLPAPER_MANAGER extends SrcSinkType
case object SRCSINK_WAP extends SrcSinkType
case object SRCSINK_WEB_BROWSER extends SrcSinkType
case object SRCSINK_WIDGETS extends SrcSinkType
case object SRCSINK_IPC extends SrcSinkType
case object SRCSINK_UNKNOWN extends SrcSinkType
case object MEMORY_SRCSINK extends SrcSinkType // ADDED BY US FOR ADM!

// Updated
sealed trait InstrumentationSource extends CDM18
case object InstrumentationSource extends CustomEnum[InstrumentationSource] { val values = Seq(SOURCE_ANDROID_JAVA_CLEARSCOPE, SOURCE_ANDROID_NATIVE_CLEARSCOPE, SOURCE_FREEBSD_OPENBSM_TRACE, SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS, SOURCE_FREEBSD_LOOM_CADETS, SOURCE_FREEBSD_MACIF_CADETS, SOURCE_LINUX_SYSCALL_TRACE, SOURCE_LINUX_NETFILTER_TRACE, SOURCE_LINUX_PROC_TRACE, SOURCE_LINUX_BEEP_TRACE, SOURCE_LINUX_THEIA, SOURCE_WINDOWS_DIFT_FAROS, SOURCE_WINDOWS_PSA_FAROS, SOURCE_WINDOWS_FIVEDIRECTIONS, SOURCE_WINDOWS_MARPLE) }
case object SOURCE_ANDROID_JAVA_CLEARSCOPE extends InstrumentationSource
case object SOURCE_ANDROID_NATIVE_CLEARSCOPE extends InstrumentationSource
case object SOURCE_FREEBSD_OPENBSM_TRACE extends InstrumentationSource
case object SOURCE_FREEBSD_DTRACE_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_TESLA_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_LOOM_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_MACIF_CADETS extends InstrumentationSource
case object SOURCE_LINUX_SYSCALL_TRACE extends InstrumentationSource
case object SOURCE_LINUX_NETFILTER_TRACE extends InstrumentationSource
case object SOURCE_LINUX_PROC_TRACE extends InstrumentationSource
case object SOURCE_LINUX_BEEP_TRACE extends InstrumentationSource
case object SOURCE_LINUX_THEIA extends InstrumentationSource
case object SOURCE_WINDOWS_DIFT_FAROS extends InstrumentationSource
case object SOURCE_WINDOWS_PSA_FAROS extends InstrumentationSource
case object SOURCE_WINDOWS_FIVEDIRECTIONS extends InstrumentationSource
case object SOURCE_WINDOWS_MARPLE extends InstrumentationSource

// No changes
sealed trait PrincipalType extends CDM18
case object PrincipalType extends CustomEnum[PrincipalType] { val values = Seq(PRINCIPAL_LOCAL, PRINCIPAL_REMOTE) }
case object PRINCIPAL_LOCAL extends PrincipalType
case object PRINCIPAL_REMOTE extends PrincipalType

// Updated
sealed trait EventType extends CDM18
case object EventType extends CustomEnum[EventType] { val values = Seq(EVENT_ACCEPT, EVENT_ADD_OBJECT_ATTRIBUTE, EVENT_BIND, EVENT_BLIND, EVENT_BOOT, EVENT_CHANGE_PRINCIPAL, EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLONE, EVENT_CLOSE, EVENT_CONNECT, EVENT_CREATE_OBJECT, EVENT_CREATE_THREAD, EVENT_DUP, EVENT_EXECUTE, EVENT_EXIT, EVENT_FLOWS_TO, EVENT_FCNTL, EVENT_FORK, EVENT_LINK, EVENT_LOADLIBRARY, EVENT_LOGCLEAR, EVENT_LOGIN, EVENT_LOGOUT, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_MODIFY_PROCESS, EVENT_MOUNT, EVENT_MPROTECT, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_READ_SOCKET_PARAMS, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_RENAME, EVENT_SENDTO, EVENT_SENDMSG, EVENT_SERVICEINSTALL, EVENT_SHM, EVENT_SIGNAL, EVENT_STARTSERVICE, EVENT_TRUNCATE, EVENT_UMOUNT, EVENT_UNIT, EVENT_UNLINK, EVENT_UPDATE, EVENT_WAIT, EVENT_WRITE, EVENT_WRITE_SOCKET_PARAMS, PSEUDO_EVENT_PARENT_SUBJECT) }
case object EVENT_ACCEPT extends EventType
case object EVENT_ADD_OBJECT_ATTRIBUTE extends EventType
case object EVENT_BIND extends EventType
case object EVENT_BLIND extends EventType
case object EVENT_BOOT extends EventType
case object EVENT_CHANGE_PRINCIPAL extends EventType
case object EVENT_CHECK_FILE_ATTRIBUTES extends EventType
case object EVENT_CLONE extends EventType
case object EVENT_CLOSE extends EventType
case object EVENT_CONNECT extends EventType
case object EVENT_CREATE_OBJECT extends EventType
case object EVENT_CREATE_THREAD extends EventType
case object EVENT_DUP extends EventType
case object EVENT_EXECUTE extends EventType
case object EVENT_EXIT extends EventType
case object EVENT_FLOWS_TO extends EventType
case object EVENT_FCNTL extends EventType
case object EVENT_FORK extends EventType
case object EVENT_LINK extends EventType
case object EVENT_LOADLIBRARY extends EventType
case object EVENT_LOGCLEAR extends EventType
case object EVENT_LOGIN extends EventType
case object EVENT_LOGOUT extends EventType
case object EVENT_LSEEK extends EventType
case object EVENT_MMAP extends EventType
case object EVENT_MODIFY_FILE_ATTRIBUTES extends EventType
case object EVENT_MODIFY_PROCESS extends EventType
case object EVENT_MOUNT extends EventType
case object EVENT_MPROTECT extends EventType
case object EVENT_OPEN extends EventType
case object EVENT_OTHER extends EventType
case object EVENT_READ extends EventType
case object EVENT_READ_SOCKET_PARAMS extends EventType
case object EVENT_RECVFROM extends EventType
case object EVENT_RECVMSG extends EventType
case object EVENT_RENAME extends EventType
case object EVENT_SENDTO extends EventType
case object EVENT_SENDMSG extends EventType
case object EVENT_SERVICEINSTALL extends EventType
case object EVENT_SHM extends EventType
case object EVENT_SIGNAL extends EventType
case object EVENT_STARTSERVICE extends EventType
case object EVENT_TRUNCATE extends EventType
case object EVENT_UMOUNT extends EventType
case object EVENT_UNIT extends EventType
case object EVENT_UNLINK extends EventType
case object EVENT_UPDATE extends EventType
case object EVENT_WAIT extends EventType
case object EVENT_WRITE extends EventType
case object EVENT_WRITE_SOCKET_PARAMS extends EventType
case object PSEUDO_EVENT_PARENT_SUBJECT extends EventType   // Added by us to pluck out parent/child processes

// Updated
sealed trait FileObjectType extends CDM18
case object FileObjectType extends CustomEnum[FileObjectType] { val values = Seq(FILE_OBJECT_BLOCK, FILE_OBJECT_CHAR, FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_LINK, FILE_OBJECT_NAMED_PIPE, FILE_OBJECT_PEFILE, FILE_OBJECT_UNIX_SOCKET) }
case object FILE_OBJECT_BLOCK extends FileObjectType
case object FILE_OBJECT_CHAR extends FileObjectType
case object FILE_OBJECT_DIR extends FileObjectType
case object FILE_OBJECT_FILE extends FileObjectType
case object FILE_OBJECT_LINK extends FileObjectType
case object FILE_OBJECT_NAMED_PIPE extends FileObjectType
case object FILE_OBJECT_PEFILE extends FileObjectType
case object FILE_OBJECT_UNIX_SOCKET extends FileObjectType

// No change
sealed trait ValueType extends CDM18
case object ValueType extends CustomEnum[ValueType] { val values = Seq(VALUE_TYPE_SRC, VALUE_TYPE_SINK, VALUE_TYPE_CONTROL) }
case object VALUE_TYPE_SRC extends ValueType
case object VALUE_TYPE_SINK extends ValueType
case object VALUE_TYPE_CONTROL extends ValueType

// Updated
sealed trait ValueDataType extends CDM18
case object ValueDataType extends CustomEnum[ValueDataType] { val values = Seq(VALUE_DATA_TYPE_BYTE, VALUE_DATA_TYPE_BOOL, VALUE_DATA_TYPE_CHAR, VALUE_DATA_TYPE_SHORT, VALUE_DATA_TYPE_INT, VALUE_DATA_TYPE_FLOAT, VALUE_DATA_TYPE_LONG, VALUE_DATA_TYPE_DOUBLE, VALUE_DATA_TYPE_POINTER32, VALUE_DATA_TYPE_POINTER64, VALUE_DATA_TYPE_COMPLEX) }
case object VALUE_DATA_TYPE_BYTE extends ValueDataType
case object VALUE_DATA_TYPE_BOOL extends ValueDataType
case object VALUE_DATA_TYPE_CHAR extends ValueDataType
case object VALUE_DATA_TYPE_SHORT extends ValueDataType
case object VALUE_DATA_TYPE_INT extends ValueDataType
case object VALUE_DATA_TYPE_FLOAT extends ValueDataType
case object VALUE_DATA_TYPE_LONG extends ValueDataType
case object VALUE_DATA_TYPE_DOUBLE extends ValueDataType
case object VALUE_DATA_TYPE_POINTER32 extends ValueDataType
case object VALUE_DATA_TYPE_POINTER64 extends ValueDataType
case object VALUE_DATA_TYPE_COMPLEX extends ValueDataType

// No change
sealed trait TagOpCode extends CDM18
case object TagOpCode extends CustomEnum[TagOpCode] { val values = Seq(TAG_OP_UNION, TAG_OP_ENCODE, TAG_OP_STRONG, TAG_OP_MEDIUM, TAG_OP_WEAK) }
case object TAG_OP_UNION extends TagOpCode
case object TAG_OP_ENCODE extends TagOpCode
case object TAG_OP_STRONG extends TagOpCode
case object TAG_OP_MEDIUM extends TagOpCode
case object TAG_OP_WEAK extends TagOpCode

// No change
sealed trait IntegrityTag extends CDM18
case object IntegrityTag extends CustomEnum[IntegrityTag] { val values = Seq(INTEGRITY_UNTRUSTED, INTEGRITY_BENIGN, INTEGRITY_INVULNERABLE) }
case object INTEGRITY_UNTRUSTED extends IntegrityTag
case object INTEGRITY_BENIGN extends IntegrityTag
case object INTEGRITY_INVULNERABLE extends IntegrityTag

// No change
sealed trait ConfidentialityTag extends CDM18
case object ConfidentialityTag extends CustomEnum[ConfidentialityTag] { val values = Seq(CONFIDENTIALITY_SECRET, CONFIDENTIALITY_SENSITIVE, CONFIDENTIALITY_PRIVATE, CONFIDENTIALITY_PUBLIC) }
case object CONFIDENTIALITY_SECRET extends ConfidentialityTag
case object CONFIDENTIALITY_SENSITIVE extends ConfidentialityTag
case object CONFIDENTIALITY_PRIVATE extends ConfidentialityTag
case object CONFIDENTIALITY_PUBLIC extends ConfidentialityTag

// No change
sealed trait CryptoHashType extends CDM18
case object CryptoHashType extends CustomEnum[CryptoHashType] { val values = Seq(MD5, SHA1, SHA256, SHA512, AUTHENTIHASH, SSDEEP, IMPHASH) }
case object MD5 extends CryptoHashType
case object SHA1 extends CryptoHashType
case object SHA256 extends CryptoHashType
case object SHA512 extends CryptoHashType
case object AUTHENTIHASH extends CryptoHashType
case object SSDEEP extends CryptoHashType
case object IMPHASH extends CryptoHashType

// New
sealed trait HostType extends CDM18
case object HostType extends CustomEnum[HostType] { val values = Seq(HOST_MOBILE, HOST_SERVER, HOST_DESKTOP) }
case object HOST_MOBILE extends HostType
case object HOST_SERVER extends HostType
case object HOST_DESKTOP extends HostType

//  /* Some Python for generating the CustomEnum[T] code above */
//def make_enum(name, string_array):
//    trait = "sealed trait " + name + "\n"
//    values_list = []
//    object_list = []
//    for i in string_array:
//        values_list.append(i)
//        object_list.append("case object " + i + " extends " + name + "\n")
//    object = "case object " + name + " extends CustomEnum[" + name + "] { val values = Seq(" + ", ".join(values_list) + ") }\n"
//    all = [trait] + [object] + object_list
//    print "\n" + "".join(all)
