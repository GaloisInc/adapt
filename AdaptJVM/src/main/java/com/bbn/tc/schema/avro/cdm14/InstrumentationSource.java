/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;
@SuppressWarnings("all")
/** * InstrumentationSource identifies the source reporting provenance information.
     *
     * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation
     * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation
     * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM
     * SOURCE_FREEBSD_DTRACE_CADETS,      from CADETS freebsd instrumentation
     * SOURCE_FREEBSD_TESLA_CADETS,       from CADETS freebsd instrumentation
     * SOURCE_FREEBSD_LOOM_CADETS,        from CADETS freebsd instrumentation
     * SOURCE_FREEBSD_MACIF_CADETS,       from CADETS freebsd instrumentation
     * SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit
     * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc
     * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation
     * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation source
     * SOURCE_WINDOWS_DIFT_FAROS,         from FAROS' DIFT module
     * SOURCE_WINDOWS_PSA_FAROS,          from FAROS' PSA module
     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections windows events */
@org.apache.avro.specific.AvroGenerated
public enum InstrumentationSource {
  SOURCE_ANDROID_JAVA_CLEARSCOPE, SOURCE_ANDROID_NATIVE_CLEARSCOPE, SOURCE_FREEBSD_OPENBSM_TRACE, SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS, SOURCE_FREEBSD_LOOM_CADETS, SOURCE_FREEBSD_MACIF_CADETS, SOURCE_LINUX_AUDIT_TRACE, SOURCE_LINUX_PROC_TRACE, SOURCE_LINUX_BEEP_TRACE, SOURCE_LINUX_THEIA, SOURCE_WINDOWS_DIFT_FAROS, SOURCE_WINDOWS_PSA_FAROS, SOURCE_WINDOWS_FIVEDIRECTIONS  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* InstrumentationSource identifies the source reporting provenance information.\\n     *\\n     * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation\\n     * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation\\n     * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM\\n     * SOURCE_FREEBSD_DTRACE_CADETS,      from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_TESLA_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_LOOM_CADETS,        from CADETS freebsd instrumentation\\n     * SOURCE_FREEBSD_MACIF_CADETS,       from CADETS freebsd instrumentation\\n     * SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit\\n     * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc\\n     * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation\\n     * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation source\\n     * SOURCE_WINDOWS_DIFT_FAROS,         from FAROS' DIFT module\\n     * SOURCE_WINDOWS_PSA_FAROS,          from FAROS' PSA module\\n     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections windows events\",\"symbols\":[\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_LINUX_AUDIT_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_WINDOWS_PSA_FAROS\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
