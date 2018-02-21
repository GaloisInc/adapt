#!/bin/bash


PARAMS=()

while (( "$#" )); do
  case "$1" in
    -r|--search_repository)
      REP=$2
      if [[ -z "$REP" ]] ; then rep='.'; else rep=$REP; fi
      files=($(find $rep -name '*cadets-*cdm17.bin' -or -name '*trace-*cdm17.bin' -or -name '*five*cdm17.bin'))
      shift 2
      ;;
    -p|--port)
      PORT=$2
      shift 2
      ;;
    -i|--interval)
      INTERV=$2
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
#echo 'PARAM' $PARAM

#mem=${PARAM[0]}
#port=${PARAM[1]}
#dbkeyspace=${PARAM[2]}

if [[ -z "$PORT" ]] ; then port=8080; else port=$PORT; fi
if [[ -z "$INTERV" ]] ; then interval=10; else interval=$interv; fi


let "end_port=$port+2000"
ports=($(shuf -i $port-$end_port -n ${#files[@]}))
echo 'first file' ${files[0]}
echo 'third file' ${files[2]}
echo 'len files' ${#files[@]}
echo 'len ports' ${#ports[@]}
echo 'port 1' ${ports[0]}

#./ingest_script.sh -i cadets.bin -p 8090 -db cadets
#for
dbkeyspaces=()
fileindices=${!files[*]}
for i in $fileindices
	do
		#echo $i
		namef=${files[i]}
		#echo 'namef' $namef
		DB=${namef%-cdm17.bin}  # retain the part before the colon
		DB=${DB##*ta1-}
		DB=$(echo $DB | tr - _)  # retain the part after the last slash
		#echo 'DB' $DB
		dbkeyspaces+=($DB)
	done
	
for i in $fileindices
	do
		./ingest_script.sh -i ${files[i]} -p ${ports[i]} -db ${dbkeyspaces[i]}
	done

#echo 'dbkeyspaces[0]' ${dbkeyspaces[0]}
#echo 'dbkeyspaces[2]' ${dbkeyspaces[2]}
#echo 'len dbkeyspaces' ${#dbkeyspaces[@]}
