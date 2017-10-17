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

The following entity resolution occurs

  * `UnitDependency` records are merged into their closest non-unit subject ancestor
  * `Subject` process records get their `cmdLine` field from fork events if they don't have that
    information already
  * `Netflow` records are deduplicated based on the local IP, global IP, and port
  * `FileObject` records are merged by their path, type, and local principal. Filepath information
    found on some `Event` records is moved onto the `FileObject`.
  * `Event` records that come one after another, apply to the same object, and are the same, are
    merged.

  [0]: package.scala
