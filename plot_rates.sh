#!/bin/bash

set -e
if [ $# -ne 1 ]; then
  echo "Expected one file argument: $0 LOGFILE"
  exit 1
fi

# Arguments
LOGFILE=$1
rate_csv=$(mktemp)
avg_csv=$(mktemp)

rate_jpeg=${LOGFILE%.*}"_rate.jpeg"
avg_jpeg=${LOGFILE%.*}"_avg_rate.jpeg"

# Plots
cat $LOGFILE | grep -oe 'Rate: \d*'                 | cut -c 7-  > $rate_csv
cat $LOGFILE | grep -oe 'Rate since beginning: \d*' | cut -c 23- > $avg_csv
gnuplot -e "set terminal jpeg; set title 'ingest rate';     plot '"$rate_csv"'" > $rate_jpeg 
gnuplot -e "set terminal jpeg; set title 'avg ingest rate'; plot '"$avg_csv"'"  > $avg_jpeg
echo "Wrote out ingest rate plot '"$rate_jpeg"' and average ingest rate plot '"$avg_jpeg"'"

# Clean up 
rm $rate_csv
rm $avg_csv
