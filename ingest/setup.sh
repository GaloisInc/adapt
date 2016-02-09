#! /usr/bin/env bash

if [ `id -u` != 0 ]; then echo Please run as root: sudo -H $0; exit 1; fi

if echo "$HOME" | grep root > /dev/null; then :; else echo Please use -H with sudo.; exit 1; fi

apt-get install -y aide graphviz python3-nose

CONF=/etc/aide/aide.conf
if ! egrep '^Checksums = sha256\+crc32$' $CONF  > /dev/null
then
    echo Please replace Checksums line in $CONF with this text:
    echo 'Checksums = sha256+crc32'
    exit 1
fi

DIR=`pwd`
cat > /etc/cron.d/aide-daily <<EOF

# Ingest relies on fresh FS reports being in /var/lib/aide.
30 23  * * * root  $DIR/daily_aide_report.sh
EOF

pip3 install coverage flake8 graphviz gremlinrestclient
