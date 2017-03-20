package com.galois.adapt.cdm16

trait CustomEnum[T] extends CDM16 {
  val values: Seq[T]
  def from(s: String): Option[T] = values.find(_.toString == s)  // TODO: strings will be slow. Use ordinals.
}

class FixedShort(val bytes: Array[Byte]) extends AnyVal

sealed trait SubjectType extends CDM16
case object SubjectType extends CustomEnum[SubjectType] { val values = Seq(SUBJECT_PROCESS, SUBJECT_THREAD, SUBJECT_UNIT, SUBJECT_BASIC_BLOCK) }
case object SUBJECT_PROCESS extends SubjectType
case object SUBJECT_THREAD extends SubjectType
case object SUBJECT_UNIT extends SubjectType
case object SUBJECT_BASIC_BLOCK extends SubjectType

sealed trait PrivilegeLevel extends CDM16
case object PrivilegeLevel extends CustomEnum[PrivilegeLevel] { val values = Seq(LIMITED, ELEVATED, FULL) }
case object LIMITED extends PrivilegeLevel
case object ELEVATED extends PrivilegeLevel
case object FULL extends PrivilegeLevel

sealed trait SrcSinkType extends CDM16
case object SrcSinkType extends CustomEnum[SrcSinkType] { val values = Seq(SOURCE_ACCELEROMETER, SOURCE_TEMPERATURE, SOURCE_GYROSCOPE, SOURCE_MAGNETIC_FIELD, SOURCE_HEART_RATE, SOURCE_LIGHT, SOURCE_PROXIMITY, SOURCE_PRESSURE, SOURCE_RELATIVE_HUMIDITY, SOURCE_LINEAR_ACCELERATION, SOURCE_MOTION, SOURCE_STEP_DETECTOR, SOURCE_STEP_COUNTER, SOURCE_TILT_DETECTOR, SOURCE_ROTATION_VECTOR, SOURCE_GRAVITY, SOURCE_GEOMAGNETIC_ROTATION_VECTOR, SOURCE_CAMERA, SOURCE_GPS, SOURCE_AUDIO, SOURCE_SYSTEM_PROPERTY, SOURCE_ENV_VARIABLE, SOURCE_ACCESSIBILITY_SERVICE, SOURCE_ACTIVITY_MANAGEMENT, SOURCE_ALARM_SERVICE, SOURCE_ANDROID_TV, SOURCE_AUDIO_IO, SOURCE_BACKUP_MANAGER, SOURCE_BINDER, SOURCE_BLUETOOTH, SOURCE_BOOT_EVENT, SOURCE_BROADCAST_RECEIVER_MANAGEMENT, SOURCE_CLIPBOARD, SOURCE_COMPONENT_MANAGEMENT, SOURCE_CONTENT_PROVIDER, SOURCE_CONTENT_PROVIDER_MANAGEMENT, SOURCE_DATABASE, SOURCE_DEVICE_ADMIN, SOURCE_DEVICE_SEARCH, SOURCE_DEVICE_USER, SOURCE_DISPLAY, SOURCE_DROPBOX, SOURCE_EMAIL, SOURCE_EXPERIMENTAL, SOURCE_FILE, SOURCE_FILE_SYSTEM_MANAGEMENT, SOURCE_FINGERPRINT, SOURCE_FLASHLIGHT, SOURCE_HDMI, SOURCE_IDLE_DOCK_SCREEN, SOURCE_IMS, SOURCE_INFRARED, SOURCE_INSTALLED_PACKAGES, SOURCE_JSSE_TRUST_MANAGER, SOURCE_KEYCHAIN, SOURCE_KEYGUARD, SOURCE_LOCATION, SOURCE_MACHINE_LEARNING, SOURCE_MEDIA_LOCAL_MANAGEMENT, SOURCE_MEDIA_LOCAL_PLAYBACK, SOURCE_MEDIA_NETWORK_CONNECTION, SOURCE_MEDIA_REMOTE_PLAYBACK, SOURCE_NETWORK_MANAGEMENT, SOURCE_NFC, SOURCE_NOTIFICATION, SOURCE_PAC_PROXY, SOURCE_PERMISSIONS, SOURCE_PERSISTANT_DATA, SOURCE_POWER_MANAGEMENT, SOURCE_PRINT_SERVICE, SOURCE_PROCESS_MANAGEMENT, SOURCE_RPC, SOURCE_SCREEN_AUDIO_CAPTURE, SOURCE_SERIAL_PORT, SOURCE_SERVICE_MANAGEMENT, SOURCE_SMS_MMS, SOURCE_SPEECH_INTERACTION, SOURCE_STATUS_BAR, SOURCE_SYNC_FRAMEWORK, SOURCE_TELEPHONY, SOURCE_TEXT_SERVICES, SOURCE_THREADING, SOURCE_TIME_EVENT, SOURCE_UI, SOURCE_UI_AUTOMATION, SOURCE_UI_RPC, SOURCE_UID_EVENT, SOURCE_USAGE_STATS, SOURCE_USB, SOURCE_USER_ACCOUNTS_MANAGEMENT, SOURCE_VIBRATOR, SOURCE_WAKE_LOCK, SOURCE_WALLPAPER_MANAGER, SOURCE_WAP, SOURCE_WEB_BROWSER, SOURCE_WIDGETS, SOURCE_SINK_IPC, SOURCE_SINK_UNKNOWN) }
case object SOURCE_ACCELEROMETER extends SrcSinkType
case object SOURCE_TEMPERATURE extends SrcSinkType
case object SOURCE_GYROSCOPE extends SrcSinkType
case object SOURCE_MAGNETIC_FIELD extends SrcSinkType
case object SOURCE_HEART_RATE extends SrcSinkType
case object SOURCE_LIGHT extends SrcSinkType
case object SOURCE_PROXIMITY extends SrcSinkType
case object SOURCE_PRESSURE extends SrcSinkType
case object SOURCE_RELATIVE_HUMIDITY extends SrcSinkType
case object SOURCE_LINEAR_ACCELERATION extends SrcSinkType
case object SOURCE_MOTION extends SrcSinkType
case object SOURCE_STEP_DETECTOR extends SrcSinkType
case object SOURCE_STEP_COUNTER extends SrcSinkType
case object SOURCE_TILT_DETECTOR extends SrcSinkType
case object SOURCE_ROTATION_VECTOR extends SrcSinkType
case object SOURCE_GRAVITY extends SrcSinkType
case object SOURCE_GEOMAGNETIC_ROTATION_VECTOR extends SrcSinkType
case object SOURCE_CAMERA extends SrcSinkType
case object SOURCE_GPS extends SrcSinkType
case object SOURCE_AUDIO extends SrcSinkType
case object SOURCE_SYSTEM_PROPERTY extends SrcSinkType
case object SOURCE_ENV_VARIABLE extends SrcSinkType
case object SOURCE_ACCESSIBILITY_SERVICE extends SrcSinkType
case object SOURCE_ACTIVITY_MANAGEMENT extends SrcSinkType
case object SOURCE_ALARM_SERVICE extends SrcSinkType
case object SOURCE_ANDROID_TV extends SrcSinkType
case object SOURCE_AUDIO_IO extends SrcSinkType
case object SOURCE_BACKUP_MANAGER extends SrcSinkType
case object SOURCE_BINDER extends SrcSinkType
case object SOURCE_BLUETOOTH extends SrcSinkType
case object SOURCE_BOOT_EVENT extends SrcSinkType
case object SOURCE_BROADCAST_RECEIVER_MANAGEMENT extends SrcSinkType
case object SOURCE_CLIPBOARD extends SrcSinkType
case object SOURCE_COMPONENT_MANAGEMENT extends SrcSinkType
case object SOURCE_CONTENT_PROVIDER extends SrcSinkType
case object SOURCE_CONTENT_PROVIDER_MANAGEMENT extends SrcSinkType
case object SOURCE_DATABASE extends SrcSinkType
case object SOURCE_DEVICE_ADMIN extends SrcSinkType
case object SOURCE_DEVICE_SEARCH extends SrcSinkType
case object SOURCE_DEVICE_USER extends SrcSinkType
case object SOURCE_DISPLAY extends SrcSinkType
case object SOURCE_DROPBOX extends SrcSinkType
case object SOURCE_EMAIL extends SrcSinkType
case object SOURCE_EXPERIMENTAL extends SrcSinkType
case object SOURCE_FILE extends SrcSinkType
case object SOURCE_FILE_SYSTEM_MANAGEMENT extends SrcSinkType
case object SOURCE_FINGERPRINT extends SrcSinkType
case object SOURCE_FLASHLIGHT extends SrcSinkType
case object SOURCE_HDMI extends SrcSinkType
case object SOURCE_IDLE_DOCK_SCREEN extends SrcSinkType
case object SOURCE_IMS extends SrcSinkType
case object SOURCE_INFRARED extends SrcSinkType
case object SOURCE_INSTALLED_PACKAGES extends SrcSinkType
case object SOURCE_JSSE_TRUST_MANAGER extends SrcSinkType
case object SOURCE_KEYCHAIN extends SrcSinkType
case object SOURCE_KEYGUARD extends SrcSinkType
case object SOURCE_LOCATION extends SrcSinkType
case object SOURCE_MACHINE_LEARNING extends SrcSinkType
case object SOURCE_MEDIA_LOCAL_MANAGEMENT extends SrcSinkType
case object SOURCE_MEDIA_LOCAL_PLAYBACK extends SrcSinkType
case object SOURCE_MEDIA_NETWORK_CONNECTION extends SrcSinkType
case object SOURCE_MEDIA_REMOTE_PLAYBACK extends SrcSinkType
case object SOURCE_NETWORK_MANAGEMENT extends SrcSinkType
case object SOURCE_NFC extends SrcSinkType
case object SOURCE_NOTIFICATION extends SrcSinkType
case object SOURCE_PAC_PROXY extends SrcSinkType
case object SOURCE_PERMISSIONS extends SrcSinkType
case object SOURCE_PERSISTANT_DATA extends SrcSinkType
case object SOURCE_POWER_MANAGEMENT extends SrcSinkType
case object SOURCE_PRINT_SERVICE extends SrcSinkType
case object SOURCE_PROCESS_MANAGEMENT extends SrcSinkType
case object SOURCE_RPC extends SrcSinkType
case object SOURCE_SCREEN_AUDIO_CAPTURE extends SrcSinkType
case object SOURCE_SERIAL_PORT extends SrcSinkType
case object SOURCE_SERVICE_MANAGEMENT extends SrcSinkType
case object SOURCE_SMS_MMS extends SrcSinkType
case object SOURCE_SPEECH_INTERACTION extends SrcSinkType
case object SOURCE_STATUS_BAR extends SrcSinkType
case object SOURCE_SYNC_FRAMEWORK extends SrcSinkType
case object SOURCE_TELEPHONY extends SrcSinkType
case object SOURCE_TEXT_SERVICES extends SrcSinkType
case object SOURCE_THREADING extends SrcSinkType
case object SOURCE_TIME_EVENT extends SrcSinkType
case object SOURCE_UI extends SrcSinkType
case object SOURCE_UI_AUTOMATION extends SrcSinkType
case object SOURCE_UI_RPC extends SrcSinkType
case object SOURCE_UID_EVENT extends SrcSinkType
case object SOURCE_USAGE_STATS extends SrcSinkType
case object SOURCE_USB extends SrcSinkType
case object SOURCE_USER_ACCOUNTS_MANAGEMENT extends SrcSinkType
case object SOURCE_VIBRATOR extends SrcSinkType
case object SOURCE_WAKE_LOCK extends SrcSinkType
case object SOURCE_WALLPAPER_MANAGER extends SrcSinkType
case object SOURCE_WAP extends SrcSinkType
case object SOURCE_WEB_BROWSER extends SrcSinkType
case object SOURCE_WIDGETS extends SrcSinkType
case object SOURCE_SINK_IPC extends SrcSinkType
case object SOURCE_SINK_UNKNOWN extends SrcSinkType

sealed trait InstrumentationSource extends CDM16
case object InstrumentationSource extends CustomEnum[InstrumentationSource] { val values = Seq(SOURCE_ANDROID_JAVA_CLEARSCOPE, SOURCE_ANDROID_NATIVE_CLEARSCOPE, SOURCE_FREEBSD_OPENBSM_TRACE, SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS, SOURCE_FREEBSD_LOOM_CADETS, SOURCE_FREEBSD_MACIF_CADETS, SOURCE_LINUX_AUDIT_TRACE, SOURCE_LINUX_PROC_TRACE, SOURCE_LINUX_BEEP_TRACE, SOURCE_LINUX_THEIA, SOURCE_WINDOWS_DIFT_FAROS, SOURCE_WINDOWS_PSA_FAROS, SOURCE_WINDOWS_FIVEDIRECTIONS) }
case object SOURCE_ANDROID_JAVA_CLEARSCOPE extends InstrumentationSource
case object SOURCE_ANDROID_NATIVE_CLEARSCOPE extends InstrumentationSource
case object SOURCE_FREEBSD_OPENBSM_TRACE extends InstrumentationSource
case object SOURCE_FREEBSD_DTRACE_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_TESLA_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_LOOM_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_MACIF_CADETS extends InstrumentationSource
case object SOURCE_LINUX_AUDIT_TRACE extends InstrumentationSource
case object SOURCE_LINUX_PROC_TRACE extends InstrumentationSource
case object SOURCE_LINUX_BEEP_TRACE extends InstrumentationSource
case object SOURCE_LINUX_THEIA extends InstrumentationSource
case object SOURCE_WINDOWS_DIFT_FAROS extends InstrumentationSource
case object SOURCE_WINDOWS_PSA_FAROS extends InstrumentationSource
case object SOURCE_WINDOWS_FIVEDIRECTIONS extends InstrumentationSource

sealed trait PrincipalType extends CDM16
case object PrincipalType extends CustomEnum[PrincipalType] { val values = Seq(PRINCIPAL_LOCAL, PRINCIPAL_REMOTE) }
case object PRINCIPAL_LOCAL extends PrincipalType
case object PRINCIPAL_REMOTE extends PrincipalType

sealed trait EventType extends CDM16
case object EventType extends CustomEnum[EventType] { val values = Seq(EVENT_ACCEPT, EVENT_BIND, EVENT_BLIND, EVENT_CHANGE_PRINCIPAL, EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLONE, EVENT_CLOSE, EVENT_CONNECT, EVENT_CREATE_OBJECT, EVENT_CREATE_THREAD, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_FORK, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_MODIFY_PROCESS, EVENT_MPROTECT, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_RENAME, EVENT_SENDTO, EVENT_SENDMSG, EVENT_SHM, EVENT_SIGNAL, EVENT_TRUNCATE, EVENT_UNIT, EVENT_UNLINK, EVENT_WAIT, EVENT_WRITE, EVENT_EXIT, EVENT_LOADLIBRARY, EVENT_BOOT, EVENT_LOGCLEAR, EVENT_MOUNT, EVENT_STARTSERVICE, EVENT_LOGIN, EVENT_LOGOUT) }
case object EVENT_ACCEPT extends EventType
case object EVENT_BIND extends EventType
case object EVENT_BLIND extends EventType
case object EVENT_CHANGE_PRINCIPAL extends EventType
case object EVENT_CHECK_FILE_ATTRIBUTES extends EventType
case object EVENT_CLONE extends EventType
case object EVENT_CLOSE extends EventType
case object EVENT_CONNECT extends EventType
case object EVENT_CREATE_OBJECT extends EventType
case object EVENT_CREATE_THREAD extends EventType
case object EVENT_DUP extends EventType
case object EVENT_EXECUTE extends EventType
case object EVENT_FNCTL extends EventType
case object EVENT_FORK extends EventType
case object EVENT_LINK extends EventType
case object EVENT_LSEEK extends EventType
case object EVENT_MMAP extends EventType
case object EVENT_MODIFY_FILE_ATTRIBUTES extends EventType
case object EVENT_MODIFY_PROCESS extends EventType
case object EVENT_MPROTECT extends EventType
case object EVENT_OPEN extends EventType
case object EVENT_OTHER extends EventType
case object EVENT_READ extends EventType
case object EVENT_RECVFROM extends EventType
case object EVENT_RECVMSG extends EventType
case object EVENT_RENAME extends EventType
case object EVENT_SENDTO extends EventType
case object EVENT_SENDMSG extends EventType
case object EVENT_SHM extends EventType
case object EVENT_SIGNAL extends EventType
case object EVENT_TRUNCATE extends EventType
case object EVENT_UNIT extends EventType
case object EVENT_UNLINK extends EventType
case object EVENT_UPDATE extends EventType
case object EVENT_WAIT extends EventType
case object EVENT_WRITE extends EventType
case object EVENT_EXIT extends EventType
case object EVENT_LOADLIBRARY extends EventType
case object EVENT_BOOT extends EventType
case object EVENT_LOGCLEAR extends EventType
case object EVENT_MOUNT extends EventType
case object EVENT_STARTSERVICE extends EventType
case object EVENT_LOGIN extends EventType
case object EVENT_LOGOUT extends EventType

sealed trait FileObjectType extends CDM16
case object FileObjectType extends CustomEnum[FileObjectType] { val values = Seq(FILE_OBJECT_FILE, FILE_OBJECT_DIR, FILE_OBJECT_NAMED_PIPE, FILE_OBJECT_UNIX_SOCKET, FILE_OBJECT_PEFILE) }
case object FILE_OBJECT_FILE extends FileObjectType
case object FILE_OBJECT_DIR extends FileObjectType
case object FILE_OBJECT_NAMED_PIPE extends FileObjectType
case object FILE_OBJECT_UNIX_SOCKET extends FileObjectType
case object FILE_OBJECT_PEFILE extends FileObjectType

sealed trait ValueType extends CDM16
case object ValueType extends CustomEnum[ValueType] { val values = Seq(VALUE_TYPE_SRC, VALUE_TYPE_SINK, VALUE_TYPE_CONTROL) }
case object VALUE_TYPE_SRC extends ValueType
case object VALUE_TYPE_SINK extends ValueType
case object VALUE_TYPE_CONTROL extends ValueType

sealed trait ValueDataType extends CDM16
case object ValueDataType extends CustomEnum[ValueDataType] { val values = Seq(VALUE_DATA_TYPE_BYTE, VALUE_DATA_TYPE_BOOL, VALUE_DATA_TYPE_CHAR, VALUE_DATA_TYPE_SHORT, VALUE_DATA_TYPE_INT, VALUE_DATA_TYPE_FLOAT, VALUE_DATA_TYPE_LONG, VALUE_DATA_TYPE_DOUBLE, VALUE_DATA_TYPE_COMPLEX) }
case object VALUE_DATA_TYPE_BYTE extends ValueDataType
case object VALUE_DATA_TYPE_BOOL extends ValueDataType
case object VALUE_DATA_TYPE_CHAR extends ValueDataType
case object VALUE_DATA_TYPE_SHORT extends ValueDataType
case object VALUE_DATA_TYPE_INT extends ValueDataType
case object VALUE_DATA_TYPE_FLOAT extends ValueDataType
case object VALUE_DATA_TYPE_LONG extends ValueDataType
case object VALUE_DATA_TYPE_DOUBLE extends ValueDataType
case object VALUE_DATA_TYPE_COMPLEX extends ValueDataType

sealed trait TagOpCode extends CDM16
case object TagOpCode extends CustomEnum[TagOpCode] { val values = Seq(TAG_OP_UNION, TAG_OP_ENCODE, TAG_OP_STRONG, TAG_OP_MEDIUM, TAG_OP_WEAK) }
case object TAG_OP_UNION extends TagOpCode
case object TAG_OP_ENCODE extends TagOpCode
case object TAG_OP_STRONG extends TagOpCode
case object TAG_OP_MEDIUM extends TagOpCode
case object TAG_OP_WEAK extends TagOpCode

sealed trait IntegrityTag extends CDM16
case object IntegrityTag extends CustomEnum[IntegrityTag] { val values = Seq(INTEGRITY_UNTRUSTED, INTEGRITY_BENIGN, INTEGRITY_INVULNERABLE) }
case object INTEGRITY_UNTRUSTED extends IntegrityTag
case object INTEGRITY_BENIGN extends IntegrityTag
case object INTEGRITY_INVULNERABLE extends IntegrityTag

sealed trait ConfidentialityTag extends CDM16
case object ConfidentialityTag extends CustomEnum[ConfidentialityTag] { val values = Seq(CONFIDENTIALITY_SECRET, CONFIDENTIALITY_SENSITIVE, CONFIDENTIALITY_PRIVATE, CONFIDENTIALITY_PUBLIC) }
case object CONFIDENTIALITY_SECRET extends ConfidentialityTag
case object CONFIDENTIALITY_SENSITIVE extends ConfidentialityTag
case object CONFIDENTIALITY_PRIVATE extends ConfidentialityTag
case object CONFIDENTIALITY_PUBLIC extends ConfidentialityTag

sealed trait CryptoHashType extends CDM16
case object CryptoHashType extends CustomEnum[CryptoHashType] { val values = Seq(MD5, SHA1, SHA256, SHA512, AUTENTIHASH, SSDEEP, IMPHASH)}
case object MD5 extends CryptoHashType
case object SHA1 extends CryptoHashType
case object SHA256 extends CryptoHashType
case object SHA512 extends CryptoHashType
case object AUTENTIHASH extends CryptoHashType
case object SSDEEP extends CryptoHashType
case object IMPHASH extends CryptoHashType

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
