/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;
@SuppressWarnings("all")
/** * The different types of simple edges in the CDM
     * EDGE_EVENT_AFFECTS_MEMORY          an event affects a memory object (such as updating a memory location)
     *   EDGE_EVENT_AFFECTS_FILE            an event affects a file object (such as writing to a file)
     * EDGE_EVENT_AFFECTS_REGISTRYKEY     an event affects a registry value (such as writing to a registry key)
     *   EDGE_EVENT_AFFECTS_NETFLOW         an event affects a netflow object (such as writing to socket)
     * EDGE_EVENT_AFFECTS_SUBJECT         an event affects a subject (such as forking a process)
     *   EDGE_EVENT_AFFECTS_SRCSINK         an event affects a generic src/sink object
     *   EDGE_EVENT_HASPARENT_EVENT         a metaevent that represents a set of child atomic events
     *   EDGE_EVENT_CAUSES_EVENT an event caused/triggered another event
     * EDGE_EVENT_ISGENERATEDBY_SUBJECT   an event is generated by a subject (every event is)
     *   EDGE_SUBJECT_AFFECTS_EVENT         a subject affects an event (such as when killing a process)
     *   EDGE_SUBJECT_HASPARENT_SUBJECT a subject has a parent subject (such as thread has parent process)
     * EDGE_SUBJECT_HASPRINCIPAL          a subject has a principal (such as a process owned by a user)
     *   EDGE_SUBJECT_RUNSON                a subject runs on a host (TODO: host not yet modeled)
     *   EDGE_FILE_AFFECTS_EVENT an event reads from a file
     *   EDGE_REGISTRYKEY_AFFECTS_EVENT     an event reads values from a registry key
     *   EDGE_NETFLOW_AFFECTS_EVENT         an event reads from a network flow
     *   EDGE_MEMORY_AFFECTS_EVENT          an event reads from a memory object
     *   EDGE_SRCSINK_AFFECTS_EVENT         a generic source/sink object affects an event
     *   EDGE_OBJECT_PREV_VERSION the previous version of an object, typically used for file versioning
     * EDGE_FILE_HAS_TAG                  attach a tag to a file object
     * EDGE_REGISTRYKEY_HAS_TAG           attach a tag to a registrykey object
     * EDGE_NETFLOW_HAS_TAG               attach a tag to a netflow object
     * EDGE_MEMORY_HAS_TAG                attach a tag to a memory object
     * EDGE_SRCSINK_HAS_TAG               attach a tag to a srcsink object
     * EDGE_SUBJECT_HAS_TAG               attach a tag to a subject
     * EDGE_EVENT_HAS_TAG                 attach a tag to an event
     * */
@org.apache.avro.specific.AvroGenerated
public enum EdgeType {
  EDGE_EVENT_AFFECTS_MEMORY, EDGE_EVENT_AFFECTS_FILE, EDGE_EVENT_AFFECTS_NETFLOW, EDGE_EVENT_AFFECTS_SUBJECT, EDGE_EVENT_AFFECTS_SRCSINK, EDGE_EVENT_HASPARENT_EVENT, EDGE_EVENT_CAUSES_EVENT, EDGE_EVENT_ISGENERATEDBY_SUBJECT, EDGE_SUBJECT_AFFECTS_EVENT, EDGE_SUBJECT_HASPARENT_SUBJECT, EDGE_SUBJECT_HASLOCALPRINCIPAL, EDGE_SUBJECT_RUNSON, EDGE_FILE_AFFECTS_EVENT, EDGE_NETFLOW_AFFECTS_EVENT, EDGE_MEMORY_AFFECTS_EVENT, EDGE_SRCSINK_AFFECTS_EVENT, EDGE_OBJECT_PREV_VERSION, EDGE_FILE_HAS_TAG, EDGE_NETFLOW_HAS_TAG, EDGE_MEMORY_HAS_TAG, EDGE_SRCSINK_HAS_TAG, EDGE_SUBJECT_HAS_TAG, EDGE_EVENT_HAS_TAG, EDGE_EVENT_AFFECTS_REGISTRYKEY, EDGE_REGISTRYKEY_AFFECTS_EVENT, EDGE_REGISTRYKEY_HAS_TAG  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"EdgeType\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* The different types of simple edges in the CDM\\n     * EDGE_EVENT_AFFECTS_MEMORY          an event affects a memory object (such as updating a memory location)\\n     *   EDGE_EVENT_AFFECTS_FILE            an event affects a file object (such as writing to a file)\\n     * EDGE_EVENT_AFFECTS_REGISTRYKEY     an event affects a registry value (such as writing to a registry key)\\n     *   EDGE_EVENT_AFFECTS_NETFLOW         an event affects a netflow object (such as writing to socket)\\n     * EDGE_EVENT_AFFECTS_SUBJECT         an event affects a subject (such as forking a process)\\n     *   EDGE_EVENT_AFFECTS_SRCSINK         an event affects a generic src/sink object\\n     *   EDGE_EVENT_HASPARENT_EVENT         a metaevent that represents a set of child atomic events\\n     *   EDGE_EVENT_CAUSES_EVENT an event caused/triggered another event\\n     * EDGE_EVENT_ISGENERATEDBY_SUBJECT   an event is generated by a subject (every event is)\\n     *   EDGE_SUBJECT_AFFECTS_EVENT         a subject affects an event (such as when killing a process)\\n     *   EDGE_SUBJECT_HASPARENT_SUBJECT a subject has a parent subject (such as thread has parent process)\\n     * EDGE_SUBJECT_HASPRINCIPAL          a subject has a principal (such as a process owned by a user)\\n     *   EDGE_SUBJECT_RUNSON                a subject runs on a host (TODO: host not yet modeled)\\n     *   EDGE_FILE_AFFECTS_EVENT an event reads from a file\\n     *   EDGE_REGISTRYKEY_AFFECTS_EVENT     an event reads values from a registry key\\n     *   EDGE_NETFLOW_AFFECTS_EVENT         an event reads from a network flow\\n     *   EDGE_MEMORY_AFFECTS_EVENT          an event reads from a memory object\\n     *   EDGE_SRCSINK_AFFECTS_EVENT         a generic source/sink object affects an event\\n     *   EDGE_OBJECT_PREV_VERSION the previous version of an object, typically used for file versioning\\n     * EDGE_FILE_HAS_TAG                  attach a tag to a file object\\n     * EDGE_REGISTRYKEY_HAS_TAG           attach a tag to a registrykey object\\n     * EDGE_NETFLOW_HAS_TAG               attach a tag to a netflow object\\n     * EDGE_MEMORY_HAS_TAG                attach a tag to a memory object\\n     * EDGE_SRCSINK_HAS_TAG               attach a tag to a srcsink object\\n     * EDGE_SUBJECT_HAS_TAG               attach a tag to a subject\\n     * EDGE_EVENT_HAS_TAG                 attach a tag to an event\\n     *\",\"symbols\":[\"EDGE_EVENT_AFFECTS_MEMORY\",\"EDGE_EVENT_AFFECTS_FILE\",\"EDGE_EVENT_AFFECTS_NETFLOW\",\"EDGE_EVENT_AFFECTS_SUBJECT\",\"EDGE_EVENT_AFFECTS_SRCSINK\",\"EDGE_EVENT_HASPARENT_EVENT\",\"EDGE_EVENT_CAUSES_EVENT\",\"EDGE_EVENT_ISGENERATEDBY_SUBJECT\",\"EDGE_SUBJECT_AFFECTS_EVENT\",\"EDGE_SUBJECT_HASPARENT_SUBJECT\",\"EDGE_SUBJECT_HASLOCALPRINCIPAL\",\"EDGE_SUBJECT_RUNSON\",\"EDGE_FILE_AFFECTS_EVENT\",\"EDGE_NETFLOW_AFFECTS_EVENT\",\"EDGE_MEMORY_AFFECTS_EVENT\",\"EDGE_SRCSINK_AFFECTS_EVENT\",\"EDGE_OBJECT_PREV_VERSION\",\"EDGE_FILE_HAS_TAG\",\"EDGE_NETFLOW_HAS_TAG\",\"EDGE_MEMORY_HAS_TAG\",\"EDGE_SRCSINK_HAS_TAG\",\"EDGE_SUBJECT_HAS_TAG\",\"EDGE_EVENT_HAS_TAG\",\"EDGE_EVENT_AFFECTS_REGISTRYKEY\",\"EDGE_REGISTRYKEY_AFFECTS_EVENT\",\"EDGE_REGISTRYKEY_HAS_TAG\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}