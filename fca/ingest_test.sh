#!/bin/bash

mem=$1
port=$2
dbkeyspace=$3
loadfiles=$4


sbt -mem $mem -Dadapt.runflow=db -Dadapt.ingest.quitafteringest=yes -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace -Dadapt.ingest.loadfiles.0=$4 run
sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
sleep 30 && cd explore/ && python3 query_test.py --port $2
