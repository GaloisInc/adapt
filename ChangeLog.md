# Combined Revision history for Ingest, IngestDaemon, GremlinClient

## 0.4 -- Unreleased, running changes please add any items.
* More CDM13->Adapt Schema fixes.
* stack.yaml updates for avro library.
* Do not add empty 'properties' maps to the database

## 0.3 -- 2016-06-23
* Use the 'avro' library to handle avro decoding, schema resolution and container objects.
  - This is pulled in as a subtree till we can opensource
* Switch to CDM13
* Make the internal schema userId and gid text instead of int.
* Parse _but silently drop_ RegistryKeyObject's from FiveDirections
    - No technical reason, it's just a time constraint till we think on the
      schema and actually add it.
* More schema type specifications for Segments.

## 0.2 -- 2016-06-10

* Increased the verbosity, sending more data to the Kafka log topic.
* Fix a mis-labeled eventType property (was "subjectType")

## 0.1  -- 2016-06-02

* First version. Released on an unsuspecting world.
  - Ingest: Parse CMD10 into a Haskell representation of Adapt's schema
  - GremlinClient: Wrapper easing websocket interface to the Titan database.
  - IngestDaemon: Daemon that manages the 4 kafka queues, database connection,
    plumbs the data around as needed.
* Supports CDM 10, expect this to change to CDM 12 or 13 or 14...
* Schema uses Strings instead of Java dates for times, expect this to change to Date.
