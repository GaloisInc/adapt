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

if [ `hostname` != zhangang ]
then
    echo Running on unusual Monitored Host. Proceding...
fi
sudo rm -f /tmp/simple_apt.cdm
echo /tmp/simple 13.1.102.2 4000
set -ex

sudo auditctl -a exit,always -F arch=b64 -S socket -S bind -S listen -S accept
sudo auditctl -l
sudo auditctl -b 8000

sudo spade start
exit 0
sleep 4
# list all
# add storage CDM output=/tmp/simple_apt.cdm
# add reporter Audit arch=64 units=false fileIO=true netIO=true
# list all

# add filter OPM2ProvTC position=1
# add storage Prov output=$PWD/$1 tc=http://spade.csl.sri.com/rdf/audit-tc.rdfs
spade control <<EOF
list all
EOF
