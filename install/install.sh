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

get_if_hash_differs() {
    SOURCE=$1
    DEST=$2
    VALID_HASH=$3
    SUDO=$4
    CURRENT_HASH=''
    if [ -e $DEST ] ; then
        CURRENT_HASH="`sha256sum $DEST`"
    fi
    if [ ! -e $DEST ] || { [ -z "`echo $CURRENT_HASH | egrep \"^$VALID_HASH\"`" ]; } ; then
        $SUDO rm -f $DEST || handle_error $LINENO
        $SUDO wget $SOURCE -O $DEST || handle_error $LINENO
    fi
    if [ ! -z $VALID_HASH ] ; then
        sha256sum $DEST | egrep "^$VALID_HASH" > /dev/null || handle_error $LINENO
    fi
}

install_kafka() {
    KAFKA_VER=$1
    SCALA_VER=$2
    VALID_HASH=$3
    SOURCE=http://apache.mirrors.pair.com/kafka/${KAFKA_VER}/kafka_${SCALA_VER}-${KAFKA_VER}.tgz

    CWD=$(pwd)
    cd $KAFKA_ROOT || handle_error $LINENO
    get_if_hash_differs "$SOURCE" "$KAFKA_ROOT/kafka_${SCALA_VER}-${KAFKA_VER}.tgz" $VALID_HASH sudo
    sudo tar xvzf kafka_${SCALA_VER}-${KAFKA_VER}.tgz || handle_error $LINENO
    sudo ln -fsn /opt/kafka_${SCALA_VER}-${KAFKA_VER} /opt/kafka || handle_error $LINENO
    sudo chown --recursive vagrant:vagrant /opt/kafka
    sudo chmod g+w /opt/kafka
    cd ${CWD} || handle_error $LINENO
}

remove_titan() {
    sudo rm -rf $TITAN_SERVER_DIR || handle_error $LINENO
}
install_titan() {
    CWD=$(pwd)
    cd /opt || handle_error $LINENO
    GRZIP=/opt/titan.zip
    GOODHASH='67538e231db5be75821b40dd026bafd0cd7451cdd7e225a2dc31e124471bb8ef'
    SOURCE=http://s3.thinkaurelius.com/downloads/titan/titan-1.0.0-hadoop1.zip

    get_if_hash_differs $SOURCE $GRZIP $GOODHASH sudo || handle_error $LINENO
    sudo rm -rf $TITAN_SERVER_DIR || handle_error $LINENO
    rm -rf /opt/titan-1.0.0-hadoop1 || handle_error $LINENO
    sudo unzip $GRZIP || handle_error $LINENO
    sudo mv titan-1.0.0-hadoop1 $TITAN_SERVER_DIR || handle_error $LINENO
    sudo chown --recursive vagrant:vagrant $TITAN_SERVER_DIR
    sudo chmod g+w $TITAN_SERVER_DIR
    cd $CONFIG_DIR/titan || handle_error $LINENO
    sudo $ADAPT_DIR/tools/GenerateSchema.py || handle_error $LINENO
    sudo chown --recursive vagrant:vagrant $TITAN_SERVER_DIR
    cd $CWD || handle_error $LINENO
}

ensure_vagrant_user() {
    # post-condition:  a vagrant userid shall appear in /etc/passwd
    egrep '^vagrant:' /etc/group  > /dev/null || sudo addgroup vagrant
    egrep '^vagrant:' /etc/passwd > /dev/null || sudo adduser vagrant --disabled-password --gecos "" --ingroup vagrant
}

# Install zookeeper, kafka, titan, java, git, python, stack, ghc, etc.
install_adapt_dependencies() {
    USER_BIN=$HOME/.local/bin
    TEMP=$ADAPT_DIR/tmp

    KAFKAVER='0.9.0.1'
    SCALAVER='2.11'
    KAFKA_HASH='db28f4d5a9327711013c26632baed8e905ce2f304df89a345f25a6dfca966c7a'

    mkdir -p $TEMP || handle_error $LINENO
    mkdir -p $USER_BIN || handle_error $LINENO
    ret=`echo $PATH | grep "$USER_BIN" ; true`
    if [ -z $ret ]
    then
        echo "export PATH=\$PATH:$USER_BIN" >> $HOME/.bashrc || handle_error $LINENO
    fi

    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 \
                     --recv-keys 575159689BEFB442 || handle_error $LINENO
    echo 'deb http://download.fpcomplete.com/ubuntu trusty main' | sudo tee \
                /etc/apt/sources.list.d/fpco.list || handle_error $LINENO
    sudo add-apt-repository ppa:webupd8team/java || handle_error $LINENO
    sudo apt-get update || handle_error $LINENO
    echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 boolean true" \
                | sudo debconf-set-selections || handle_error $LINENO
    sudo apt-get install -y \
         exuberant-ctags \
         git \
         jq \
         python \
         python-pip \
         python3-nose \
         python3-setuptools \
         stack \
         supervisor \
         unzip \
         wget \
         graphviz \
       || handle_error $LINENO
    sudo apt-get install -y oracle-java8-installer || echo $'tries = 100\ntimeout = 5000' | sudo tee -a /var/cache/oracle-jdk8-installer/wgetrc && sudo apt-get install -y oracle-java8-installer || handle_error $LINENO
    sudo -H easy_install3 pip || handle_error $LINENO
    sudo -H pip2 install \
        avroknife \
        gremlinclient \
        kafka-python \
        tornado \
      || handle_error $LINENO
      sudo -H pip3 install \
          Arpeggio \
          avroknife \
          aiogremlin \
          coverage \
          dnspython \
          flake8 \
          gremlinrestclient \
          networkx \
          pydot \
          numpy \
          sklearn \
          scipy \
          parsley \
          pyparsing \
          flask \
      || handle_error $LINENO

    sudo -H pip3 install -r $ADAPT_DIR/dx/simulator/config/requirements.txt

    sudo rm -f /etc/rc?.d/S20supervisor || handle_error $LINENO

    stack setup || handle_error $LINENO

    ensure_vagrant_user
    install_kafka $KAFKAVER $SCALAVER $KAFKA_HASH || handle_error $LINENO
    install_titan
    if [ -e $CONFIG_DIR/titan ] ; then
        sudo cp -r $CONFIG_DIR/titan/* $TITAN_SERVER_DIR/ || handle_error $LINENO
    fi
    sudo chown vagrant:vagrant /opt/* || handle_error $LINENO

    if [ -z "$USE_TC_IN_A_BOX_CONFIG" ] ; then
        (cd ~/adapt/config && test -r supervisord.conf || ln -s supervisord.conf.adaptinabox supervisord.conf)
    else
        (cd ~/adapt/config && test -r supervisord.conf || ln -s supervisord.conf.tcinabox supervisord.conf)
    fi
}

function install_ingest_dashboard() {
    CWD=$(pwd)
    # Install ingest and dashboard using a single sandbox
    mkdir -p $ADAPT_DIR/.stack-adapt                          || handle_error $LINENO
    cp $ADAPT_DIR/install/stack.yaml $ADAPT_DIR/.stack-adapt/ || handle_error $LINENO
    cd $ADAPT_DIR/.stack-adapt                                || handle_error $LINENO
    stack install                                             || handle_error $LINENO
    cd $CWD
}

function install_ad() {
    CWD=$(pwd)
    cd $ADAPT_DIR/ad/osu_iforest || handle_error $LINENO
    make                         || handle_error $LINENO
    cd $CWD
}

function install_supervisor_config() {
    # Copy over supervisor configuration files
    ln -sf $CONFIG_DIR/supervisord.conf.adaptinabox $CONFIG_DIR/supervisord.conf || handle_error $LINENO
}

function install_adapt() {
    # Installs: In, PE, Se, AD, AC,DX
    export PATH=$PATH:$HOME/.local/bin

    install_ingest_dashboard
    install_ad
    install_supervisor_config
}

mkdir -p $KAFKA_ROOT

install_adapt_dependencies
install_adapt
