#! /usr/bin/env bash

if [ `id -u` != 0 ]; then echo Please run as root: sudo $0; exit 1; fi

apt-get install aide

DIR=`pwd`
cat > /etc/cron.d/aide-daily <<EOF

# Ingest relies on fresh FS reports being in /var/lib/aide.
30 23  * * * root  $DIR/daily_aide_report.sh
EOF
