## Segment Specification

`$ python3 ../feature_extractor/seg_specifier.py > seg_spec.csv`

## Feature Extraction

`$ python3 ../feature_extractor/extract_features.py seg_spec.csv > seg_spec_features.csv`

## Anomaly Score Calculation

`$ ./../osu_iforest/iforest.exe -i seg_spec_features.csv -o seg_spec_features_score.csv -m 1-3 -t 100 -s 100`
