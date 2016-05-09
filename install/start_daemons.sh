#! /bin/bash

# Starts daemons: supervisord, zookeeper, kafka, gremlin

# This script is like start.sh,
# but for folks who prefer tail -f over tmux.
# Renaming it on top of start.sh would be fine.
#
# Also, if supervisord is already running it avoids re-running.
#
# c.f. the clause that starts supervisord in Adapt-classify/titan/Makefile

# apt-get wants root to run the daemon. We prefer to run it ourselves.
if pgrep -U root supervisord > /dev/null
then
    sudo service supervisor stop
    sleep 1
fi

ADAPT=$HOME/adapt
supercfg=$ADAPT/config/supervisord.conf

cd $ADAPT || exit 1

# This takes about one second of CPU if no building is needed.
(cd $ADAPT/ingest && make)

# run supervisord (zookeeper, kafka, gremlin)
pgrep supervisord > /dev/null || (set -x; supervisord -c $supercfg; echo Started.)

# kafka and zookeeper are frustratingly slow and some of the helper
# scripts do not fail or retry well.
sleep 4
pstree -u | grep java

# Setup the Kafka Topics
KAFKA=/opt/kafka/bin/
TOPICS="ta2 in-finished ac ad dx px se ui ac-log ad-log dx-log in-log px-log se-log "

# Avoid creating topic names that already exist.
declare -A CURR
for TOPIC in `$KAFKA/kafka-topics.sh --list --zookeeper localhost:2181`
do
    CURR[$TOPIC]=1
done
for TOPIC in $TOPICS ; do
    if [[ -z "${CURR[$TOPIC]}" ]]
    then
        echo "[start_daemons.sh] Creating topic: $TOPIC"
        $KAFKA/kafka-topics.sh --create --topic $TOPIC --zookeeper localhost:2181 --partitions 1 --replication-factor 1
    fi
done

echo '[start_daemons.sh] Ready to push data to ingestd, e.g. with:'
echo '[start_daemons.sh] Trint -p example/*.bin'
