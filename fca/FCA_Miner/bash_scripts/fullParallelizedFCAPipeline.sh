#!/bin/bash

PARAMS=()
SLEEP=60

#command line options parser
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
				RULE_SPEC_FILES+=( "./fca/FCA_Miner/rulesSpecs/rules_positive_implication.json" )
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

#make sure the script is executed from adapt directory
if ! [[ ($(basename $PWD) =~ 'adapt') ]] 
	then 
		new_path=$(echo $PWD | grep -Eo '.+adapt/')
		if [[ -n "$new_path" ]]
			then
				cd $new_path 
			else
				cd $(dirname $(find $HOME -type d -wholename '*adapt/fca'))
		fi 
fi


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


#random generation of number x ports given seed (i.e start of interval) and length of interval
generate_ports(){
	seed=$1
	interv=$2
	number=$3
	let "end_port=$seed+$interv"
	ports=($(shuf -i $seed-$end_port -n $number))
	echo ${ports[@]}
}

#check usability of port seed and generate usable one if not
generate_port_seed(){
 seed=$1
 used_ports=( $(nmap -p "*" localhost | regex1 '([0-9]+)\/[a-zA-Z]+[[:space:]]+[a-zA-Z]+[[:space:]]+[a-zA-Z_\-]+') )	
 max_used_port=$(printf '%s\n' "${used_ports[@]}" | sort -nr | head -n1)
 if [[ -n $(lsof -t -i :$seed) ]]
	then 
		echo $(( $max_used_port+1 ))
	else 
		echo $seed
 fi
}

#get dbkeypace of running UI from port number
get_dbkeyspace_from_runningUI_port(){
	port=$1
	potential_ui_pid=$(lsof -t -i :$port)
	if [[ -n "$potential_ui_pid" && ${#potential_ui_pid[@]} -eq 1 ]]
		then
			parent=$(ps -o ppid= $potential_ui_pid)
			db=$(ps -p $parent -o command= | regex1 '.+dbkeyspace\=([A-Za-z0-9_\-\.]+)\s.+')
			if [[ -n "$db" ]]
				then
					echo $db $port
			fi
	fi
}

#construct names of csv context names (output of conversion phase)
construct_path2csv(){
		csv_dir=$1
		dbkeyspace=$2
		context_name=$3
		path2csv=$csv_dir'/'$dbkeyspace'_'$context_name'_1.csv'
		if [[ -e $path2csv ]]
		  then
			num=$(ls $csv_dir'/'$dbkeyspace'_'$context_name* | sort -V | tail -1)
			num=${num##*_}
			num=$(( ${num%.csv}+1 ))
			path2csv=$csv_dir'/'$dbkeyspace'_'$context_name'_'$num'.csv'
		fi
		echo $path2csv
}

construct_output_analysis(){
		fca_analysis_dir=$1
		dbkeyspace=$2
		context_name=$3
		min_support=$4
		minsupp=$(echo $min_support | tr . _)
		rule_spec=$5
		spec=$(basename "$rule_spec" | cut -d. -f1)
		analysis_output=$fca_analysis_dir'/'$dbkeyspace'_'$context_name'_support'$minsupp'_ruleSpec_'$spec'_1.csv'
		if [[ -e $analysis_output ]]
		  then
			num_a=$(ls $fca_analysis_dir'/'$dbkeyspace'_'$context_name'_support'$minsupp'_ruleSpec_'$spec* | sort -V | tail -1)
			num_a=${num_a##*_}
			num_a=$(( ${num_a%.csv}+1 ))
			ANALYSIS_OUTPUT=$fca_analysis_dir'/'$dbkeyspace'_'$context_name'_support'$minsupp'_ruleSpec_'$spec'_'$num_a'.csv'
		fi
		echo $analysis_output
}

export -f get_dbkeyspace_from_runningUI_port
export -f construct_path2csv
export -f construct_output_analysis

# function to start ingest 
ingest(){
	mem=$1
	port=$2
	dbkeyspace=$3
	loadfiles=$4
	
	sbt -mem $mem -Dadapt.runflow=db -Dadapt.ingest.quitafteringest=yes -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace -Dadapt.ingest.loadfiles.0=$loadfiles run
}


# function to start ui instance
ui(){
	mem=$1
	port=$2
	dbkeyspace=$3
	sleep_time=$4
	#add handling of optional port
	#check if ui started
	pid=$(pgrep -f "sbt -mem [0-9]+ -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no (-Dadapt.runtime.port=$port |-Dadapt.runtime.dbkeyspace=$DB |-Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$DB )run")
	dbport_arr=( $(get_dbkeyspace_from_runningUI_port $port) )
	if [[ -z $pid && -z "$dbport_arr" ]]
		then
			sbt -mem $mem -Dadapt.runflow=ui -Dadapt.ingest.quitafteringest=no -Dadapt.runtime.port=$port -Dadapt.runtime.dbkeyspace=$dbkeyspace run &
			sleep $sleep_time
	fi
}


#conversion of query results to csv context function
conversion(){
	context_name=$1
	port=$2
	path2csv=$3
	specdirectories=($@)
	if [[ -n "$specdirectories" ]]
		then 
			spec_dirs=$(perl -le 'print join " ",@ARGV' "$(echo ${specdirectories[@]}|tr '\n' ' ')")
	fi
	
	if [[ -z "$port" && -z "$path2csv" && -z "$specdirectories" && -z "$context_name" ]]
	       then
				echo "python3 ./fca/conversion_script.py -p $port"
				python3 ./fca/FCA_Miner/python_scripts/conversion_script.py 
	elif [[ -z  "$port" && -z "$path2csv" && -z "$specdirectories" && -n "$context_name" ]]
			then
				echo "python3 ./fca/conversion_script.py -n $context_name"
				python3 ./fca/FCA_Miner/python_scripts/conversion_script.py -n $context_name
	   elif [[ -z  "$port" && -z "$path2csv" && -n "$specdirectories" && -n "$context_name" ]]
			then
			    echo "python3 ./fca/conversion_script.py -n $context_name -d  $spec_dirs"
				python3 ./fca/FCA_Miner/python_scripts/conversion_script.py -n $context_name -d  $spec_dirs
	  elif [[ -z  "$port" && -n "$path2csv" && -n "$specdirectories" && -n "$context_name" ]]
			then
			    echo "python3 ./fca/conversion_script.py -n $context_name -d $spec_dirs -cp $ptah2csv"
				python3 ./fca/FCA_Miner/python_scripts/conversion_script.py  -n $context_name -d $spec_dirs -cp $path2csv
	   else
	        echo "python3 ./fca/conversion_script.py -p $port -n $context_name -d $spec_dirs -cp $ptah2csv"
			python3 ./fca/FCA_Miner/python_scripts/conversion_script.py  -p $port -n $context_name -d $spec_dirs -cp $path2csv
	   fi

}

fca_execution(){
	fca_workflow=$1
	analysis_output=$2
	port=$3
	fca_input=$4
	min_support=$5
	rule_spec=$6
	
	
	input_base=$(basename $fca_input)
	input_ext=${input_base##*.}
	if [[ $input_ext == 'json' ]]
	 then
		echo "python3 ./fca/FCA_Miner/python_scripts/fcascript.py --quiet -w $w -s $fca_input -m $min_support --port $port -rs $rule_spec -oa $analysis_output"
		python3 ./fca/FCA_Miner/python_scripts/fcascript.py --quiet -w $w -s $fca_input -m $min_support --port $port -rs $rule_spec -oa $analysis_output
	elif [[ ($input_ext == 'csv') || ($input_ext == 'cxt') || ($input_ext == 'fimi') ]] 	
		echo "python3 ./fca/FCA_Miner/python_scripts/fcascript.py --quiet -w $w -i $fca_input -m $min_support -rs $rule_spec -oa $analysis_output"
		python3 ./fca/FCA_Miner/python_scripts/fcascript.py --quiet -w $w -i $fca_input -m $min_support -rs $rule_spec -oa $analysis_output
	else
		echo "Input format $input_ext not supported. Expecting cxt, fimi, json or csv. No analysis."
	fi
}

export -f ingest
export -f create_dbkeyspace
export -f generate_ports

export -f generate_port_seed
export -f fca_execution

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
				seed=8080 
				seed=$(generate_port_seed $seed) #re-generating seed port if port not available
				ports=($(generate_ports $seed $interv ${#files[@]}))
		elif [[ ${#PORT[@]} -eq 1 ]]
		  then
				seed=${PORT[0]}  
				seed=$(generate_port_seed $seed) #re-generating seed port if port not available
				ports=($(generate_ports $seed $interv ${#files[@]}))
		elif [[ ${#PORT[@]} -lt ${#files[@]} ]]
			then
				ports=(${PORT[@]})
				seed=$(printf '%s\n' "${ports[@]}" | sort -nr | head -n1)
				seed=$(generate_port_seed $seed) #re-generating seed port if port not available
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
			seed=8080 
			seed=$(generate_port_seed $seed)
			ports=($(generate_ports $seed $interv ${#dbkeyspace[@]}))
	  elif [[ -z "$DBKEYSPACE" && -n "$PORT" ]]
		then
			parallel -k --xapply get_dbkeyspace_from_runningUI_port ::: ${PORT[@]} > tmp_res.txt
			while read line; do IFS=' ' dbkeyspace+=( $(echo $line | head -n1 | awk '{print $1;}') ); ports+=( $(echo $line | head -n1 | awk '{print $2;}') ); done < tmp_res.txt
			rm tmp_res.txt
	  fi
  
fi


#ingest/ui memory allocation initialization

if [[ -z "$MEM" ]] ; then mem=( 3000 ); else mem=( ${MEM[@]} ); fi


#some more variable initializations

##variables relevant to conversion

#context name initialization

if [[ -z "$CONTEXT_NAMES" ]]; then context_names=( "ProcessEvent" ) ; else context_names=( ${CONTEXT_NAMES[@]} ); fi  

#context spec directory initialization

if [[ -z "$CONTEXT_SPEC_DIR" ]]; then context_spec_dir=( './fca/contextSpecFiles' ); else context_spec_dir=( ${CONTEXT_SPEC_DIR[@]} ); fi

#csv context directory initialization

if [[ -z "$CSV_DIR" ]]; then csv_dir=( './fca/csvContexts' ); else csv_dir=( ${CSV_DIR[@]} ); fi



#ingest

if [[ $INGEST -eq 1 ]]
   then
	  if [[ -n ${files[@]} ]]
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
	  fi
fi
		

#ui

if [[ $UI -eq 1 ]]
	then 
		parallel  --jobs $NB_JOBS ui -xapply ::: ${mem[@]} ::: ${port[@]} ::: ${dbkeyspace[@]} ::: $SLEEP
fi


#conversion

if [[ $CONVERT -eq 1 ]]
   then
		if [[ -z $PATH2CSV ]]
		   then
				readarray --jobs $NB_JOBS path2csv <<< $(parallel -k construct_path2csv ::: $csv_dir ::: ${dbkeyspace[@]} ::: ${context_names[@]})
		elif [[  ${#PATH2CSV[@]} -eq  $(( ${#dbkeyspace[@]} * ${#context_names[@]} )) ]]
			then
				path2csv=( ${PATH2CSV[@]} )
		elif [[  ${#PATH2CSV[@]} -gt  $(( ${#dbkeyspace[@]} * ${#context_names[@]} )) ]]
			then
				path2csv=( ${PATH2CSV[@]::$(( ${#dbkeyspace[@]} * ${#context_names[@]} ))} )
		else
			readarray --jobs $NB_JOBS csvpaths <<< $(parallel -k construct_path2csv ::: $csv_dir ::: ${dbkeyspace[@]} ::: ${context_names[@]})
			path2csv=( ${PATH2CSV[@]} )
			path2csv+=( ${csvpaths[@]:${#PATH2CSV[@]}} )
		fi
		parallel  -k --jobs $NB_JOBS conversion -xapply ::: ${context_names[@]} ::: ${ports[@]} ::: ${path2csv[@]} ::: ${context_spec_dir[@]}
fi

#fca

if [[ $FCA -eq 1 ]]
   then
		##initialization of variables relevant to FCA execution
		#fca workflow
		if [[ -z "$FCA_WORKFLOW" ]]; then fca_workflow='both'; else fca_workflow=$FCA_WORKFLOW; fi
		#fca input
		if [[ -n "$FCA_INPUT" ]]
			then
				fca_input=( ${FCA_INPUT[@]} )
		else
			if [[ -n "$path2csv" ]]
				then
					fca_input=( ${path2csv[@]} )				
			fi
		fi
		# minimal support
		if [[ -z "$MIN_SUPPORT" ]]; then min_support=(0); else min_support=${MIN_SUPPORT[@]}; fi

		#rule specification
		
		if [[ -n "$RULE_SPEC_FILES" ]]
				then 
					rule_spec_files=( ${RULE_SPEC_FILES[@]} )
		elif [[ -z "$RULE_SPEC_FILES" && -z "$RULE_NAMES" && -z "$RULE_MIN_THRESH" && -z "$NUMBER_RULES" ]]
			then
				rule_spec_files=( "./fca/FCA_Miner/rulesSpecs/rules_positive_implication.json" ) 
		else
				if [[ -z "$NUMBER_RULES" ]] ; then number_rules+=( "'*'" ); elif [[ ${#NUMBER_RULES[@]} -eq 1 ]] ; then number_rules+=(${NUMBER_RULES[0]}) ; else number_rules=("${NUMBER_RULES[@]}"); fi
				if [[ -z "$RULE_MIN_THRESH" ]] ; then rule_thresholds+=( 0.95 ); elif [[ ${RULE_MIN_THRESH[@]} -eq 1 ]] ; then rule_thresholds+=(${RULE_MIN_THRESH[0]}) ; else rule_thresholds=("${RULE_MIN_THRESH[@]}"); fi
				if [[ -z "$RULE_NAMES" ]] ; then rule_names+=( 'implication' ); elif [[ ${#RULE_NAMES[@]} -eq 1 ]] ; then rule_names+=(${RULE_NAMES[0]}) ; else rule_names=("${RULE_NAMES[@]}"); fi
				
				echo 'Constructing rule specification files'
				thresh_string='_thresh'
				numrules_string='_numrules'
				json_ext='.json'
				
				inputargs=$(parallel echo --rule_properties {1} {2} {3} $rule_spec'/'{1}'_thresh'{2}'_numrules'{3}'.json' ::: ${rule_names[@]} ::: ${rule_thresholds[@]} ::: ${number_rules[@]} | sed " s/\(\.\{1\}\)\([0-9]\+\)\(_\)/_\2\3/g ; s/'//g; s/\*/all/g ; s/ all / '*' /g")
				rule_spec_files=( $(parallel echo --rule_properties {1} {2} {3} $rule_spec'/'{1}'_thresh'{2}'_numrules'{3}'.json' ::: ${rule_names[@]} ::: ${rule_thresholds[@]} ::: ${number_rules[@]} | awk '{print $NF}' | sed " s/\(\.\{1\}\)\([0-9]\+\)\(_\)/_\2\3/g ; s/'//g; s/\*/all/g ; s/ all / '*' /g") )
				pythonSpecGen_arg=( python3 fca/ruleSpecificationGeneration.py $inputargs )
		fi
		
		#analysis output paths
		if [[ -z "ANALYSIS_OUTPUT_DIR" ]] ; then analysis_output_dir='./fca/FCA_Miner/fcaAnalysis'; else analysis_output_dir=$ANALYSIS_OUTPUT_DIR; fi
		
		if [[ -z $ANALYSIS_OUTPUT ]]
		   then
				readarray --jobs $NB_JOBS path2csv <<< $(parallel -k construct_output_analysis ::: $analysis_output_dir ::: ${dbkeyspace[@]} ::: ${context_names[@]} ::: ${min_support[@]} ::: ${rule_spec_files[@]} ) 
		elif [[  ${#ANALYSIS_OUTPUT[@]} -eq  $(( ${#dbkeyspace[@]} * ${#context_names[@]} * ${#min_support[@]} * ${#rule_spec_files[@]} )) ]]
			then
				analysis_output=( ${ANALYSIS_OUTPUT[@]} )
		elif [[  ${#ANALYSIS_OUTPUT[@]} -gt  $(( ${#dbkeyspace[@]} * ${#context_names[@]} * ${#min_support[@]} * ${#rule_spec_files[@]} )) ]]
			then
				analysis_output=( ${ANALYSIS_OUTPUT[@]::$(( ${#dbkeyspace[@]} * ${#context_names[@]} * ${#min_support[@]} * ${#rule_spec_files[@]} ))} )
		else
			readarray --jobs $NB_JOBS analysispaths <<< $(parallel -k construct_output_analysis ::: $analysis_output_dir ::: ${dbkeyspace[@]} ::: ${context_names[@]} ::: ${min_support[@]} ::: ${rule_spec_files[@]} )
			analysis_output=( ${ANALYSIS_OUTPUT[@]} )
			analysis_output+=( ${analysispaths[@]:${#ANALYSIS_OUTPUT[@]}} )
		fi
		
		parallel echo {1} {2} {3} ::: ${min_support[@]}  ::: ${rule_spec_files[@]} ::: ${fca_input[@]} | sed -r 's/\s+\S+$//' > test_parallel.txt
		parallel --xapply echo {1} {2} {3} {4} {5} {6} ::: $fca_workflow ::: ${analysis_output[@]} ::: ${port[@]} ::: ${fca_input[@]} :::: test_parallel.txt > $INPUT_FILE
		rm test_parallel.txt
		parallel --joblog ./log_ingestall.txt --jobs $NB_JOBS --xapply fca_execution :::: $INPUT_FILE 
		



fi



