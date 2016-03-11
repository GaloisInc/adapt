#! /usr/bin/env bash

set -e -x

cd ~/adapt/trace
rsync -av adapt@seaside.galois.com:trace/ .
