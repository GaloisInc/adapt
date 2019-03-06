#!/bin/bash
set -e

# Usage
PROG_NAME=$0
usage ()
{
  echo "Usage: $0 [-h] [-i ssh-key] folder"
  echo "  -i path    : Use this public key for autheticating to 'gw.tc.bbn.com'"
  echo "  -k         : Kill all other java processes before starting this up"
  echo "  -d path    : File to clear before running quine-adapt"
  echo "  -v         : Be verbose." 
  echo "  -h         : Display this message."
  exit
}

BBN_AUTH=""
FOLDER=""
DELETEME="persistence-multimap_by_event.db"
KILLALL="NO"
OUTSTUFF=/dev/null

# Options
while getopts "hi:kd:v" OPT; do
  case $OPT in
    h)  usage $PROG_NAME ;;
    i)  BBN_AUTH="-i$OPTARG" ;;
    k)  KILLALL="YES" ;;
    d)  DELETEME=$OPTARG ;;
    v)  OUTSTUFF=/dev/stdout ;;
    \?) echo "Invalid option: -$OPTARG" >&2; usage $PROG_NAME ;;
  esac
done
shift $(expr $OPTIND - 1)

# Arguments
if [[ $# -ne 1 ]]; then
  echo "Illegal number of parameters: $# (expected 1 - the folder containing configs)" >&2
  usage $PROG_NAME
elif [ ! -d $1 ]; then
  echo "Argument '$1' does not refer to a folder" >&2
  usage $PROG_NAME
else
  FOLDER=$1
fi

# Delete already running instances
if [ "YES" = "$KILLALL" ]; then
  echo -n "Running 'killall java' on the BBN machines..."
  for NODENUM in {1..3}; do
    ssh $BBN_AUTH gw.tc.bbn.com "ssh $NODENUM 'killall java; true'"        &> $OUTSTUFF
  done
  echo " Done."
fi

# Build JAR an upload it to gateway server
SBT_OPTS=-Xss4m sbt 'set assemblyJarName := "../../quine-adapt.jar"' assembly
scp $BBN_AUTH quine-adapt.jar gw.tc.bbn.com:~/quine-adapt.jar

# Deploy and run one each node
for NODENUM in {1..3}; do
  THIS_CONF="$FOLDER/$NODENUM.conf"
  if [ -f $THIS_CONF ]; then
    echo -n "Deploying to BBN $NODENUM (with $THIS_CONF)..."
    
    # Copy the conf file and to the gateway server, then to the actual machine
    scp $BBN_AUTH $THIS_CONF gw.tc.bbn.com:~/$FOLDER.conf                  &> $OUTSTUFF
    ssh $BBN_AUTH gw.tc.bbn.com "scp $FOLDER.conf $NODENUM:~/$FOLDER.conf" &> $OUTSTUFF

    # Copy and run the JAR on the node
    NODECMD="./vvm_java.sh -Xmx30G -Dconfig.file=/home/darpa/$FOLDER.conf -jar ./quine-adapt.jar"
    ssh $BBN_AUTH gw.tc.bbn.com "scp quine-adapt.jar $NODENUM:~"           &> $OUTSTUFF
    ssh $BBN_AUTH gw.tc.bbn.com "ssh $NODENUM '$NODECMD'"                  &> $OUTSTUFF

    echo " Done."
  fi
done

# TODO SSH TUNNELS
