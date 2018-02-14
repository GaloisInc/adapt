#!/bin/bash 
echo "Now Running The CSV Files Comparison"
a='./contexts/ProcessEventSample.csv'
b='./contexts/ProcessEventSample_altered.csv'
Rscript compare_csv.r $a $b 
 

 
  