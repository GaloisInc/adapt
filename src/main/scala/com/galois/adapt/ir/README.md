IR is intended as an internal format of CDM. Compared to CDM, it has several advantages: 

  * there are fewer the extraneous fields (CDM is verbose)
  * we perform entity resolution
  * we can apply provider specific rules to normalize the data

To the effect of the first point, the entire definition of the IR type [lives in one file][0]. There
are currently only a handful of types 

  * `IrEvent`
  * `IrSubject`
  * `IrPrincipal`
  * `IrFileObject`
  * `IrProvenanceTagNode`
  * `IrNetflowObject`
  * `IrSrcSinkObject`

Entity resolution occurs still in a streaming fashion. Since related records can be recieved with
arbitrarily large time-delays between them, ER is pessimistic. The measure of when to "give up"
holding onto information is usually in the form of `Tick`'s, which are emitted at regular time
intervals.

Holding onto records for longer (by increasing the `Tick` delay) means we can potentially perform
more entity resolution, but it also means we are must hold onto more state and a greate delay
between when we receive records and when we start doing any sort of APT detection on that
information.

# Details

The following entity resolution occurs

  * `Subject`
      - Subjects of type `SUBJECT_UNIT` are merged into their closest non-unit subject ancestor
      - Subjects collect a set of `cmdLines` from
          + the `cmdLine` field on the subject
          + an event which has this subject and with a `cmdLine` field
          + an event which has this subject and with a `exec` field
          + in the `name` field which can exist on the subjects property map
      - Synthesize parent subject edge (suggestion coming from Daniel)
      - If a subject has a `cmdLine` that indicates it corresponds to one of several common
        interpreters (Python, Ruby, ...), for every `EVENT_READ` attached to this subject
        an `EVENT_EXECUTE` is synthesized

  * `Event`
      - Sequences of events with the same subject and predicate objects are merged into groups of
          + `EVENT_WRITE` and `EVENT_LSEEK`
          + `EVENT_RECVFROM` and `EVENT_RECVMSG`
          + `EVENT_SENDMSG`
      - `EVENT_OPEN` and `EVENT_CLOSE` are ignored, except as boundaries between groups of events

  * `FileObject`
      - File objects look for path information on the `predicateFileObjectPath` field of events
        that reference them as their `predicateObject` and collect all such information into a set
      - File objects are deduplicated based on their path (if they have one), type, and local
        principal

  * `Netflow`
      - Netflows are deduplicated based on the local/global IP and local/global port
      
  * `RegistryKeyObject`
      - Converted into files (discard the `value`, use the `key` as the path)

  [0]: package.scala
