#!/bin/bash

# Global constants
ASSEMBLED_ADAPT="./target/scala-2.11/adapt-assembly-0.1.jar"
ASSEMBLED_SCEPTER="./scepter/target/scala-2.11/scepter-assembly-0.1.jar"

REMOTE="alfred:/srv/www/adapt.galois.com/public_html/acceptance_tests" #"adapt.galois.com~acceptance_tests"

# Usage
usage ()
{
  echo "Usage: $0 -hfs filename"
  echo "  -s (adapt|scepter) : Skip completely updating either of the JARs."
  echo "  -t file            : Downloads and tests 'adapter-tester.jar' with the provided file."
  echo "  -h                 : Display this message."
  exit
}

# Options
UPDATE_ADAPT=true
UPDATE_SCEPTER=true

while getopts "ht:s:" opt; do
  case $opt in
    h)  usage $0 ;;
    t)  TEST_FILE=$OPTARG ;;
    s)  case $OPTARG in
          adapt)   unset UPDATE_ADAPT ;;
          scepter) unset UPDATE_SCEPTER ;;
          *)       echo "Invalid argument to -s: $OPTARG" >&2 ;;
        esac ;;
    \?) echo "Invalid option: -$OPTARG" >&2; usage $0 ;;
  esac
done

# Re-build 'adapt.jar', upload it, compute its hash, and upload that too
if [ $UPDATE_ADAPT ]
then
  sbt clean
  sbt assembly && \
  scp $ASSEMBLED_ADAPT ${REMOTE}/adapt.jar && \
  md5 -q $ASSEMBLED_ADAPT > ./adapt.hash && \
  scp ./adapt.hash ${REMOTE}/current.hash
  rm ./adapt.hash 2> /dev/null
else
  echo "Skipping 'adapt.jar'"
fi

# Re-build 'adapt-tester.jar', upload it, compute its hash, and upload that too
if [ $UPDATE_SCEPTER ]
then
  sbt scepter/clean
  sbt scepter/assembly && \
  scp $ASSEMBLED_SCEPTER ${REMOTE}/adapt-tester.jar && \
  md5 -q $ASSEMBLED_SCEPTER > ./scepter.hash && \
  scp ./scepter.hash ${REMOTE}/current-tester.hash
  rm ./scepter.hash 2> /dev/null
else
  echo "Skipping 'adapt-tester.jar'"
fi

# Download the latest adapt-tester and try it
if [ !$TEST_FILE ]
then
  # Clean up stuff that may interfere
  rm ./adapt-tester.jar 2> /dev/null
  rm ./adapt.jar 2> /dev/null

  # Download and run the latest adapt-tester
  wget http://adapt.galois.com/adapt-tester.jar && \
  java -jar ./adapt-tester.jar $TEST_FILE #./Engagement1DataCDM13/pandex/ta1-clearscope-cdm13_pandex.bin
  rm ./adapt-tester.jar 2> /dev/null

else
  echo "No tests"
fi

echo "Done"

