# 0.4
- Segmenter functions
- Require Trusty64 >= 06/21/2016
- Smarter testing / creation of Kafka topics

# 0.3.1 -- Minor point release, 2016-06-24
- Fixes for CDM13 to Adapt Schema translation
- Fixes for building ingester with new Avro library
- Add --clean to restart_services.sh
- Make the 'ident' index unique.
    - NB this is notionally different than making the schema 'ident' unique but
      with the same result to the end users.

# 0.3 -- Released 2016-06-23

- Move to CDM13, including schema changes
  - This impacts both ingestd and Trint
  - We should now be able to consume and translate between compatible CDM versions.
- Improved schema for segements
- TMD: I observed some DX work and segment work - feel free to update the
       changelog if you want that documented more completely.


# 0.2 -- Released 2016-06-10

- Add ElasticSearch to Titan config
- Add an index 'byURL' that uses elastic search for the 'url' property key.
- New 'stop the world' script
- Pull in Adapt-Ingest 0.2 for fixes.
- Changes that are not user-visible:
  * Modularize the adapt.groovy script

# 0.1

- Initial versioned release of adapt-in-a-box
- System services auto start:
  - Titan+cassandra+elasticsearch
      - Titan schema from Ingest 0.1
      - Indexing on 'ident'
  - Kafka starts
    - topics are created per SystemSpecification in AdaptMisc.git
  - Zookeeper starts (incidental, needed by Kafka)
- Auto-starting Adapt components include:
  - Ingestd
  - AD
  - dashboard
  - classifyd
  - segmentd
- Can ingest at ~300 statements per second on commodity hardware.
- Trint, avroknife utilities installed.
- Currently non-existent components include:
  - PX
  - DX
  - UI
