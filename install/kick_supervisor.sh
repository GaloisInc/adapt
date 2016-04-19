#!/bin/sh

# Sends a signal to supervisord that indicates it should re-read the
# configuration file and kill or restart daemons as necessary given the delta.
kill -HUP `(ps -e | grep supervisord | cut -d " " -f 1)`
