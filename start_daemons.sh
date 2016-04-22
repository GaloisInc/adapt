#! /bin/bash

echo "############# TC-IN-A-BOX Integretaed daemon execution system #################"

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
fi

ADAPT=$HOME/adapt
supercfg=$ADAPT/config/supervisord.conf

cd $ADAPT || exit 1

# This takes about one second of CPU if no building is needed.
(cd $ADAPT/ingest && make)

# run supervisord (zookeeper, kafka, titan, ingestd)
pgrep supervisord > /dev/null || (set -x; supervisord -c $supercfg; echo Started.)

# Setup the Kafka Topics for our internal (adapt components only) kafka instance
KAFKA=/opt/kafka/bin/

TOPICS="in-finished px se ad ac dx ui in-log px-log se-log ad-log ac-log dx-log"
CURR_TOPICS=`$KAFKA/kafka-topics.sh --list --zookeeper localhost:2181`

for TOPIC_NAME in $TOPICS ; do
    if [ -z `echo "$CURR_TOPICS" | grep "$TOPIC_NAME"` ]
    then
        $KAFKA/kafka-topics.sh --create --topic $TOPIC_NAME --zookeeper localhost:2181 --partitions 1 --replication-factor 1
    fi
done
