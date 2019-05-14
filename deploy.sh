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

SSH_AUTH=""
FOLDER=""
DELETEME="persistence-multimap_by_event.db"
KILLALL="NO"
SKIPASSEMBLY="NO"
OUTSTUFF=/dev/null

JVM_OPTS=()
JVM_OPTS[pi1]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"
JVM_OPTS[pi2]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"
JVM_OPTS[pi3]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"
JVM_OPTS[pi4]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"
JVM_OPTS[pi5]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"
JVM_OPTS[pi6]="-Xmx800M -Xloggc:gc.log -XX:+PrintGC -XX:+PrintGCTimeStamps"

# Options
while getopts "hi:ksd:v" OPT; do
  case $OPT in
    h)  usage $PROG_NAME ;;
    i)  SSH_AUTH="-i$OPTARG" ;;
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
  echo -n "Running 'killall java' on the nodes..."
  for NODENUM in ${MACHINES_INVOLVED[@]}; do
    ssh $NODENUM "killall java; rm $DELETEME; true"         &> $OUTSTUFF
  done
  echo " Done."
fi

# Build JAR
if [ "NO" = "$SKIPASSEMBLY" ]; then
  echo "Rebuilding the JAR..."
  SBT_OPTS=-Xss4m sbt 'set assemblyJarName := "../../quine-adapt.jar"' assembly
fi

# Deploy and run one each node
for NODENUM in ${MACHINES_INVOLVED[@]}; do
  THIS_CONF="$FOLDER/$NODENUM.conf"
  if [ -f $THIS_CONF ]; then
    echo -n "Deploying to $NODENUM (with $THIS_CONF)..."

    # Copy the conf file and JAR and to the machine
    scp $SSH_AUTH $THIS_CONF $NODENUM:~/$FOLDER.conf        &> $OUTSTUFF
    scp $SSH_AUTH quine-adapt.jar $NODENUM:~                &> $OUTSTUFF

    # Run the JAR on the node
    NODECMD="./vvm_java.sh $(echo ${JVM_OPTS[$NODENUM]} | sed 's/\</-/g') -Dconfig.file=$FOLDER.conf -jar ./quine-adapt.jar"
    ssh $SSH_AUTH gw.tc.bbn.com "$NODECMD"                  &> $OUTSTUFF

    echo " Done."
  fi
done

# TODO SSH TUNNELS
