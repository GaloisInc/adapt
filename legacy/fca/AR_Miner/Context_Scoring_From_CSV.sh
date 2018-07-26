#!/bin/bash 
echo "Now Running The CSV Files Comparison"
context_name='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_file='./contexts/Context_ProcessEvent.rcf'
output_scoring_file='./contexts/Scoring_Of_Context_ProcessEvent.csv'
MinSup='90'
MinConf='90'
Rscript soft_contexts_scoring_shell.r $context_name $csv_file $rcf_file $output_scoring_file $MinSup $MinConf
