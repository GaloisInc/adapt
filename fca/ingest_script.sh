#!/bin/bash

PARAMS=()
SLEEP=30

while (( "$#" )); do
  case "$1" in
    -i|--ingest)
      LOAD=$2
      shift 2
      ;;
    -I|--ingest_only)
	  INGEST=1
      shift 1
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
    -f|--fca_only)
	  FCA=1
      shift 1
      ;;
    #-q|--query_input)
	  #QUERY=$2
      #shift 2
      #;;
    #-csv|--csv_input)
	  #CSV=$2
      #shift 2
      #;; 
    -g|--generation_only)
	  GEN=1
      shift 1
      ;;
    -c|--conversion)
      #takes a variable number of arguments (1 to 4 arguments). Argument 1: context name (e.g ProcessEvent). 
      #Argument 2: directory where context specification files are stored. 
      # Argument 3: Path to where the input CSV file should be stored 
      # Argument 4: query port
	  len="$#"
	  if [[ ($2 == "-fp") || ($2 == "--fca_params") || ($2 == "-i") || ($2 == "--ingest") || ($2 == "-p") || ($2 == "--port") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-db") || ($2 == "--dbkeysp2ce") || ($2 == "-f") || ($2 == "--fc2_only") || ($2 == "-g") || ($2 == "--gener2tion_only") || ($2 == "-q" ) || ($2 == "--query_input" ) || ($2 == "-csv") || ($2 == "--csv_input" ) ]]
		then
		   DEFAULT_VAL=1
		   shift 1
		else
		    DEFAULT_VAL=0
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-fp") || ($a == "--fca_params") || ($a == "-i") || ($a == "--ingest") || ($a == "-p") || ($a == "--port") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-db") || ($a == "--dbkeyspace") || ($a == "-f") || ($a == "--fca_only") || ($a == "-g") || ($a == "--generation_only") || ($a == "-q" ) || ($a == "--query_input" ) || ($a == "-csv") || ($a == "--csv_input" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								CONV+=($a)
							fi			
			done
			s=${#CONV[@]}
			#echo $s
			shift "$s"
	 fi
      ;; 
    -fp|--fca_params)
         #pass strings with the arguments to pass to the python fca script e.g -fp w=both i=cadets.csv m=0.05 p=8080 rs=implication.json o=concepts.txt oa=scores.csv
         # this would correspond to invoking python3 fcascript -w 'both' -i cadets.csv -m 0.05 -p 8080 -rs implication.json -o concepts.txt -oa scores.csv
         #FCA_PARAMS=$2
         len="$#"
		 if [[ ($2 == "-c") || ($2 == "--conversion") || ($2 == "-i") || ($2 == "--ingest") || ($2 == "-p") || ($2 == "--port") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-db") || ($2 == "--dbkeysp2ce") || ($2 == "-f") || ($2 == "--fc2_only") || ($2 == "-g") || ($2 == "--gener2tion_only") || ($2 == "-q" ) || ($2 == "--query_input" ) || ($2 == "-csv") || ($2 == "--csv_input" ) ]]
			then
		   DEFAULT_FCA_VAL=1
		   shift 1
		else
		    DEFAULT_FCA_VAL=0
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-c") || ($a == "--conversion") || ($a == "-i") || ($a == "--ingest") || ($a == "-p") || ($a == "--port") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-db") || ($a == "--dbkeyspace") || ($a == "-f") || ($a == "--fca_only") || ($a == "-g") || ($a == "--generation_only") || ($a == "-q" ) || ($a == "--query_input" ) || ($a == "-csv") || ($a == "--csv_input" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								FCA_PARAMS+=($a)
							fi			
			done
			s=${#FCA_PARAMS[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    --) # end argument parsing
      shift
      breakinput=$(grep -o -E "/i|s=.+\.json|csv|cxt|fimi" <<< $fca_params)
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
gen=$GEN
fca=$FCA
#query=$QUERY${array[@]}
#csv=$CSV
#query=$QUERY
default=$DEFAULT_VAL
#port_param=' p='
#fca_params=$FCA_PARAMS$port_param$port
if ! [[ " ${FCA_PARAMS[@]} " =~ p=[0-9]+  ]]; then FCA_PARAMS+=(p=$port) ; fi

if [[ $gen -eq 1 && $fca -eq 1 ]]
  then
		gen=0
		fca=0
		echo 'WARNING: Ignoring -f and -g flags and running the full pipeline (i.e input generation followed by FCA and rule mining)'
fi

if [[ -n "$LOAD" ]] 
	then
		loadfiles=$LOAD
	    sbt -mem $mem -Dadapt.runflow=db -Dadapt.ingest.quitafteringest=yes -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace -Dadapt.ingest.loadfiles.0=$loadfiles run
fi
pid=$(pgrep -f "sbt -mem [0-9]+ -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=9000 -Dadapt.runtime.dbkeyspace=neo4j run")
#echo 'pid' $pid

conversion(){
	#$1=$default $2=$port array=CONV
	default=$1
	#echo 'default' $default
	port=$2 && shift 2
	#echo 'port' $port
	conv_array=($@)
	l=${#conv_array[@]}
	#echo 'conv_array' ${conv_array[@]}
	conv=$(perl -le 'print join " ",@ARGV' "${conv_array[@]}")
	#echo 'conv' $conv
	new_port=$(grep -o -E "p=[0-9]+" <<< $conv)
	new_port=${new_port##*p=}
	#echo 'new port' $new_port
	context_name=$(grep -o -P "(n=)[^ ]+" <<< $conv)
	context_name=${context_name##*n=}
	#echo 'context name' $context_name
	context_path=$(grep -o -P "cp=.+?\.csv" <<< $conv)
	context_path=${context_path##*cp=}
	#echo 'context path' $context_path
	spec_directory=$(grep -o -P "(d=)[^ ]+" <<< $conv)
	spec_directory=${spec_directory##*d=}
	#echo 'spec directory' $spec_directory
	if [[ $default -eq 1 ]]
	  then
	      #echo "conversion with default values"
          python3 ./fca/conversion_script.py -p $port
	else./fca/ingest_script.sh -p 9000 -f -fp "w=both m=0.05 s=fca/contextSpecFiles/neo4jspec_ProcessEvent.json rs=fca/rulesSpecs/rules_implication_all.json" -m 4000
	   if [[ -z  "$new_port" && -z "$context_path" && -z "$spec_directory" && -n "$context_name" ]]
	       then
				#echo "python3 ./fca/conversion_script.py -p $port -n $context_name"
				python3 ./fca/conversion_script.py -p $port -n $context_name
	   elif [[ -z  "$new_port" && -n "$context_path" && -z "$spec_directory" && -n "$context_name" ]]
			then
			    #echo "python3 ./fca/conversion_script.py -p $port -n $context_name -d $spec_directory"
				python3 ./fca/conversion_script.py -p $port -n $context_name -d $spec_directory
	   elif [[ -z  "$new_port" && -n "$context_path" && -n "$spec_directory" && -n "$context_name" ]]
			then
			    #echo "python3 ./fca/conversion_script.py -p $port -n $context_name -cp $context_path -d  $spec_directory"
				python3 ./fca/conversion_script.py -p $port -n $context_name -cp $context_path -d  $spec_directory
	   elif [[ -n  "$new_port" && -n "$context_path" && -n "$spec_directory" && -n "$context_name" ]]
			then
			    #echo "python3 ./fca/conversion_script.py -p $new_port -n $context_name -d $spec_directory -cp $context_path"
				python3 ./fca/conversion_script.py -p $new_port -n $context_name -d $spec_directory -cp $contex./fca/ingest_script.sh -p 9000 -f -fp "w=both m=0.05 s=fca/contextSpecFiles/neo4jspec_ProcessEvent.json rs=fca/rulesSpecs/rules_implication_all.json" -m 4000t_path
	   else
			python3 ./fca/conversion_script.py -p $port
	   fi
	 fi
}

fca_pipeline(){
	 #The input to this function is a quoted string that contains the arguments to pass to the python fca script e.g "w=both i=cadets.csv m=0.05 p=8080 rs=implication.json o=concepts.txt oa=scores.csv"
     # this would correspond to invoking python3 fcascript -w 'both' -i cadets.csv -m 0.05 --port 8080 -rs implication.json -o concepts.txt -oa scores.csv
	fcaparams=($@)
	#echo 'fcaparams' ${fcaparams[@]}
	fca_params=$(perl -le 'print join " ",@ARGV' "${fcaparams[@]}")
	#echo 'FCA params' $fca_params
	w=$(grep -o -E "w=(both|fca|context|analysis)" <<< $fca_params)
	w=${w##*w=}
	#echo 'original workflow' $w
	rule_spec=$(grep -o -E "rs=.+\.json" <<< $fca_params)
	rule_spec=${rule_spec##*rs=}
	input=$(grep -o -P "(i|\bs)=.+?\.(json|csv|cxt|fimi)" <<< $fca_params)
	#echo 'original input ' $input
	#input=${input##*(i|s)=}
	input=$(echo $input | cut -d= -f2-)
	#echo 'fca input ' $input
	spec_ext=".json"
	workflow_default="both"./fca/ingest_script.sh -p 9000 -f -fp "w=both m=0.05 s=fca/contextSpecFiles/neo4jspec_ProcessEvent.json rs=fca/rulesSpecs/rules_implication_all.json" -m 4000
	workflow_analysis="analysis"
	support=$(grep -o -E "m=[0-9]\.*[0-9]*" <<< $fca_params)
	support=${support##*m=}
	new_port=$(grep -o -E "p=[0-9]+" <<< $fca_params)
	new_port=${new_port##*p=}
	analysis_output=$(grep -o -E "oa=.+?\.(json|csv|txt)" <<< $fca_params)
	analysis_output=${analysis_output##*oa=}
	concept_output=$(grep -o -P "\bo=.+?\.(json|csv|txt|fimi)" <<< $c4)
	concept_output=${concept_output##*o=}
	
	if [[ -z "$new_port" ]] ; then port=8080 ; else port=$new_port; fi #the port value is set to 8080 by default if not passed as parameter in the input quoted string
	if [[ -z "$support" ]] ; then minsupp=0; else minsupp=$support; fi
	if [[ -z "$rule_spec" ]] ; then spec_rules="./fca/rulesSpecs/rules_implication_all.json"; else spec_rules=$rule_spec; fi
	if [[ -z "$w" ]] ; then workflow="both"; else workflow=$w; fi
	
	echo 'input FCA' $input
	
	if [[ $input =~ $spec_ext ]]
		then 
			if [[ $workflow =~ $workflow_default || $workflow =~ $workflow_analysis ]]
			  then
			    if [[ -n "$concept_output" && -n "$analysis_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port -o $concept_output -rs $spec_rules -oa $analysis_output
				elif [[ -z "$concept_output" && -n "$analysis_output" ]]
			      then
			        python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port -rs $spec_rules -oa $analysis_output
			    elif [[ -n "$concept_output" && -z "$analysis_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port -o $concept_output -rs $spec_rules 
				else 
				    python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port -rs $spec_rules 
				fi
			  else
			    if [[ -n "$concept_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port -o $concept_output 
				  else
				    python3 ./fca/fcascript.py -w $w -s $input -m $support --port $port 
				fi
			  fi
		else
			if [[ $workflow =~ $workflow_default || $workflow =~ $workflow_analysis ]]
			  then
			    if [[ -n "$concept_output" && -n "$analysis_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port -o $concept_output -rs $spec_rules -oa $analysis_output
				elif [[ -z "$concept_output" && -n "$analysis_output" ]]
			      then
			        python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port -rs $spec_rules -oa $analysis_output
			    elif [[ -n "$concept_output" && -z "$analysis_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port -o $concept_output -rs $spec_rules 
				else 
				    python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port -rs $spec_rules 
				fi
			  else
			    if [[ -n "$concept_output" ]]
			      then
					python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port -o $concept_output 
				  else
				    python3 ./fca/fcascript.py -w $w -i $input -m $support --port $port 
				fi
			  fi
	fi
}

if [[ $INGEST -eq 1 ]]
 then
	if [[ -z $pid ]]
		then
			echo 'Starting UI at port '$port
			sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
			if [[ $gen -eq 0 && $fca -eq 0 ]]
				then
					echo 'Running the full input conversion/FCA pipeline'
					sleep $SLEEP && conversion $default $port ${CONV[@]} && fca_pipeline ${FCA_PARAMS[@]}
			elif [[ $gen -eq 1 ]]
				then
					echo 'Running the input conversion only'
					sleep $SLEEP && conversion $default $port ${CONV[@]}
			elif [[ $fca -eq 1 ]]
				then
					echo 'Running the FCA pipeline only'
					sleep $SLEEP && fca_pipeline ${FCA_PARAMS[@]}
			else
				echo 'UI running. Nothing else to do.'
			fi
	else
		echo 'UI at port' $port 'already started. Starting input conversion and/or FCA pipeline'
		if [[ $gen -eq 0 && $fca -eq 0 ]]
		  then
			echo 'Running the full input conversion/FCA pipeline'
			conversion $default $port ${CONV[@]} && fca_pipeline ${FCA_PARAMS[@]}
		elif [[ $gen -eq 1 ]]
		    then
				echo 'Running the input conversion only'
				conversion $default $port ${CONV[@]}
		elif [[ $fca -eq 1 ]]
			then
				echo 'Running the FCA pipeline only'
				fca_pipeline ${FCA_PARAMS[@]}
		else 
			echo 'UI running. Nothing else to do.'
		fi
	fi
fi
