#! /usr/bin/env bash

set -x
killall supervisord
/opt/titan/bin/titan.sh stop

netstat -tan|sort|egrep 'LISTEN'
jps
