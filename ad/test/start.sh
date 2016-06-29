#! /usr/bin/env bash

python3 ../feature_extractor/extract_features.py features.csv
./../osu_iforest/iforest.exe -i features.csv -o features_score.csv -m 1 -t 100 -s 100
python3 ../feature_extractor/attach_scores.py features_score.csv
# python3 UnitTest.py
