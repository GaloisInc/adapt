#! /usr/bin/env bash

python3 ../feature_extractor/extract_features.py netflow_view_features.csv process_view_features.csv
./../osu_iforest/iforest.exe -i netflow_view_features.csv -o netflow_view_features_score.csv -m 1 -t 100 -s 100
./../osu_iforest/iforest.exe -i process_view_features.csv -o process_view_features_score.csv -m 1 -t 100 -s 100
python3 ../feature_extractor/attach_scores.py netflow_view_features_score.csv
python3 ../feature_extractor/attach_scores.py process_view_features_score.csv
python3 UnitTest.py
