#! /usr/bin/env bash

set -x
killall supervisord
/opt/titan/bin/titan.sh stop
# No need for this file to continue growing without bound.
mv /opt/titan/log/gremlin-server.log{,~}

netstat -tan|sort|egrep 'LISTEN'
jps
