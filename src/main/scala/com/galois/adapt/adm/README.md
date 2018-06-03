Link to the [ADM schema][1].

ADM (adapt data model) is intended as an internal format of CDM. Compared to CDM, 

  * there are fewer the extraneous fields (CDM is verbose)
  * we perform entity resolution
  * we can apply provider specific rules to normalize the data

To the effect of the first point, the entire definition of the ADM type [lives in one file][0]. There
are currently only a handful of types 

  * `AdmEvent`
  * `AdmSubject`
  * `AdmPrincipal`
  * `AdmFileObject` - `RegistryKeyObject`s are merged into this
  * `AdmProvenanceTagNode`
  * `AdmNetFlowObject`
  * `AdmSrcSinkObject`
  * `AdmPathNode` - This node type represents both file paths and command lines. The motivation for
     breaking paths/command-line into separate nodes is two-fold:
       - we can emit extra path/command-line information after we've emitted the object it refers to
       - we can do reverse lookups to see which files/processes share a path/command-line
       
Entity resolution occurs in a streaming fashion. Since related records can be received with
arbitrarily large time-delays between them, ER is somewhat pessimistic.

Since just about the whole pipeline is processed asynchronously, our pressure-value to regulate memory
usage and time-guarantees is the timeout for these computations. 

# Details

The following entity resolution occurs

  * `Subject`
      - Subjects that are _not_ processes are merged into their closest process ancestor
      - The `exec` field on some subject processes is moved onto an `AdmPathNode`

  * `Event`
      - Successive events having the same type and with the same subject and predicate objects are merged
      - <s>`EVENT_OPEN` and `EVENT_CLOSE` are ignored</s> (not yet implemented)
      - An `EVENT_FORK` with a `cmdLine` field produces an `AdmPathNode` with an edge between the path and process
      - Events with a `predicateObject` and `predicateObjectPath` produce an `AdmPathNode` with an edge
        to the predicate object. Ditto for `predicateObject2` / `predicateObject2Path`.

  * `FileObject`
      - The path information on some file objects is moved onto an `AdmPathNode`

  * `Netflow`
      - Netflows are deduplicated based on the local/global IP and local/global port
      
  * `RegistryKeyObject`
      - Converted into files (discard the `value`, use the `key` as the path)
      
# Future

Building off of the current system here are some things that are now possible, and may be useful:

  * adding edges to link nodes to other nodes that are temporally close to them  

  [0]: package.scala
  [1]: https://owncloud-tng.galois.com/index.php/s/I3tOvpXsnKjopce#pdfviewer
