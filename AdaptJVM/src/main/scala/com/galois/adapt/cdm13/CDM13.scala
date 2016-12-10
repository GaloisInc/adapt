package com.galois.adapt.cdm13


class FixedShort(val bytes: Array[Byte]) extends AnyVal


trait CustomEnum[T] extends CDM13 {
  val values: Seq[T]
  def from(s: String): Option[T] = values.find(_.toString == s)
}


sealed trait TagOpCode extends CDM13
case object TagOpCode extends CustomEnum[TagOpCode] { val values = Seq(TAG_OP_SEQUENCE, TAG_OP_UNION, TAG_OP_ENCODE, TAG_OP_STRONG, TAG_OP_MEDIUM, TAG_OP_WEAK) }
case object TAG_OP_SEQUENCE extends TagOpCode
case object TAG_OP_UNION extends TagOpCode
case object TAG_OP_ENCODE extends TagOpCode
case object TAG_OP_STRONG extends TagOpCode
case object TAG_OP_MEDIUM extends TagOpCode
case object TAG_OP_WEAK extends TagOpCode


sealed trait SubjectType extends CDM13
case object SubjectType extends CustomEnum[SubjectType] { val values = Seq(SUBJECT_PROCESS, SUBJECT_THREAD, SUBJECT_UNIT, SUBJECT_BASIC_BLOCK) }
case object SUBJECT_PROCESS extends SubjectType
case object SUBJECT_THREAD extends SubjectType
case object SUBJECT_UNIT extends SubjectType
case object SUBJECT_BASIC_BLOCK extends SubjectType


sealed trait InstrumentationSource extends CDM13
case object InstrumentationSource extends CustomEnum[InstrumentationSource] { val values = Seq(SOURCE_LINUX_AUDIT_TRACE, SOURCE_LINUX_PROC_TRACE, SOURCE_LINUX_BEEP_TRACE, SOURCE_FREEBSD_OPENBSM_TRACE, SOURCE_ANDROID_JAVA_CLEARSCOPE, SOURCE_ANDROID_NATIVE_CLEARSCOPE, SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS, SOURCE_FREEBSD_LOOM_CADETS, SOURCE_FREEBSD_MACIF_CADETS, SOURCE_WINDOWS_DIFT_FAROS, SOURCE_LINUX_THEIA, SOURCE_WINDOWS_FIVEDIRECTIONS) }
case object SOURCE_LINUX_AUDIT_TRACE extends InstrumentationSource
case object SOURCE_LINUX_PROC_TRACE extends InstrumentationSource
case object SOURCE_LINUX_BEEP_TRACE extends InstrumentationSource
case object SOURCE_FREEBSD_OPENBSM_TRACE extends InstrumentationSource
case object SOURCE_ANDROID_JAVA_CLEARSCOPE extends InstrumentationSource
case object SOURCE_ANDROID_NATIVE_CLEARSCOPE extends InstrumentationSource
case object SOURCE_FREEBSD_DTRACE_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_TESLA_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_LOOM_CADETS extends InstrumentationSource
case object SOURCE_FREEBSD_MACIF_CADETS extends InstrumentationSource
case object SOURCE_WINDOWS_DIFT_FAROS extends InstrumentationSource
case object SOURCE_LINUX_THEIA extends InstrumentationSource
case object SOURCE_WINDOWS_FIVEDIRECTIONS extends InstrumentationSource


sealed trait IntegrityTag extends CDM13
case object IntegrityTag extends CustomEnum[IntegrityTag] { val values = Seq(INTEGRITY_UNTRUSTED, INTEGRITY_BENIGN, INTEGRITY_INVULNERABLE) }
case object INTEGRITY_UNTRUSTED extends IntegrityTag
case object INTEGRITY_BENIGN extends IntegrityTag
case object INTEGRITY_INVULNERABLE extends IntegrityTag


sealed trait ConfidentialityTag extends CDM13
case object ConfidentialityTag extends CustomEnum[ConfidentialityTag] { val values = Seq(CONFIDENTIALITY_SECRET, CONFIDENTIALITY_SENSITIVE, CONFIDENTIALITY_PRIVATE, CONFIDENTIALITY_PUBLIC) }
case object CONFIDENTIALITY_SECRET extends ConfidentialityTag
case object CONFIDENTIALITY_SENSITIVE extends ConfidentialityTag
case object CONFIDENTIALITY_PRIVATE extends ConfidentialityTag
case object CONFIDENTIALITY_PUBLIC extends ConfidentialityTag


sealed trait EventType extends CDM13
case object EventType extends CustomEnum[EventType] { val values = Seq(EVENT_ACCEPT, EVENT_BIND, EVENT_CHANGE_PRINCIPAL, EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLONE, EVENT_CLOSE, EVENT_CONNECT, EVENT_CREATE_OBJECT, EVENT_CREATE_THREAD, EVENT_EXECUTE, EVENT_FORK, EVENT_LINK, EVENT_UNLINK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_MPROTECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_RENAME, EVENT_WRITE, EVENT_SIGNAL, EVENT_TRUNCATE, EVENT_WAIT, EVENT_OS_UNKNOWN, EVENT_KERNEL_UNKNOWN, EVENT_APP_UNKNOWN, EVENT_UI_UNKNOWN, EVENT_UNKNOWN, EVENT_BLIND, EVENT_UNIT, EVENT_UPDATE, EVENT_SENDTO, EVENT_SENDMSG, EVENT_SHM, EVENT_EXIT) }
case object EVENT_ACCEPT extends EventType
case object EVENT_BIND extends EventType
case object EVENT_CHANGE_PRINCIPAL extends EventType
case object EVENT_CHECK_FILE_ATTRIBUTES extends EventType
case object EVENT_CLONE extends EventType
case object EVENT_CLOSE extends EventType
case object EVENT_CONNECT extends EventType
case object EVENT_CREATE_OBJECT extends EventType
case object EVENT_CREATE_THREAD extends EventType
case object EVENT_EXECUTE extends EventType
case object EVENT_FORK extends EventType
case object EVENT_LINK extends EventType
case object EVENT_UNLINK extends EventType
case object EVENT_MMAP extends EventType
case object EVENT_MODIFY_FILE_ATTRIBUTES extends EventType
case object EVENT_MPROTECT extends EventType
case object EVENT_OPEN extends EventType
case object EVENT_READ extends EventType
case object EVENT_RECVFROM extends EventType
case object EVENT_RECVMSG extends EventType
case object EVENT_RENAME extends EventType
case object EVENT_WRITE extends EventType
case object EVENT_SIGNAL extends EventType
case object EVENT_TRUNCATE extends EventType
case object EVENT_WAIT extends EventType
case object EVENT_OS_UNKNOWN extends EventType
case object EVENT_KERNEL_UNKNOWN extends EventType
case object EVENT_APP_UNKNOWN extends EventType
case object EVENT_UI_UNKNOWN extends EventType
case object EVENT_UNKNOWN extends EventType
case object EVENT_BLIND extends EventType
case object EVENT_UNIT extends EventType
case object EVENT_UPDATE extends EventType
case object EVENT_SENDTO extends EventType
case object EVENT_SENDMSG extends EventType
case object EVENT_SHM extends EventType
case object EVENT_EXIT extends EventType


sealed trait ValueType extends CDM13
case object ValueType extends CustomEnum[ValueType] { val values = Seq(VALUE_TYPE_IN, VALUE_TYPE_OUT, VALUE_TYPE_INOUT) }
case object VALUE_TYPE_IN extends ValueType
case object VALUE_TYPE_OUT extends ValueType
case object VALUE_TYPE_INOUT extends ValueType


sealed trait ValueDataType extends CDM13
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


sealed trait SrcSinkType extends CDM13
case object SrcSinkType extends CustomEnum[SrcSinkType] { val values = Seq(SOURCE_ACCELEROMETER, SOURCE_TEMPERATURE, SOURCE_GYROSCOPE, SOURCE_MAGNETIC_FIELD, SOURCE_HEART_RATE, SOURCE_LIGHT, SOURCE_PROXIMITY, SOURCE_PRESSURE, SOURCE_RELATIVE_HUMIDITY, SOURCE_LINEAR_ACCELERATION, SOURCE_MOTION, SOURCE_STEP_DETECTOR, SOURCE_STEP_COUNTER, SOURCE_TILT_DETECTOR, SOURCE_ROTATION_VECTOR, SOURCE_GRAVITY, SOURCE_GEOMAGNETIC_ROTATION_VECTOR, SOURCE_CAMERA, SOURCE_GPS, SOURCE_AUDIO, SOURCE_SYSTEM_PROPERTY, SOURCE_ENV_VARIABLE, SOURCE_SINK_IPC, SOURCE_UNKNOWN) }
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
case object SOURCE_SINK_IPC extends SrcSinkType
case object SOURCE_UNKNOWN extends SrcSinkType


sealed trait PrincipalType extends CDM13
case object PrincipalType extends CustomEnum[PrincipalType] { val values = Seq(PRINCIPAL_LOCAL, PRINCIPAL_REMOTE) }
case object PRINCIPAL_LOCAL extends PrincipalType
case object PRINCIPAL_REMOTE extends PrincipalType


sealed trait EdgeType extends CDM13
case object EdgeType extends CustomEnum[EdgeType] { val values = Seq(EDGE_EVENT_AFFECTS_MEMORY, EDGE_EVENT_AFFECTS_FILE, EDGE_EVENT_AFFECTS_NETFLOW, EDGE_EVENT_AFFECTS_SUBJECT, EDGE_EVENT_AFFECTS_SRCSINK, EDGE_EVENT_HASPARENT_EVENT, EDGE_EVENT_CAUSES_EVENT, EDGE_EVENT_ISGENERATEDBY_SUBJECT, EDGE_SUBJECT_AFFECTS_EVENT, EDGE_SUBJECT_HASPARENT_SUBJECT, EDGE_SUBJECT_HASLOCALPRINCIPAL, EDGE_SUBJECT_RUNSON, EDGE_FILE_AFFECTS_EVENT, EDGE_NETFLOW_AFFECTS_EVENT, EDGE_MEMORY_AFFECTS_EVENT, EDGE_SRCSINK_AFFECTS_EVENT, EDGE_OBJECT_PREV_VERSION, EDGE_FILE_HAS_TAG, EDGE_NETFLOW_HAS_TAG, EDGE_MEMORY_HAS_TAG, EDGE_SRCSINK_HAS_TAG, EDGE_SUBJECT_HAS_TAG, EDGE_EVENT_HAS_TAG, EDGE_EVENT_AFFECTS_REGISTRYKEY, EDGE_REGISTRYKEY_AFFECTS_EVENT, EDGE_REGISTRYKEY_HAS_TAG) }
case object EDGE_EVENT_AFFECTS_MEMORY extends EdgeType
case object EDGE_EVENT_AFFECTS_FILE extends EdgeType
case object EDGE_EVENT_AFFECTS_NETFLOW extends EdgeType
case object EDGE_EVENT_AFFECTS_SUBJECT extends EdgeType
case object EDGE_EVENT_AFFECTS_SRCSINK extends EdgeType
case object EDGE_EVENT_HASPARENT_EVENT extends EdgeType
case object EDGE_EVENT_CAUSES_EVENT extends EdgeType
case object EDGE_EVENT_ISGENERATEDBY_SUBJECT extends EdgeType
case object EDGE_SUBJECT_AFFECTS_EVENT extends EdgeType
case object EDGE_SUBJECT_HASPARENT_SUBJECT extends EdgeType
case object EDGE_SUBJECT_HASLOCALPRINCIPAL extends EdgeType
case object EDGE_SUBJECT_RUNSON extends EdgeType
case object EDGE_FILE_AFFECTS_EVENT extends EdgeType
case object EDGE_NETFLOW_AFFECTS_EVENT extends EdgeType
case object EDGE_MEMORY_AFFECTS_EVENT extends EdgeType
case object EDGE_SRCSINK_AFFECTS_EVENT extends EdgeType
case object EDGE_OBJECT_PREV_VERSION extends EdgeType
case object EDGE_FILE_HAS_TAG extends EdgeType
case object EDGE_NETFLOW_HAS_TAG extends EdgeType
case object EDGE_MEMORY_HAS_TAG extends EdgeType
case object EDGE_SRCSINK_HAS_TAG extends EdgeType
case object EDGE_SUBJECT_HAS_TAG extends EdgeType
case object EDGE_EVENT_HAS_TAG extends EdgeType
case object EDGE_EVENT_AFFECTS_REGISTRYKEY extends EdgeType
case object EDGE_REGISTRYKEY_AFFECTS_EVENT extends EdgeType
case object EDGE_REGISTRYKEY_HAS_TAG extends EdgeType
