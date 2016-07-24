#! /usr/bin/env bash

python3 ../feature_extractor/compute_views.py netflow_view_features.csv process_view_features.csv

l=`wc -l netflow_view_features.csv | sed 's/^\([0-9]*\).*$/\1/'`
if [ $l -gt 1 ]
then
./../osu_iforest/iforest.exe -i netflow_view_features.csv -o netflow_view_features_score.csv -m 1 -t 100 -s 100
python3 ../feature_extractor/attach_scores.py netflow_view_features_score.csv
fi

l=`wc -l process_view_features.csv | sed 's/^\([0-9]*\).*$/\1/'`
if [ $l -gt 1 ]
then
./../osu_iforest/iforest.exe -i process_view_features.csv -o process_view_features_score.csv -m 1 -t 100 -s 100
python3 ../feature_extractor/attach_scores.py process_view_features_score.csv
fi

python3 UnitTest.py
