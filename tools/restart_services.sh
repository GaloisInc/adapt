#!/bin/sh

# Sends a signal to supervisord that indicates it should re-read the
# configuration file and kill or restart daemons as necessary given the delta.
pkill java
sleep 3
/opt/titan/bin/titan.sh start
killall -HUP supervisord
