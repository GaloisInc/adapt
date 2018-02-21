#!/bin/bash

PARAMS=()
SLEEP=30

while (( "$#" )); do
  case "$1" in
    -i|--ingest)
      LOAD=$2
      shift 2
      ;;
    -p|--port)
      PORT=$2
      shift 2
      ;;
    -m|--mem)
      MEM=$2
      shift 2
      ;;
    -db|--dbkeyspace)
      DB=$2
      shift 2
      ;;
    --) # end argument parsing
      shift
      break
      ;;
    -*|--*=) # unsupported flags
      echo "Error: Unsupported flag $1" >&2
      exit 1
      ;;
    *) # preserve positional arguments
      #PARAM="$PARAMS $1"
      PARAM+=($1)
      shift
      ;;
  esac
done

# set positional arguments in their proper place
eval set -- "$PARAMS"
echo 'PARAM' $PARAM

#mem=${PARAM[0]}
#port=${PARAM[1]}
#dbkeyspace=${PARAM[2]}
if [[ -z "$MEM" ]] ; then mem=8000; else mem=$MEM; fi
if [[ -z "$PORT" ]] ; then port=8080; else port=$PORT; fi
if [[ -z "$DB" ]] ; then dbkeyspace="neo4j"; else dbkeyspace=$DB; fi
#mem=$MEM
#port=$PORT
#dbkeyspace=$DB
#echo 'mem' $mem
#echo 'port' $port
#echo 'dbkeyspace' $dbkeyspace
#echo 'loadfiles' $loadfiles

if [[ -z "$LOAD" ]] 
	then
		sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
		sleep $SLEEP && cd explore/ && python3 query_test.py -p $port
	else
		loadfiles=$LOAD
	    sbt -mem $mem -Dadapt.runflow=db -Dadapt.ingest.quitafteringest=yes -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace -Dadapt.ingest.loadfiles.0=$loadfiles run
		sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
		sleep $SLEEP && cd explore/ && python3 query_test.py -p $port
fi
