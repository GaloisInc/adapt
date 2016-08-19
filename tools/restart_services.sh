#!/bin/sh

# Sends a signal to supervisord that indicates it should re-read the
# configuration file and kill or restart daemons as necessary given the delta.
sudo /opt/titan/bin/titan.sh stop
if [ "$1" = "--clean" ]; then
	sudo /opt/titan/bin/titan.sh clean
fi
sudo /opt/titan/bin/titan.sh start
killall -HUP supervisord
