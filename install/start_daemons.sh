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
# TODO: Figure out a programatic way to _check_ for and _create_ topics
#       as necessary in each Adapt component.
KAFKA=$ADAPT/kafka/bin/

# Notice topics must not be sub-strings of one another for the below script
# to work.
TOPICS="ta2 pattern adaptDashboard ingestd-log px-log se-log ad-log ac-log dx-log"
CURR_TOPICS=`$KAFKA/kafka-topics.sh --list --zookeeper localhost:2181`

for TOPIC_NAME in $TOPICS ; do
    if [ -z `echo "$CURR_TOPICS" | grep "$TOPIC_NAME"` ]
    then
        $KAFKA/kafka-topics.sh --create --topic $TOPIC_NAME --zookeeper localhost:2181 --partitions 1 --replication-factor 1
    fi
done

echo Ready to populate the Titan DB, e.g. with:
echo 'Trint -u example/*.bin'
