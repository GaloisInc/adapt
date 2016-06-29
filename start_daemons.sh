#! /usr/bin/env bash

# Starts daemons: titan, supervisord, zookeeper, kafka, Adapt components

cd /tmp

jps | grep GremlinServer > /dev/null || /opt/titan/bin/titan.sh start

# If supervisord is already running we avoid re-running.
#
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
(cd $ADAPT/.stack-adapt && stack install)

# Some applications over allocate and expect an allocate-on-use behavior as
# typical of Linux VMM.  For this to work we need to enable over-allocation
# on the VM.
sudo sysctl -w vm.overcommit_memory=1

# run supervisord (zookeeper, kafka, Adapt components)
pgrep supervisord > /dev/null || (set -x; supervisord -c $supercfg; sleep 5; echo Started.)

# Setup the Kafka Topics for our internal (adapt components only) kafka instance
KAFKA=/opt/kafka/bin/

TOPICS="ta2 in-finished ac ad dx pe se ui ac-log ad-log dx-log in-log pe-log se-log "
for TOPIC in $TOPICS
do
    QUERYRES=`$KAFKA/kafka-topics.sh --topic $TOPIC --describe --zookeeper localhost:2181`
    if [ -z "$QUERYRES" ] ; then
        $KAFKA/kafka-topics.sh --create --topic $TOPIC --zookeeper localhost:2181 --partitions 1 --replication-factor 1
    fi
done

# To halt daemons, use:
#   /opt/titan/bin/titan.sh stop;  killall supervisord
