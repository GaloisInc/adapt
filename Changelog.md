# 0.3 -- In progress

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
