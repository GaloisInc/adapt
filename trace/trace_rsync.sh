#! /usr/bin/env bash

set -e -x

cd ~/adapt/trace
rsync -av \
      --exclude '*ttl.txt' \
      --exclude '*.dot' \
      --exclude '*.dot.gz' \
      --exclude '*.json.CDM.json' \
      --exclude '*.log' \
      --exclude '*.log.gz' \
      --exclude '*.provn' \
      --exclude '*.provn.gz' \
      --exclude '*.provn.ttl' \
      --exclude '*.raw' \
      --exclude '*.raw.pp_json' \
      --exclude '*.tar.gz' \
      --exclude '*.tgz' \
      --exclude '*.ttl' \
      --exclude '*.zip' \
      adapt@seaside.galois.com:trace/ .
