#!/bin/bash


PARAMS=()
NB_JOBS=3

while (( "$#" )); do
  case "$1" in
    -r|--search_repository)
      REP=$2
      if [[ -z "$REP" ]] ; then rep='.'; else rep=$REP; fi
      files=($(find $rep -name '*cadets-*cdm17.bin' -or -name '*trace-*cdm17.bin' -or -name '*five*cdm17.bin'))
      shift 2
      ;;
    -p|--port)
		len="$#"
		 if [[ ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-n" ) || ($2 == "--context_name" ) || ($2 == "-rs") || ($2 == "--fca_rule_spec" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   PORT+=(8080)
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-n" ) || ($a == "--context_name" ) || ($a == "-rs") || ($a == "--fca_rule_spec" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								PORT+=($a)
							fi			
			done
			s=${#PORT[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
      #PORT=$2
      #shift 2
      #;;
    -N|--no_ingest)
      INGEST=0
      shift 1
      ;;
    #-i|--interval)
      #INTERV=$2
      #shift 2
      #;;
    -seq|--sequential_ingest)
      SEQ=1
      shift 1
      ;;
    -d|--context_directory)
      CONTEXT_DIR=$2
      shift 2
      ;;
    -m|--mem)
      MEM=$2
      shift 2
      ;;
    -w|--fca_workflow)
      FCA_WORKFLOW=$2
      shift 2
      ;;
    -ms|--fca_minsupp)
      FCA_MINSUPP=$2
      shift 2
      ;;
    -n|--context_name)
      CONTEXT_NAME=$2
      shift 2
      ;;
    -rs|--fca_rule_spec)
      FCA_RULE_SPEC=$2
      shift 2
      ;;
    -cd|--csv_dir)
      CSV_DIR=$2
      shift 2
      ;;
    -oa|--fca_analysis_output_dir)
      FCA_ANALYSIS_DIR=$2
      shift 2
      ;;
    --) # end argument parsing./fca/ingest_script.sh -p 8629 -m 4000 -db cadets_bovia -c n=ProcessEvent d=./fca/contextSpecFiles cp=./fca/csvContexts/cadets_bovia_ProcessEvent_1.csv p=8629 -fp w=both i=./fca/csvContexts/cadets_bovia_ProcessEvent_1.csv m=0.05 rs=./fca/rulesSpecs/rules_positive_implication.json oa=./fca/fcaAnalysis/cadets_bovia_ProcessEvent_1.txt
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

#if [[ -z "$PORT" ]] ; then port=8080; else port=$PORT; fi
if [[ -z "$MEM" ]] ; then mem=5000; else mem=$MEM; fi
if [[ -z "$INGEST" ]] ; then ingest=1; else ingest=$INGEST; fi
if [[ -z "$SEQ" ]] ; then seq=0; else seq=$SEQ; fi
if [[ -z "$FCA_WORKFLOW" ]] ; then workflow="both"; else workflow=$FCA_WORKFLOW; fi
if [[ -z "$FCA_MINSUPP" ]] ; then minsupp=0; else minsupp=$FCA_MINSUPP; fi
if [[ -z "$CONTEXT_DIR" ]] ; then context_dir='./fca/contextSpecFiles'; else context_dir=$CONTEXT_DIR; fi
if [[ -z "$FCA_RULE_SPEC" ]] ; then rule_spec='./fca/rulesSpecs/rules_positive_implication.json'; else rule_spec=$FCA_RULE_SPEC; fi
if [[ -z "$FCA_ANALYSIS_DIR" ]] ; then fca_analysis_dir='./fca/fcaAnalysis'; else fca_analysis_dir=$FCA_ANALYSIS_DIR; fi
if [[ -z "$CSV_DIR" ]] ; then csv_dir='./fca/csvContexts'; else csv_dir=$CSV_DIR; fi
if [[ -z "$CONTEXT_NAME" ]] ; then context_name='ProcessEvent'; else context_name=$CONTEXT_NAME; fi
#if [[ -z "$INTERV" ]] ; then interval=10; else interval=$interv; fi

if [[ ${#PORT[@]} -eq 0 ]] ; then port+=(8080); elif [[ ${#PORT[@]} -eq 1 ]] ; then port+=(${PORT[0]}) ; else port=${PORT[@]} ; fi

if [[ ${#PORT[@]} -le 1 ]];
 then
	let "end_port=$port+2000"
	ports=($(shuf -i $port-$end_port -n ${#files[@]}))
 else
	ports=${port[@]}
fi

echo 'ingest' $ingest
echo 'first file' ${files[0]}
echo 'third file' ${files[2]}
echo 'len files' ${#files[@]}
echo 'files' ${files[@]}
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
		PATH2CSV=$csv_dir'/'$DB'_'$context_name'_1.csv'
		if [[ -e $PATH2CSV ]]
		  then
			num=$(ls $csv_dir'/'$DB'_'$context_name* | sort -V | tail -1)
			num=${num##*_}
			num=$(( ${num%.csv}+1 ))
			PATH2CSV=$csv_dir'/'$DB'_'$context_name'_'$num'.csv'
		fi
		path2csv+=($PATH2CSV)
		ANALYSIS_OUTPUT=$fca_analysis_dir'/'$DB'_'$context_name'_1.txt'
		if [[ -e $ANALYSIS_OUTPUT ]]
		  then
			num_a=$(ls $fca_analysis_dir'/'$DB'_'$context_name* | sort -V | tail -1)
			num_a=${num_a##*_}
			num_a=$(( ${num_a%.txt}+1 ))
			ANALYSIS_OUTPUT=$fca_analysis_dir'/'$DB'_'$context_name'_'$num_a'.txt'
		fi
		analysis_output+=($ANALYSIS_OUTPUT)
	done


fca_run()
{
	port=$1
	mem=$2
	dbkeyspace=$3
	contextName='n='$4
	contextSpecDir='d='$5
	path2csv=$6
	workflow='w='$7
	minsupp='m='$8
	ruleSpec='rs='$9
	analysisOutput='oa='${10}
	loadfiles=${11}
	echo 'loadfiles' $loadfiles
	
	echo "running FCA"
	if [[ -z "$loadfiles" ]]
	  then
	     echo "./fca/ingest_script.sh -p $port -m $mem -db $dbkeyspace -c $contextName $contextSpecDir cp=$path2csv p=$port -fp $workflow i=$path2csv $minsupp $ruleSpec $analysisOutput"
	     ./fca/ingest_script.sh -p $port -m $mem -db $dbkeyspace -c $contextName $contextSpecDir cp=$path2csv p=$port -fp $workflow i=$path2csv $minsupp $ruleSpec $analysisOutput
    else
		echo "./fca/ingest_script.sh -i $loadfiles -p $port -m $mem -db $dbkeyspace -c $contextName $contextSpecDir cp=$path2csv p=$port -fp $workflow i=$path2csv $minsupp $ruleSpec $analysisOutput"
		./fca/ingest_script.sh -i $loadfiles -p $port -m $mem -db $dbkeyspace -c $contextName $contextSpecDir cp=$path2csv p=$port -fp $workflow i=$path2csv $minsupp $ruleSpec $analysisOutput
	fi
}

export -f fca_run
if [[ $seq -eq 1 ]]	
  then
        echo "sequential ingest"
		if [[ $ingest -ne 0 ]]
		  then
		    echo "running ingest"
			for i in $fileindices
				do
					./fca/ingest_script.sh -m $mem -i ${files[i]} -I -p ${ports[i]} -db ${dbkeyspaces[i]} 
				done
			echo "ingest done"
		fi
		echo "starting ui/fca pipeline"
		parallel --joblog ./log_ingestall.txt --jobs $NB_JOBS --xapply fca_run ::: ${ports[@]} ::: $mem ::: ${dbkeyspaces[@]} ::: $context_name ::: $context_dir ::: ${path2csv[@]} ::: $workflow ::: $minsupp ::: $rule_spec ::: ${analysis_output[@]}  
		#fca in parallel
  else
     echo "everything in parallel"
     echo 'ports' ${ports[@]}
     echo 'dbkeyspaces' ${dbkeyspaces[@]}
     echo 'path2csv' ${path2csv[@]}
     y=1000
     x=$(( mem/y ))
     if [[ $ingest -ne 0 ]]
        then
           echo "running full pipeline in parallel"
           
		   parallel --joblog ./log_ingestall.txt --jobs $NB_JOBS --xapply fca_run ::: ${ports[@]} ::: $mem ::: ${dbkeyspaces[@]} ::: $context_name ::: $context_dir ::: ${path2csv[@]} ::: $workflow ::: $minsupp ::: $rule_spec ::: ${analysis_output[@]} ::: ${files[@]} 
		else
			echo "starting parallelized ui/fca pipeline"
			parallel --joblog ./log_ingestall.txt --jobs $NB_JOBS --xapply fca_run ::: ${ports[@]} ::: $mem ::: ${dbkeyspaces[@]} ::: $context_name ::: $context_dir ::: ${path2csv[@]} ::: $workflow ::: $minsupp ::: $rule_spec ::: ${analysis_output[@]} 
	fi
fi



#echo 'dbkeyspaces[0]' ${dbkeyspaces[0]}
#echo 'dbkeyspaces[2]' ${dbkeyspaces[2]}
#echo 'len dbkeyspaces' ${#dbkeyspaces[@]}
