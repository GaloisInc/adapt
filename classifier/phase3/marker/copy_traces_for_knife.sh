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

#
# usage:
#     ./copy_traces_for_knife.sh
#     avroknife tojson local:/tmp/knife/apt1 | wc -l  # grep...
#


DIRS="
 apt1
 cameragrab1
 gather.avro
 imagegrab2
 imagegrab
 infoleak_small_units
 recordaudio1
 recorder
 screengrab1
 screengrab
 ta5attack1_units
 ta5attack2_units
 youtube_ie_update
 simple_with_marker_1
 simple_with_marker_2
 simple_with_marker_3
"

for DIR in $DIRS
do
    DEST=/tmp/knife/$DIR
    mkdir -p $DEST
    for FILE in ~/adapt/trace/current/${DIR}.{avro,bin}
    do
	test -r $FILE && cp -p $FILE $DEST/
    done
done
du -sk /tmp/knife/
