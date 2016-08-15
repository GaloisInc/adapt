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
#    ./build_instrumented_simple_apt.sh
#

DEST=/tmp/kudu/build
mkdir -p $DEST
cd $DEST
test -d TransparentComputing ||
    git clone git@github.com:kududyn/TransparentComputing.git
set -x
cd TransparentComputing/malware/cross-platform/simple-apt/simple/
(cd src && etags *.c linux/*.c)
git checkout -- src/{main,msg,tcp}.c
git status
cp          ~/adapt/classifier/phase3/marker/tc_marker.h    src/
patch -p5 < ~/adapt/classifier/phase3/marker/kudu_patch.txt
which cmake || sudo apt-get install -y cmake gcc-multilib g++-multilib
python cmake.py build
rsync -a  bin/linux/release/x64/simple /tmp/
rsync -av bin/linux/debug/x64/simple   /tmp/
