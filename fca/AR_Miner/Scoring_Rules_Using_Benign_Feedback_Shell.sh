#!/bin/bash 
context_name='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_file='./contexts/Context_ProcessEvent.rcf'
output_scoring_file='./contexts/Output_Scoring_Feedback_Of_Context_ProcessEvent.csv'
MinSup='90'
MinConf='90'
Benign_file='./contexts/benign_rules.csv'
original_scoring_file='./contexts/Input_Scoring_Of_Context_ProcessEvent.csv'
gt_file='./contexts/gt_file.json'
Rscript Scoring_Rules_Using_Benign_Feedback_Shell.r $context_name $csv_file $rcf_file $output_scoring_file $MinSup $MinConf $Benign_file $original_scoring_file $gt_file
