#!/bin/bash 
context_name='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_file='./contexts/Context_ProcessEvent.rcf'
output_scoring_file='./contexts/Scoring_Of_Context_ProcessEvent.csv'
MinSup='97'
MinConf='97'
Rscript contexts_scoring_shell.r $context_name $csv_file $rcf_file $output_scoring_file $MinSup $MinConf
