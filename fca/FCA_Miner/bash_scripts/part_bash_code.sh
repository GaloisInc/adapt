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
