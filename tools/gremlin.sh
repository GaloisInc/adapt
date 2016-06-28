#! /usr/bin/env bash

echo "graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();"
cd /tmp
exec /opt/titan/bin/gremlin.sh $@
