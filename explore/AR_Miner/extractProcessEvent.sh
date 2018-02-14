#!/bin/bash 
echo "Now Running The CSV File Extractor From The DB"
json='../contextSpecFiles/neo4jspec_ProcessEvent.json'
csv='./contexts/ProcessEventSample.csv'
rcf='./contexts/PrecessEventSample.rcf'
default_nb_objects=10
Rscript get_query_shell.r $json $csv $rcf $default_nb_objects
 

 
  