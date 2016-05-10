#! /usr/bin/env bash

# Starts daemons: supervisord, zookeeper, kafka, titan, ingestd

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

# Some applications over allocate and expect an allocate-on-use behavior as
# typical of Linux VMM.  For this to work we need to enable over-allocation
# on the VM.
sudo sysctl -w vm.overcommit_memory=1

# run supervisord (zookeeper, kafka, titan, ingestd, dashboad)
pgrep supervisord > /dev/null || (set -x; supervisord -c $supercfg; sleep 5; echo Started.)

# Setup the Kafka Topics for our internal (adapt components only) kafka instance
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
        $KAFKA/kafka-topics.sh --create --topic $TOPIC --zookeeper localhost:2181 --partitions 1 --replication-factor 1
    fi
done
