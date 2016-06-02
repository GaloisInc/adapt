# Combined Revision history for Ingest, IngestDaemon, GremlinClient

## 0.2

* Increased the verbosity, sending more data to the Kafka log topic.

## 0.1  -- 2016-06-02

* First version. Released on an unsuspecting world.
  - Ingest: Parse CMD10 into a Haskell representation of Adapt's schema
  - GremlinClient: Wrapper easing websocket interface to the Titan database.
  - IngestDaemon: Daemon that manages the 4 kafka queues, database connection,
    plumbs the data around as needed.
* Supports CDM 10, expect this to change to CDM 12 or 13 or 14...
* Schema uses Strings instead of Java dates for times, expect this to change to Date.
