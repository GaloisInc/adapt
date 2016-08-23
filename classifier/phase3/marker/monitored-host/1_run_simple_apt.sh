#! /usr/bin/env bash

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.
#

if [ `hostname` != hangang ]
then
    echo Running on unusual Monitored Host. Proceding...
fi

comma_to_S() {
    echo $1 | sed 's;,; -S ;g'
}


sudo rm -f /tmp/simple_apt.cdm
mkdir -p /tmp/adapt
echo sudo auditctl -D
echo /tmp/simple 13.1.102.2 4000
# sudo auditctl -a exit,always -F arch=b64 -S `comma_to_S write,close,open,socket,accept,bind,listen`
set -ex
sudo auditctl -D
sudo auditctl -b 8000

sudo spade start
sleep 4
sudo auditctl -l
# list all
# add storage CDM output=/tmp/simple_apt.cdm
# add reporter Audit arch=64 units=false fileIO=true netIO=true
# list all

# add filter OPM2ProvTC position=1
# add storage Prov output=$PWD/$1 tc=http://spade.csl.sri.com/rdf/audit-tc.rdfs
exit 0
spade control <<EOF
list all
EOF
