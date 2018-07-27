#!/bin/bash

if [ -d jar ]; then
   PRE='./'    # release version
   DEV=0
else
   PRE='../'   # development version
   DEV=1
fi

#############################################################################
## this is the user config part of the script
#############################################################################

BE_NICE=1
AUTO_MEM=1
DEBUG=0

# You can add your machine(s) to the script below
# if you want to allow Java to use more RAM.
source ${PRE}common/assets/machines.sh

#############################################################################
## OK, you can stop here
#############################################################################

PROJECT_NAME='preproc'
PREFIX='fr.loria.coronsys.preproc.transformer'

if [ -f jar/$PROJECT_NAME-bin.jar ]; then
   PG=''
else
   PG='pg-'
fi
#
if [ $BE_NICE = "1" ]; then
   NICE='nice -n 19'
else
   NICE=''
fi
#
export OLD_CLASSPATH=$CLASSPATH
if [ $DEV = "1" ]; then
   CLASSPATH=$CLASSPATH:"./bin"
   CLASSPATH=$CLASSPATH:"../coron/bin"
else
   CLASSPATH=$CLASSPATH:"jar/$PROJECT_NAME-${PG}bin.jar"
   CLASSPATH=$CLASSPATH:"jar/coron-${PG}bin.jar"
fi
#
CLASSPATH=$CLASSPATH:"${PRE}common/vendor/lib/junit-4.4.jar"
CLASSPATH=$CLASSPATH:"${PRE}common/vendor/lib/args4j-2.0.10.jar"

if [ "$OSTYPE" = "cygwin" ]; then
   CLASSPATH=`echo $CLASSPATH | sed -e "s/:/;/g"`
fi
export CLASSPATH
#
if [ "$JAVA_MAX_HEAP_SIZE" = "DEFAULT" ]; then
   JAVA_MAX_HEAP_SWITCH=''
else
   JAVA_MAX_HEAP_SWITCH="-Xmx${JAVA_MAX_HEAP_SIZE}"
fi
#
if [ $DEBUG = "0" ]; then
   $NICE  java  $JAVA_MAX_HEAP_SWITCH  $PREFIX.Main  "$@"
else
   echo  $NICE  java  $JAVA_MAX_HEAP_SWITCH  $PREFIX.Main  "$@"
fi
#
export CLASSPATH=$OLD_CLASSPATH

