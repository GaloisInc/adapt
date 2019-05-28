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
  echo "  -s         : Skip assembly."
  echo "  -v         : Be verbose." 
  echo "  -h         : Display this message."
  exit
}

BBN_AUTH=""
FOLDER=""
DELETEME="/data/persistence-multimap_by_event.db"
KILLALL="NO"
SKIPASSEMBLY="NO"
OUTSTUFF=/dev/null

BBN_JVM_OPTS=()
BBN_JVM_OPTS[1]="-XX:+UseConcMarkSweepGC -Xmx140G -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 161195
BBN_JVM_OPTS[2]="-XX:+UseConcMarkSweepGC -Xmx140G -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 161195
BBN_JVM_OPTS[3]="-XX:+UseConcMarkSweepGC -Xmx140G -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 161195
BBN_JVM_OPTS[4]="-XX:+UseConcMarkSweepGC -Xmx140G -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 161195
BBN_JVM_OPTS[5]="-XX:+UseConcMarkSweepGC -Xmx24G  -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 28142
BBN_JVM_OPTS[6]="-XX:+UseConcMarkSweepGC -Xmx24G  -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 28142
BBN_JVM_OPTS[7]="-XX:+UseConcMarkSweepGC -Xmx140G -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"  # Total mem: 161195

# Options
while getopts "hi:ksd:v" OPT; do
  case $OPT in
    h)  usage $PROG_NAME ;;
    i)  BBN_AUTH="-i$OPTARG" ;;
    k)  KILLALL="YES" ;;
    s)  SKIPASSEMBLY="YES" ;;
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

# Get machines involved
MACHINES_INVOLVED=()
for FILE in $FOLDER/*; do
  MACHINES_INVOLVED+=( $(basename $FILE | cut -d. -f1) )
done

# Delete already running instances
if [ "YES" = "$KILLALL" ]; then
  echo -n "Running 'killall java' on the BBN machines..."
  for NODENUM in ${MACHINES_INVOLVED[@]}; do
    ssh $BBN_AUTH gw.tc.bbn.com "ssh $NODENUM 'killall java; rm $DELETEME; true'" &> $OUTSTUFF
  done
  echo " Done."
fi

# Build JAR an upload it to gateway server
if [ "NO" = "$SKIPASSEMBLY" ]; then
  echo "Rebuilding and uploading the JAR..."
  SBT_OPTS=-Xss4m sbt 'set assemblyJarName := "../../quine-adapt.jar"' assembly
  scp $BBN_AUTH quine-adapt.jar gw.tc.bbn.com:~/quine-adapt.jar
fi

# Deploy and run one each node
for NODENUM in ${MACHINES_INVOLVED[@]}; do
  THIS_CONF="$FOLDER/$NODENUM.conf"
  if [ -f $THIS_CONF ]; then
    echo -n "Deploying to BBN $NODENUM (with $THIS_CONF)..."
    
    # Copy the conf file and to the gateway server, then to the actual machine
    scp $BBN_AUTH $THIS_CONF gw.tc.bbn.com:~/$FOLDER.conf                  &> $OUTSTUFF
    ssh $BBN_AUTH gw.tc.bbn.com "scp $FOLDER.conf $NODENUM:~/$FOLDER.conf" &> $OUTSTUFF

    # Copy and run the JAR on the node
    NODECMD="./vvm_java.sh $(echo ${BBN_JVM_OPTS[$NODENUM]} | sed 's/\</-/g') -Dconfig.file=/home/darpa/$FOLDER.conf -jar ./quine-adapt.jar"
    ssh $BBN_AUTH gw.tc.bbn.com "scp quine-adapt.jar $NODENUM:~"           &> $OUTSTUFF
    ssh $BBN_AUTH gw.tc.bbn.com "ssh $NODENUM '$NODECMD'"                  &> $OUTSTUFF

    echo " Done."
  fi
done

# TODO SSH TUNNELS
