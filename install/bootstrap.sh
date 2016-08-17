#! /usr/bin/env bash

KAFKA_ROOT=/opt
ADAPT_DIR=$HOME/adapt
CONFIG_DIR=$ADAPT_DIR/config
TITAN_SERVER_DIR=/opt/titan

handle_error() {
    lineno=$1
    if [ ! $FORCE ]; then
        echo "Failed to provision tc-in-a-box at line $lineno.  See the error above." >&2
        exit -1
    else
        echo "Ignoring tc-in-a-box failure at line $lineno due to force-provision flag."
    fi
}

function copy_adapt() {
    CWD=$(pwd)
    sudo apt-get update                         || handle_error $LINENO
    sudo apt-get install -y git                 || handle_error $LINENO
    hash -r                                     || handle_error $LINENO
    if [ -e $ADAPT_DIR ] ; then
        cd $ADAPT_DIR                           || handle_error $LINENO
        git fetch                               || handle_error $LINENO
        git checkout $(cd /vagrant ; git branch | grep '*' | awk '{print $2}') || handle_error $LINENO
        git pull                                || handle_error $LINENO
    else
        git clone file:///vagrant -b $(cd /vagrant ; git branch | grep '*' | awk '{print $2}') $ADAPT_DIR || handle_error $LINENO
    fi
    cd $CWD                                     || handle_error $LINENO
}

mkdir -p $KAFKA_ROOT

copy_adapt
$HOME/adapt/install/install.sh
