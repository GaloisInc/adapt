# Adapt

### Prerequistites

You will need to have:
  - a recent version of the Oracle [JDK] version 8 (http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Note: `openjdk` will throw strange errors; use the Oracle JDK. Not tested with JDK 9 yet.)
  - the latest version of [SBT](http://www.scala-sbt.org/) installed and on your path

Then, you just run

    $ sbt run          # run from the top level of the project directory


Once the system has started, you can open up the interactive UI at <http://localhost:8080/> or start querying the REST api directly.

### Command Line Options
SBT will allow you to choose the maximum ram to allocate (default is 1 GB).
Specify how much RAM to use with the `-mem` flag followed by a number in megabytes. e.g.: `sbt -mem 6000 run`

The adapt system is configurable at runtime by using the following command-line flags. Each flag should be preceded 
with `-D` and followed by a equals sign, then a value; no spaces. For example: `-Dadapt.runflow=db`

Example:

    sbt -mem 6000 -Dadapt.runflow=db -Dadapt.ingest.loadfiles.0=/Users/ryan/Desktop/ta1-cadets-pandex-cdm17.bin run

#### `-Dadapt.X` Flags

High-level commands about the primary operations of the system

| Command Line Flag | Possible Values                      | Default Value | Description |
| ----------------- |:------------------------------------:|:-------------:|:------------|
| adapt.runflow     | `ui` `db` `anomaly` `csv` `combined` | `ui`          | `ui` only starts the UI<br />`db` will run an ingest (must specify files)<br /> `anomaly` will run all the suspicioun score calculations (requires lots of RAM)<br />`csv` will ingest CDM and write it out to multiple CSV files (must specify `loadfiles`)<br />`combined` will run `ui` `db` and `anomaly` simultaneously (as we did in engagement 2)|
| adapt.app         | `prod` `accept`                      | `prod`        | previous multiple versions have been combined into `prod`. `accept` is for the acceptance testing app. |


#### `-Dadapt.ingest.X` Flags
| Command Line Flag            | Possible Values             | Default Value                                                     | Description |
| ---------------------------- |:---------------------------:|:-----------------------------------------------------------------:|:------------|
| adapt.ingest.loadfiles.0     | any full path to a CDM file | A hardcoded file path which probably doesn't apply on your system | The file at this path  will be ingested if the relevant `runflow` option is set. Multiple files can be specified by incrementing the number at the end of this flag. E.g.: `loadfiles.0`, `loadfiles.1`, `loadfiles.2`, etc. |
| adapt.ingest.startatoffset   | Any integer                 | `0`                                                               | Ingest will begin after skipping this many records in the specified file or kafka queue |
| adapt.ingest.loadlimit       | Any Integer                 | `0` (no limit)                                                    | Ingest will stop after ingesting this many. Zero means no limit. |
| adapt.ingest.quitafteringest | `yes` `no`                  | `yes`                                                             | The program will exit after ingesting all the data specified. Only applicable when ingesting from a file. |
| adapt.ingest.produceadm      | `yes` `no`                  | `yes`                                                             | Ingested CDM data will be tranformed in ADM ("Adapt data model") and the ADM data written into the database. Note: This CAN be used together with `producecdm` |
| adapt.ingest.producecdm      | `yes` `no`                  | `no`                                                              | Ingested CDM data will be written directly into the database structured as it is in the incoming CDM data; entity resolution will not be applied to this data. Note: This CAN be used together with `produceadm` |


#### `-Dadapt.runtime.X` Flags
| Command Line Flag                          | Possible Values               | Default Value             | Description |
| ------------------------------------------ |:-----------------------------:|:-------------------------:|:------------|
| adapt.runtime.apitimeout                   | Any Integer                   | `301`                     | Number of seconds before a query from the UI should be abandoned. |
| adapt.runtime.port                         | Any valid port number         | `8080`                    | The UI will run at `localhost` on this port. |
| adapt.runtime.dbkeyspace                   | Any string                    | `neo4j`                   | Ingested data will be written to this namespace. Changing this to other values will allow ingesting data from many TA1s into the same database, but will keep each namespace effectively separate. Some sane choices for othe namespaces might be: `cadets-pandex`, `cadets-bovia`, `trace-experiment-1`, `trace-experiment-2`, etc. |
| adapt.runtime.systemname                   | Any string without spaces     | `Engagement2+`            | The name of this system. |
| adapt.runtime.notesfile                    | Path to a file on this system | `/home/darpa/notes.json`  | Path to where either an existing notes JSON file resides, or where a new one should be created. The notes file stores the human-provided scores and comments for items shown in the prioritied list of cards |
| adapt.runtime.iforestpath                  | Path to a file on this system | `/home/darpa/iforest.exe` | Path to the iforest executable built for the system this is running on. |
| adapt.runtime.iforestparallelism           | Any Integer                   | `4`                       | Number of parallel iforest instances to run for anomaly detection. |
| adapt.runtime.shouldnoramlizeanomalyscores | Boolean                       | `false`                   | should rows in the CSV fed to iforest be normalized first? |


#### `-Dadapt.env.X` Flags
| Command Line Flag            | Possible Values                                                                   | Default Value                                    | Description |
| ---------------------------- |:---------------------------------------------------------------------------------:|:------------------------------------------------:|:------------|
| adapt.env.ta1                | `cadets` `clearscope` `faros` `fivedirections` `theia` `trace` `kafkaTest` `file` | `file`                                           | Defines which TA1 is the source of the data. This affects several other settings and behaviors. It is only relevant during the engagement. You do not need to set this for ingesting a file. |
| adapt.env.scenario           | `bovia` `pandex`                                                                  | `pandex`                                         | Defines which scenario the data is for. This affects several other settings and behaviors. It is only relevant during the engagement. You do not need to set this for ingesting a file. |
| adapt.env.ta1kafkatopic      | Any string                                                                        | `ta1-{adapt.env.ta1}-{adapt.env.scenario}-cdm17` | Defines which kafka topic to read from in an engagement environment. |
| adapt.env.theiaresponsetopic | Any String                                                                        | `ta1-theia-{adapt.env.scenario}-qr`              | Defines which kafka topic to read from if the TA1 is Theia (who provides provenance information on demand via a different channel) |
| adapt.env.kafkabootstrap     | String defining address and port of a Kafka broker; in Kafka's expected syntax    | `ta3-starc-adapt-1-tcip.tc.bbn.com:9092`         | Kafka configuration used during the engagement |


### Gremlin queries

Due to several reasons (see below), we don't support the full Gremlin language. Instead, we support a decent-sized subset of it. Almost all existing Gremlin queries are still valid queries in our new DSL. The most important limitations are:

  - since this is a DSL and not Gremlin, you can't put arbitrary Groovy/Java inside your queries
  - building on the last point, anonymous functions (including queries that use `it`) / closures aren't supported
  - to make parsing easy, anonymous graph traversals (things like `g.V().and(has('key1'),in())`) need to be made explicit using the `__` syntax (also documented [here][1]). The previous query should now look like `g.V().and(__.has('key1'),__.in())`.
  - whitespace is completely unimportant
  - queries using regular expressions are no longer supported. (They _were_ supported under the old Titan-backed system.)

The full grammar of queries supported is documented in `Query.scala` (and is updated whenever features are added). The functions of these mirror the functionality of the correspondingly named functions in the Tinkerpop [`Graph`][0], [`__`][1], and [`GraphTraversal`][2].

#### Examples

Here are some examples of valid queries:

    g.V().has(label,'Subject').has('anomalyScore').order().by(_.values('anomalyScore').max(),decr).limit(20)`
  
    g.V().has(label,'Entity-File').has('url', 'file:///tmp/zqxf1').has('file-version',1).dedup()

    g.V().as('parent').union(_.values('commandLine').as('cmd'),
                             _.until(_.in().has(label,'EDGE_EVENT_ISGENERATEDBY_SUBJECT')
                                      .in().has('eventType',10).count().is(0))
                              .repeat(_.in().has(label,'EDGE_EVENT_ISGENERATEDBY_SUBJECT')
                                      .in().has('eventType',10).out().has(label,'EDGE_EVENT_AFFECTS_SUBJECT')
                                      .out().has(label,'Subject').has('subjectType',0))
                              .values('commandLine').as('cmd'))
                      .select('parent').dedup().values('pid').as('parent_pid').select('parent_pid','cmd')"

#### Querying the REST API

After you've started up the system using `sbt run` (passing in whatever options you may need) and all data has been ingested, you can query the loaded data via a REST API. Depending on the type of result you expect back, POST to one of

  * for querying vertices <http://localhost:8080/query/nodes>
  * for querying edges <http://localhost:8080/query/edges>
  * for any other raw query <http://localhost:8080/query/generic>
  
your string query with the key `"query"`. You'll get back an appropriate JSON string reponse for the first two of these, and a list of raw strings for the third. In Python, for the query `g.V().limit(10)`, that could look like

```python
>>> import requests
>>> requests.post('http://localhost:8080/query/nodes', data={'query': 'g.V().limit(10)'}).json
[{'type': 'vertex', 'id': 0, 'label': 'Principal', 'properties': {'source': [{'id': 3, 'value': 'SOURCE_FREEBSD_DTRACE_CADETS'}], 'uuid': [{'id': 1, 'value':  # output snipped
```

#### Why not just use Gremlin
   
This DSL abstraction exists because:

  - We need some way of going from strings to queries. The Groovy reflection based Gremlin engine wasn't working, so we decided to roll our own DSL, then interpret that using the Java tinkerpop API.
  
  - The Java datatypes for this (mostly under `tinkerpop.gremlin.process.traversal.dsl.graph`) are unpleasant to handle. For instance, they tie traversals immediately to a graph.
  
  - This could be a good first-class representation of a mutation to execute on a graph (for example to add vertices or edges), but without being opaque.

#### Bugs / Missing features

Obviously we don't support all of the functionality we could. However, if you see anything in the Java Tinkerpop APIs ([`Graph`][0], [`__`][1], and [`GraphTraversal`][2]) that you would like to use, open up an issue or/and email <atheriault@galois.com> about it. Even if it is something that isn't straightforard, open an issue and we can discuss it.

[0]: http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/structure/Graph.html
[1]: http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/process/traversal/dsl/graph/__.html
[2]: http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/process/traversal/dsl/graph/GraphTraversal.html
