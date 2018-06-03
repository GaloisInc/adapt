#!/bin/bash 
echo "Now Running The CSV File Extractor From The DB"
json='../contextSpecFiles/neo4jspec_ProcessEvent.json'
csv='./contexts/Context_ProcessEvent.csv'
rcf='./contexts/Context_ProcessEvent.rcf'
default_nb_objects=10
Rscript get_query_shell.r $json $csv $rcf $default_nb_objects
 

 
  