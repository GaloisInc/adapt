#!/bin/bash
echo '\n ;;;;;;;;;;;;;;;;;;;;;;; \n'

if [ -d jar ]; then
   PRE='./'    # release version
   DEV=0
else
   PRE='./'   # development version
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
if [ $AUTO_MEM = "1" ]; then
   case "$HOSTNAME" in
      "BARRACUDA"            )   JAVA_MAX_HEAP_SIZE=16384m;;
      "coutures"             )   JAVA_MAX_HEAP_SIZE=16384m;;
      "debrecen"             )   JAVA_MAX_HEAP_SIZE=16384m;;
      "hagrid.loria.fr"      )   JAVA_MAX_HEAP_SIZE=16384m;;
      "hoth"                 )   JAVA_MAX_HEAP_SIZE=16384m;;
      "kraken"	    	        )   JAVA_MAX_HEAP_SIZE=16384m;;
      "servbioinfo.loria.fr" )   JAVA_MAX_HEAP_SIZE=16384m;;
      *                      )   JAVA_MAX_HEAP_SIZE=16384m;;
   esac
else
   JAVA_MAX_HEAP_SIZE=8192m
fi
#############################################################################
## OK, you can stop here
#############################################################################

PROJECT_NAME='assrulex'
PREFIX='fr.loria.coronsys'

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
   $NICE  java  $JAVA_MAX_HEAP_SWITCH  $PREFIX.$PROJECT_NAME.Main  "$@"
else
   echo  $NICE  java  $JAVA_MAX_HEAP_SWITCH  $PREFIX.$PROJECT_NAME.Main  "$@"
fi
export CLASSPATH=$OLD_CLASSPATH

