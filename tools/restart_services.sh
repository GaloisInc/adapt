#!/bin/sh

# Sends a signal to supervisord that indicates it should re-read the
# configuration file and kill or restart daemons as necessary given the delta.
/opt/titan/bin/titan.sh stop
/opt/titan/bin/titan.sh start
killall -HUP supervisord
