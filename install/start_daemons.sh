#! /bin/sh

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

# If we are using vagrant then don't change to the adapt directory,
# otherwise we can use this same script to stand up a machine though a git
# clone to $HOME/adapt, so assume that's the case.
if [ $USER != "vagrant" -a \( -e $HOME/adapt \) ]
then
    ADAPT=$HOME/adapt
else
    ADAPT=$HOME
fi

supercfg=$ADAPT/config/supervisord.conf

cd $ADAPT || exit 1

# run supervisord (zookeeper, kafka, gremlin)
pgrep supervisord > /dev/null || (set -x; supervisord -c $supercfg; echo Started.)

# kafka and zookeeper are frustratingly slow and some of the helper
# scripts do not fail or retry well.
sleep 4
pstree -u | grep java

# This takes about one second of CPU if no building is needed.
(cd $ADAPT/ingest && make)

echo Ready to populate the Titan DB, e.g. with:
echo 'tar xJf example/infoleak*.tar.xz && Trint -u infoleak.provn'
