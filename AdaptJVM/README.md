# Adapt

### Prerequistites

You will need to have

  - a recent version of the [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - the latest version of [SBT](http://www.scala-sbt.org/)

Then, you will need to run

    $ sbt -mem 6000 run \
        -Dadapt.loadfiles.0=<PATH TO A DATA FILE> \
        -Dadapt.loadlimit=<MAXIMUM NUMBER OF EVENTS TO INGEST>

This should hopefully download all dependencies, compile everything, and start running it. You
should now see a rapidly increasing count of the number of ingested events. Once this is done, you
will see the message

        Server online at http://localhost:8080/

    Press RETURN to stop..

At this point you can open up the interactive UI at <http://localhost:8080/> or start querying the REST api directly. However, there are some restrictions on the queries you can make... 

### Gremlin queries

Due to several reasons (see below), we don't support the full Gremlin language. Instead, we support a decent-sized subset of it. Almost all existing Gremlin queries are still valid queries in our new DSL. The most important limitations are:

  - since this is a DSL and not Gremlin, you can't put arbitrary Groovy/Java inside your queries
  - building on the last point, anonymous functions (including queries that use `it`) / closures aren't supported
  - to make parsing easy, anonymous graph traversals (things like `g.V().and(has('key1'),in())`) need to be made explicit using the `__` syntax (also documented [here][1]). The previous query should now look like `g.V().and(__.has('key1'),__.in())`.
  - whitespace is completely unimportant

The full grammar of queries supported is documented in `Query.scala` (and is updated whenever features are added). The functions of these mirror the functionality of the correspondingly named functions in the Tinkerpop [`Graph`][0], [`__`][1], and [`GraphTraversal`][2].

#### Examples

Here are some examples of valid queries:

    g.V().has(label,'Subject').has('anomalyScore').order().by(_.values('anomalyScore').max(),decr).limit(20)`
  
    g.V().has(label,'Entity-File').has('url', regex('.*file:///tmp/zqxf1.*')).has('file-version',1).dedup()

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
