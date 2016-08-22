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

AVRO=~/adapt/trace/current/simple_with_marker_2.avro
SEG="./adapt_segmenter.py --broker ws://localhost:8182/ --criterion pid --radius 2 --store-segment Yes --spec /local/home/jhanley/adapt/config/segmentByPID.json --log-to-kafka --kafka localhost:9092"

set -e -x
killall ingestd
~/adapt/tools/delete_nodes.py 
~/adapt/trace/trace_rsync.sh
ls -lL $AVRO
Trint -p $AVRO
sleep 10; Trint -f; sleep 10
(cd ~/adapt/segment/segmenter && time $SEG)
time ~/adapt/classifier/phase3/fg_classifier.py
~/adapt/tools/label_count.py
