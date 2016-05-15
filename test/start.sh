#! /usr/bin/env bash

python3 ../feature_extractor/extract_features.py seg_spec_features.csv
./../osu_iforest/iforest.exe -i seg_spec_features.csv -o seg_spec_features_score.csv -m 1-3 -t 100 -s 100
python3 ../feature_extractor/attach_scores.py seg_spec_features_score.csv
python3 UnitTest.py
