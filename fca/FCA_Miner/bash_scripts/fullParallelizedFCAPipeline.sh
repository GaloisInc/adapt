#!/bin/bash

PARAMS=()
SLEEP=60

while (( "$#" )); do
  case "$1" in
    -i|--ingest)
      INGEST=1
      shift 1
      ;;
	-ui)
      UI=1
      shift 1
      ;;
    -c|--conversion2csv)
      CONVERT=1
      shift 1
      ;;
    -fca)
      FCA=1
      shift 1
      ;;
	-fi|--files_to_ingest)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								FILES+=($a)
						fi			
			done
			s=${#FILES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -r|--search_repository)
		REP=$2
		if [[ -z "$REP" ]] ; then rep='.'; else rep=$REP; fi
		shift 2
		;;
	-E|--regexps)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				REGEXP+=( '*cadets-*cdm17.bin' '*trace-*cdm17.bin' '*five*cdm17.bin' )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								REGEXP+=($a)
						fi			
			done
			s=${#REGEXP[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-p|--port)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				PORT+=(8080)
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
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
	-m|--mem)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				MEM+=(3000)
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								MEM+=($a)
						fi			
			done
			s=${#MEM[@]}
			#echo $s
			shift "$s"
	fi
      ;;
	-db|--dbkeyspace)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") ]]
			then
				DBKEYSPACE+=( "neo4j" )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								DBKEYSPACE+=($a)
						fi			
			done
			s=${#DBKEYSPACE[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -seq|--sequential_ingest)
	  SEQ=1
      shift 1
      ;;
	-x|--context_names)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				CONTEXT_NAMES+=( "ProcessEvent" )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								CONTEXT_NAMES+=($a)
						fi			
			done
			s=${#CONTEXT_NAMES[@]}
			#echo $s
			shift "$s"
	fi
      ;;
	-xs|--context_spec_directories)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				CONTEXT_SPEC_DIR+=( "./fca/contextSpecFiles" )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								CONTEXT_SPEC_DIR+=($a)
						fi			
			done
			s=${#CONTEXT_SPEC_DIR[@]}
			#echo $s
			shift "$s"
	fi
      ;;
	-vd|--csv_dir)
		CSV_DIR=$2
		shift 2
	;;
	-pv|--path2CSV)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								PATH2CSV+=($a)
						fi			
			done
			s=${#PATH2CSV[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-fw|fca_workflow)
		FCA_WORKFLOW=$2
		shift 2
      ;; 
	-fci|--fca_input_files)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								FCA_INPUT+=($a)
						fi			
			done
			s=${#FCA_INPUT[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-ms|--minimal_supports)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				MIN_SUPPORT+=(0)
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								MIN_SUPPORT+=($a)
						fi			
			done
			s=${#MIN_SUPPORT[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-rs|--rule_specification_files)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				RULE_SPEC_FILES+=( "./rulesSpecs/rules_positive_implication.json" )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								RULE_SPEC_FILES+=($a)
						fi			
			done
			s=${#RULE_SPEC_FILES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-rn|--rule_names)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				RULE_NAMES+=( 	"implication"	)
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								RULE_NAMES+=($a)
						fi			
			done
			s=${#RULE_NAMES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-rt|--rule_minimal_thresholds)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				RULE_MIN_THRESH+=(0)
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								RULE_MIN_THRESH+=($a)
						fi			
			done
			s=${#RULE_MIN_THRESH[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
	-nr|--number_rules)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then
				NUMBER_RULES+=( "'*'" )
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								NUMBER_RULES+=($a)
						fi			
			done
			s=${#NUMBER_RULES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -ad|--analysis_output_dir)
		ANALYSIS_OUTPUT_DIR=$2
		shift 2
    ;;
	-oa|--analysis_output_files)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								ANALYSIS_OUTPUT+=($a)
						fi			
			done
			s=${#ANALYSIS_OUTPUT[@]}
			#echo $s
			shift "$s"
	fi
      ;;
    -cd|--concept_files_dir)
		CONCEPT_FILES_DIR=$2
		shift 2
    ;;
	-cf|--concept_files)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "-co") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--concept_output_files") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "-co") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--concept_output_files") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								CONCEPT_FILES+=($a)
						fi			
			done
			s=${#CONCEPT_FILES[@]}
			#echo $s
			shift "$s"
	fi
      ;;
    -cod|--concept_output_dir)
		CONCEPT_OUTPUT_DIR=$2
		shift 2
    ;;
-co|--concept_output_files)
		 len="$#"
		 if [[ ($2 == "--ingest") || ($2 == "-fi") || ($2 == "-c") || ($2 == "--concept_files") || ($2 == "-i") || ($2 == "-ui") || ($2 == "-seq") || ($2 == "--search_repository") || ($2 == "--rule_names") || ($2 == "-m") || ($2 == "--number_rules") || ($2 == "-xs") || ($2 == "--analysis_output_dir") || ($2 == "--rule_minimal_thresholds") || ($2 == "-ms") || ($2 == "-E") || ($2 == "-fca") || ($2 == "-rs") || ($2 == "-rt") || ($2 == "--concept_files_dir") || ($2 == "-rn") || ($2 == "--dbkeyspace") || ($2 == "--port") || ($2 == "-vd") || ($2 == "--context_names") || ($2 == "--minimal_supports") || ($2 == "--regexps") || ($2 == "--rule_specification_files") || ($2 == "-cf") || ($2 == "-pv") || ($2 == "-cod") || ($2 == "--sequential_ingest") || ($2 == "--mem") || ($2 == "--csv_dir") || ($2 == "--conversion2csv") || ($2 == "--path2CSV") || ($2 == "-fci") || ($2 == "-oa") || ($2 == "--analysis_output_files") || ($2 == "--concept_output_dir") || ($2 == "-r") || ($2 == "--files_to_ingest") || ($2 == "-p") || ($2 == "--fca_input_files") || ($2 == "-x") || ($2 == "-nr") || ($2 == "-ad") || ($2 == "--context_spec_directories") || ($2 == "-cd") || ($2 == "-db") ]]
			then				
				shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					 if [[ ($a == "--ingest") || ($a == "-fi") || ($a == "-c") || ($a == "--concept_files") || ($a == "-i") || ($a == "-ui") || ($a == "-seq") || ($a == "--search_repository") || ($a == "--rule_names") || ($a == "-m") || ($a == "--number_rules") || ($a == "-xs") || ($a == "--analysis_output_dir") || ($a == "--rule_minimal_thresholds") || ($a == "-ms") || ($a == "-E") || ($a == "-fca") || ($a == "-rs") || ($a == "-rt") || ($a == "--concept_files_dir") || ($a == "-rn") || ($a == "--dbkeyspace") || ($a == "--port") || ($a == "-vd") || ($a == "--context_names") || ($a == "--minimal_supports") || ($a == "--regexps") || ($a == "--rule_specification_files") || ($a == "-cf") || ($a == "-pv") || ($a == "-cod") || ($a == "--sequential_ingest") || ($a == "--mem") || ($a == "--csv_dir") || ($a == "--conversion2csv") || ($a == "--path2CSV") || ($a == "-fci") || ($a == "-oa") || ($a == "--analysis_output_files") || ($a == "--concept_output_dir") || ($a == "-r") || ($a == "--files_to_ingest") || ($a == "-p") || ($a == "--fca_input_files") || ($a == "-x") || ($a == "-nr") || ($a == "-ad") || ($a == "--context_spec_directories") || ($a == "-cd") || ($a == "-db") ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
						else 
								#echo 'k' $k 'a' ${!k}
								CONCEPT_OUTPUT+=($a)
						fi			
			done
			s=${#CONCEPT_OUTPUT[@]}
			#echo $s
			shift "$s"
	fi
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

pid=$(pgrep -f "sbt -mem [0-9]+ -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$DB run")
#echo 'pid' $pid
function regex1 { gawk 'match($0,/'$1'/, ary) {print ary['${2:-'1'}']}'; }
export -f regex1
create_dbkeyspace(){ 
	namef=$1
	DB=${namef%-cdm*.bin}
	DB=${DB##*ta1-}
	DB=$(echo $DB | tr - _) 
	echo $DB
}

generate_ports(){
	seed=$1
	interv=$2
	number=$3
	let "end_port=$seed+$interv"
	ports=($(shuf -i $seed-$end_port -n $number))
	echo ${ports[@]}
}


get_dbkeyspace_from_runningUI_port(){
	port=$1
	potential_ui_pid=$(lsof -t -i :$port)
	if [[ -n "$potential_ui_pid" && ${#potential_ui_pid[@]} -eq 1 ]]
		then
			parent=$(ps -o ppid= $potential_ui_pid)
			db=$(ps -p $parent -o command= | regex1 '.+dbkeyspace\=(.+)\srun')
			if [[ -n "$db" ]]
				then
					echo $db $port
			fi
	fi
}

ingest(){
mem=$1
port=$2
dbkeyspace=$3
loadfiles=$4

sbt -mem $mem -Dadapt.runflow=db -Dadapt.ingest.quitafteringest=yes -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace -Dadapt.ingest.loadfiles.0=$loadfiles run
	
}


ui(){
mem=$1
port=$2
dbkeyspace=$3
sleep_time=$4
#add handling of optional port
sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
sleep $sleep_time
	
}


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
	else
	   if [[ -z  "$new_port" && -z "$context_path" && -z "$spec_directory" && -n "$context_name" ]]
	       then
				echo "python3 ./fca/conversion_script.py -p $port -n $context_name"
				python3 ./fca/conversion_script.py -p $port -n $context_name
	   elif [[ -z  "$new_port" && -n "$context_path" && -z "$spec_directory" && -n "$context_name" ]]
			then
			    echo "python3 ./fca/conversion_script.py -p $port -n $context_name -d $spec_directory"
				python3 ./fca/conversion_script.py -p $port -n $context_name -d $spec_directory
	   elif [[ -z  "$new_port" && -n "$context_path" && -n "$spec_directory" && -n "$context_name" ]]
			then
			    echo "python3 ./fca/conversion_script.py -p $port -n $context_name -cp $context_path -d  $spec_directory"
				python3 ./fca/conversion_script.py -p $port -n $context_name -cp $context_path -d  $spec_directory
	   elif [[ -n  "$new_port" && -n "$context_path" && -n "$spec_directory" && -n "$context_name" ]]
			then
			    echo "python3 ./fca/conversion_script.py -p $new_port -n $context_name -d $spec_directory -cp $context_path"
				python3 ./fca/conversion_script.py -p $new_port -n $context_name -d $spec_directory -cp $context_path
	   else
	        echo "python3 ./fca/conversion_script.py -p $port"
			python3 ./fca/conversion_script.py -p $port
	   fi
	 fi
}

fca_execution(){
	
}

export -f ingest
export -f create_dbkeyspace
export -f generate_ports
#variable initialization

#initialize file values (get values from -fi flag if not empty else use -r/-E flags)
if [[ -n "$FILES" ]]
	then
		files="${FILES[@]}"
elif [[ -z "$FILES" && -n "$rep" && -n "$REGEXP" ]]
	reg=( find \"$rep\" -name )
	for i in seq 0 $(( ${#REGEXP[@]} - 2 )); do  reg+=( \'${REGEXP[i]}\' -or -name ) ; done
	reg+=( \'${REGEXP[-1]}\')
	files=( $( eval ${reg[@]} ) )
fi

fileindices=${!files[*]}
#initialize port, mem and dbkeyspace values

#define dbkeyspace

if [[ -n "$files" ]]
	then
		if [[ ( -z "$DBKEYSPACE" ) || ($DBKEYSPACE == "neo4j") ]]
		  then
				dbkeyspace=( $(parallel -k create_dbkeyspace ::: ${files[@]}) )
		elif [[ ${#DBKEYSPACE[@]} -lt ${#files[@]} ]]
			then
				dbkeyspace=(${DBKEYSPACE[@]})
				dbkeyspace+=( $(parallel -k create_dbkeyspace ::: ${files[@]:${#DBKEYSPACE[@]}}) )
		elif [[ ${#DBKEYSPACE[@]} -eq ${#files[@]} ]]
			then
				dbkeyspace=(${DBKEYSPACE[@]})
		elif [[ ${#DBKEYSPACE[@]} -gt ${#files[@]} ]]
			then
				dbkeyspace=(${DBKEYSPACE[@]::${#files[@]}})
		fi
fi

#define ports
interv=2500
if [[ -n "$files" ]]
	then
		if [[ -z "$PORT"  ]]
		  then
				seed=8080 #add port availability check
				ports=($(generate_ports $seed $interv ${#files[@]}))
		elif [[ ${#PORT[@]} -eq 1 ]]
		  then
				seed=${PORT[0]}  #add port availability check
				ports=($(generate_ports $seed $interv ${#files[@]}))
		elif [[ ${#PORT[@]} -lt ${#files[@]} ]]
			then
				ports=(${PORT[@]})
				seed=$(printf '%s\n' "${ports[@]}" | sort -nr | head -n1)
				ports+=($(generate_ports $seed $interv $(( ${#files[@]}-${#PORT[@]} ))))
		elif [[ ${#PORT[@]} -eq ${#files[@]} ]]
			then
				ports=(${PORT[@]})
		elif [[ ${#PORT[@]} -gt ${#files[@]} ]]
			then
				ports=${PORT[@]::${#files[@]}}
		fi
fi

#define dbkeyspaces and ports when no files provided

if [[ -z "$files" ]]
  then
	  if [[ -n "$DBKEYSPACE" && -n "$PORT" ]]
		then
			if [[ ${#DBKEYSPACE[@]} -eq ${#PORT[@]} ]]
				then
					dbkeyspace=(${DBKEYSPACE[@]})
					ports=(${PORT[@]})
			elif [[ ${#DBKEYSPACE[@]} -lt ${#PORT[@]} ]]
				then
					dbkeyspace=(${DBKEYSPACE[@]})
					ports=(${PORT[@]::${#DBKEYSPACE[@]}})
			elif [[ ${#DBKEYSPACE[@]} -gt ${#PORT[@]} ]]
				then
					dbkeyspace=(${DBKEYSPACE[@]})
					ports=(${PORT[@]})
					seed=$(printf '%s\n' "${ports[@]}" | sort -nr | head -n1)
					ports+=($(generate_ports $seed $interv $(( ${#dbkeyspace[@]}-${#PORT[@]} ))))
			fi
	  elif [[ -n "$DBKEYSPACE" && -z "$PORT" ]]
		then
			dbkeyspace=(${DBKEYSPACE[@]})
			seed=8080 #add port availability check
			ports=($(generate_ports $seed $interv ${#dbkeyspace[@]}))
	  elif [[ -z "$DBKEYSPACE" && -n "$PORT" ]]
		then
			
	  fi
  
fi

#ingest




if [[ $INGEST -eq 1 ]]
   then
      #if sequential ingest
      if [[ $SEQ -eq 1 ]]
			then
				for i in $fileindices
					do
						ingest ${mem[i]} ${port[i]} ${dbkeyspace[i]} ${files[i]}
				done
      #else if parallel ingest
      else
			parallel  --jobs $NB_JOBS ingest -xapply ::: ${mem[@]} ::: ${port[@]} ::: ${dbkeyspace[@]} ::: ${files[@]} 
      fi

#ui

if [[ $UI -eq 1 ]]
	then 
		parallel  --jobs $NB_JOBS ui -xapply ::: ${mem[@]} ::: ${port[@]} ::: ${dbkeyspace[@]}
fi

#conversion

#fca
