#! /usr/bin/env bash

python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 1 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 2 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 3 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 4 &
wait
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 5 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 6 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 7 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 8 &
wait
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 9 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 10 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 11 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 12 &
wait
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 13 &
python3 ../feature_extractor/compute_views.py ../feature_extractor/view_specification.json 14 &

wait
