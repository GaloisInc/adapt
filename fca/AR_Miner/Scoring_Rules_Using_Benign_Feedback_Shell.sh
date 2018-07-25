#!/bin/bash 
context_name='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_file='./contexts/Context_ProcessEvent.rcf'
output_scoring_file='./contexts/Output_Scoring_Feedback_Of_Context_ProcessEvent.csv'
MinSup='97'
MinConf='97'
Benign_file='./contexts/AssociationRules_Benign_ProcessEvent-Cadet-Pandex.csv'
#'./contexts/benign_rules.csv'
original_scoring_file='./contexts/Scoring_Of_Context_ProcessEvent-cadet-pandex.csv'
#/contexts/Input_Scoring_Of_Context_ProcessEvent.csv'
Rscript Scoring_Rules_Using_Benign_feedback_Shell.r $context_name $csv_file $rcf_file $output_scoring_file $MinSup $MinConf $Benign_file $original_scoring_file 
