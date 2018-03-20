#!/bin/bash 
input_scoring_file='./contexts/Input_Scoring_Feedback_Of_Context_ProcessEvent.csv'
MinSup='90'
MinConf='90'
original_scoring_file='./contexts/Input_Scoring_Of_Context_ProcessEvent.csv'
gt_file='./contexts/gt_file.json'
currentview='ProcessEvent'
Rscript Trajectory_Violator_Objects_shell.r $Original_input_scoring_file $input_scoring_file $gt_file $currentview $MinSup $MinConf 