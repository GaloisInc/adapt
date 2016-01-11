#!/bin/sh

# Starts daemons: supervisord, zookeeper, kafka, gremlin

# This script is like start.sh,
# but for folks who prefer tail -f over tmux.
# Renaming it on top of start.sh would be fine.
#
# Also, if supervisord is already running it avoids re-running.

# apt-get wants root to run the daemon. We prefer to run it ourselves.
if pgrep -U root supervisord > /dev/null
then
    sudo service supervisor stop
    sleep 1
fi

ADAPT=$HOME/adapt
supercfg=$ADAPT/config/supervisord.conf

(set -x
 cd $ADAPT || exit 1)

# run supervisord (zookeeper, kafka, gremlin)
if pgrep supervisord > /dev/null
then
    exit 0  # Nothing to do, it's already running.
fi
(set -x
 supervisord -c $supercfg)

echo Started.
# kafka and zookeeper are frustratingly slow and some of the helper
# scripts do not fail or retry well.
sleep 5
