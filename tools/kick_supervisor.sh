#!/bin/sh

# Sends a signal to supervisord that indicates it should re-read the
# configuration file and kill or restart daemons as necessary given the delta.
pkill java
killall -HUP supervisord
