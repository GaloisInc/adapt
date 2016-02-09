#! /usr/bin/env bash

PROG=$1
test -x $PROG || exit 1
set -x

coverage erase &&
coverage run $PROG &&
coverage report --include=$PROG -m
