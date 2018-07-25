#!/bin/bash 
input_scoring_file='./contexts/Output_Scoring_Feedback_Of_Context_ProcessEvent.csv'
MinSup='97'
MinConf='97'
original_scoring_file='./contexts/Scoring_Of_Context_ProcessEvent-cadet-pandex.csv'
gt_file='./contexts/cadets_pandex_webshell.json'
currentview='ProcessEvent'
Rscript Trajectory_Violator_Objects_shell.r $original_scoring_file $input_scoring_file $gt_file $currentview $MinSup $MinConf 