#!/bin/bash
set -e

# XXX check on java 1.8.0, sudo access, and be explicit about assuming we're in adapt.git

# Constants
ADAPT_DIR=$HOME/Adapt
USER_BIN=$HOME/.local/bin
TEMP=/tmp

GREMLIN_SERVER_DIR=gremlin-server
KAFKA_DIR=kafka

mkdir -p $USER_BIN

# Tools: git, stack, pip, tmux, wget
sudo apt-get update
sudo apt-get -y install git python python-pip tmux wget autotools-dev unzip libgmp-dev supervisor zlib1g-dev curl jq
if [ ! \( -e $USER_BIN/stack || -e $TEMP/stack.tar.gz \) ] ; then
	wget https://www.stackage.org/stack/linux-x86_64 -O $TEMP/stack.tar.gz
	tar xzf $TEMP/stack.tar.gz -C $TEMP
fi
[ -e $USER_BIN/stack ] || cp $TEMP/stack*/stack $USER_BIN

# Python Libraries
sudo pip install kafka-python protobuf==2.5.0

# Infrastructure Programs (Titan, Gremlin, Zookeeper, kafka)
if [ ! \( -e $GREMLIN_SERVER_DIR || -e $TEMP/gremlin.zip \) ] ; then
	wget http://www.eu.apache.org/dist/incubator/tinkerpop/3.1.0-incubating/apache-gremlin-server-3.1.0-incubating-bin.zip -O $TEMP/gremlin.zip
	unzip $TEMP/gremlin.zip
fi
[ -e $GREMLIN_SERVER_DIR ] || mv apache-gremlin-server-3.1.0-incubating $GREMLIN_SERVER_DIR

# Use stack to install ghc
$USER_BIN/stack setup

if [ ! \( -e $KAFKA_DIR || -e $TEMP/kafka.tgz \) ] ; then
	wget http://www.us.apache.org/dist/kafka/0.8.2.2/kafka_2.11-0.8.2.2.tgz -O $TEMP/kafka.tgz
	tar xzf $TEMP/kafka.tgz -C .
fi
[ -e $KAFKA_DIR ] || mv kafka_2.11-0.8.2.2 $KAFKA_DIR

# Download Adapt code
# This script should be from the Adapt.git repository, thus we already have the adapt code in cwd
# git clone git@github.com:GaloisInc/Adapt.git $ADAPT_DIR

# Compile all Adapt code
export PATH=$PATH:$USER_BIN
tar xJf example/infoleak-small.provn.tar.xz -C ./example
make build
