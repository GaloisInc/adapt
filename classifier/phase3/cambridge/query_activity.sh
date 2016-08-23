#! /usr/bin/env bash

# Displays results from a run of test_interpreter.py.

make /tmp/test_interpreter.py
cd ~/adapt
time /tmp/test_interpreter.py 2>&1 \
    --data=~/adapt/trace/current/ta5attack2_units.avro
#   --data=~/adapt/trace/current/cameragrab1.bin
#   --data=~/adapt/trace/current/5d_youtube_ie_output-100.avro

time ~/adapt/tools/gremlin.sh 2>&1 <<EOF
graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();

g.V().count()

g.V().hasLabel('Segment').has('pid', 5141).out().groupCount().by(label)

g.V().hasLabel('Activity').count()

g.V().hasLabel('Activity').out().groupCount().by(label)

g.E().hasLabel('activity:includes').count()

g.E().hasLabel('activity:includes').inV().label().groupCount()

g.V().hasLabel('Subject').has('eventType').groupCount().by('eventType')

g.V().hasLabel('Subject').has('subjectType').groupCount().by('subjectType')
EOF
echo

cat > /dev/null <<EOF
cameragrab1:
g.V().hasLabel('Segment').has('segment:name', 's146661651612025305573').out('segment:includes').
hasLabel('Subject').has('subjectType', 4).as('a').
values('startedAtTime').as('startedAtTime').select('a').values('eventType').as('eventType').select('startedAtTime', 'eventType')
EOF
