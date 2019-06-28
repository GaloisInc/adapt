# Adapt

### Prerequistites

You will need to have:
  - a recent version of the [Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) version 8 (Note: `openjdk` may throw strange errors. Not tested with JDK 9 yet.)
  - the latest version of [SBT](http://www.scala-sbt.org/) installed and on your path

Then, to compile and start up the system, run the following:

    $ sbt run          # executed from the top level of the project directory

Once the system has started, you can open up the interactive UI at <http://localhost:8080/> or start querying the REST api directly.

You may run into a `java.lang.StackOverflowError` during compilation. If that happens, just increase
the stack size. You can do this either by adding `export SBT_OPTS=-Xss4m` to your `.bashrc`, or by
manually overriding `SBT_OPTS` on every `sbt` invocation.

    $ SBT_OPTS=-Xss4m sbt run

### Command Line Options
SBT will allow you to choose the maximum ram to allocate (default is 1 GB).
Specify how much RAM to use with the `-mem` flag followed by a number in megabytes. e.g.: `sbt -mem 6000 run`

The adapt system is configurable at runtime with configuration files, which are in the [HOCON][4] format (a superset of
JSON). Then, you can specify the path to the configuration file using the `-Dconfig.file` command line flag. It is
recommended that you start from the sample configuration file 
at [`src/main/resources/application.conf`](src/main/resources/application.conf). The adapt-system options are all in the
`adapt` object. Within that object, you'll be warned about incorrect or misspelled options/values. 

Example:

    $ cp src/main/resources/application.conf myconfig.conf
    $ sbt -mem 6000 -Dconfig.file=myconfig.conf run

### Gremlin queries

The main way to make ad-hoc queries on the data is via Gremlin queries, issued either in the search bar at the top of UI served up on <http://localhost:8080/> or to the REPL at `./cmdline_query.py`.

Due to several reasons (see below), we don't support the full Gremlin language. Instead, we support a decent-sized subset of it. Almost all existing Gremlin queries are still valid queries in our new DSL. The most important limitations are:

  - since this is a DSL and not Gremlin, you can't put arbitrary Groovy/Java inside your queries
  - building on the last point, anonymous functions (including queries that use `it`) / closures aren't supported
  - whitespace is completely unimportant

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

[3]: https://github.com/lightbend/config/blob/master/HOCON.md#paths-as-keys
[4]: https://github.com/lightbend/config/blob/master/HOCON.md

