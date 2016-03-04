#!/bin/bash
set -e

# Constants
ADAPT_DIR=$HOME/Adapt
USER_BIN=$HOME/.local/bin
TEMP=/tmp

TITAN_SERVER_DIR=titan
KAFKA_DIR=kafka
CONFIG_DIR=$HOME/config

mkdir -p $USER_BIN
ret=`echo $PATH | grep "$USER_BIN" ; true`
if [ -z $ret ]
then
    echo "export PATH=\$PATH:$USER_BIN" >> $HOME/.bashrc
fi

# Verify we can run as root:
sudo id -u || exit 1

# Allow PPA's in Debian
sudo apt-get install build-essential man wget -y
sudo apt-get install strace tcpdump -y
sudo apt-get install libssl-dev zlib1g-dev libcurl3-dev libxslt-dev -y
sudo apt-get install software-properties-common python-software-properties -y
sudo apt-get install git -y

# Install Java 8
# sudo echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
# sudo add-apt-repository ppa:webupd8team/java -y
# sudo apt-get update -y
# sudo apt-get install oracle-java8-installer -y
# sudo apt-get install ant -y
sudo sh -c 'echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections'
sudo sh -c 'echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list ; echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list ; apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886'
sudo apt-get update -y
sudo apt-get install oracle-java8-installer -y

if [ -e /etc/rc2.d/S20supervisor ]
then
    # Adapt runs supervisor as ordinary user,
    # unlike a vanilla install which would run as root,
    # perhaps for the host to support unrelated services.
    echo fatal: a vanilla supervisor install is already present
    exit 1
fi

# Tools: git, stack, pip, tmux, wget
# Entries you might prefer not to use:
#   openjdk-8-jre-headless -- not available on trusty (use oracle ppa instead)
#   python-pip -- not advised if versionitis forced using "sudo easy_install pip"
which pip             || sudo apt-get -y install python-pip
sudo apt-get -y install \
    autotools-dev \
    curl \
    git \
    graphviz \
    jq \
    libgmp-dev \
    python \
    python3-setuptools \
    supervisor \
    tmux \
    unzip \
    wget \
    zlib1g-dev \

which pip3 || (sudo -H easy_install3 pip && test -x /usr/bin/pip && sudo rm -f /usr/local/bin/pip)
sudo -H pip3 install coverage flake8 graphviz gremlinrestclient

# We do not wish for root to launch a supervisor daemon upon reboot.
sudo rm -f /etc/rc?.d/S20supervisor

if [ ! \( -e $USER_BIN/stack -o -e $TEMP/stack.tar.gz \) ] ; then
	wget https://www.stackage.org/stack/linux-x86_64 -O $TEMP/stack.tar.gz
	tar xzf $TEMP/stack.tar.gz -C $TEMP
fi
[ -e $USER_BIN/stack ] || cp $TEMP/stack*/stack $USER_BIN

# Python Libraries
sudo -H pip install kafka-python # protobuf==2.5.0

# Infrastructure Programs (Titan, Gremlin, Zookeeper, kafka)
GRZIP=$TEMP/titan.zip
if [ ! -e $GRZIP ] ; then
	wget http://s3.thinkaurelius.com/downloads/titan/titan-1.0.0-hadoop1.zip -O $GRZIP
fi
if [ ! -e $TITAN_SERVER_DIR ] ; then
	sha256sum $GRZIP | egrep '^67538e231db5be75821b40dd026bafd0cd7451cdd7e225a2dc31e124471bb8ef' > /dev/null  # -e would bail on mismatch
	unzip   $GRZIP
	mv titan-1.0.0-hadoop1 $TITAN_SERVER_DIR
fi
if [ -e $CONFIG_DIR/titan ] ; then
    cp -r $CONFIG_DIR/titan $TITAN_SERVER_DIR
fi

# Use stack to install ghc
$USER_BIN/stack setup

if [ ! \( -e $KAFKA_DIR -o -e $TEMP/kafka.tgz \) ] ; then
	wget http://www.us.apache.org/dist/kafka/0.8.2.2/kafka_2.11-0.8.2.2.tgz -O $TEMP/kafka.tgz
	tar xzf $TEMP/kafka.tgz -C .
fi
[ -e $KAFKA_DIR ] || mv kafka_2.11-0.8.2.2 $KAFKA_DIR

# Download Adapt code
# This script should be from the Adapt.git repository, thus we already have the adapt code in cwd
# git clone git@github.com:GaloisInc/Adapt.git $ADAPT_DIR

# Compile all Adapt code
export PATH=$PATH:$USER_BIN
source bootstrap-ingest.sh
install_ingest
