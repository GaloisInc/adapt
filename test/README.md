
### Build Anomaly Detector

```
$ cd ../osu_iforest
$ make
```

### Run Anomaly Detection Module

```
$ python3 StartAD.py
Waiting for signal from segmenter...
ConsumerRecord(topic='ad', partition=0, offset=1, key=None, value=b'1')
[ProduceResponsePayload(topic='ac', partition=0, error=0, offset=2)]
Starting Anomaly Detection
Inserting dummy segment nodes
Finished Inserting dummy segment nodes
Extracting features from segments
Segment 0
Segment 1
Segment 2
Segment 3
Segment 4
Segment 5
Segment 6
Segment 7
Segment 8
Segment 9
Feature extraction and attaching finished
Writing features to file: seg_spec_features.csv
Writing Finished
# Trees     = 100
# Samples   = 100
Original Data Dimension: 10,5
Anomaly score attach finished
POST Test PASSED!
Finished Anomaly Detection
[ProduceResponsePayload(topic='ac', partition=0, error=0, offset=3)]

```

### Test Only Anomaly Detection Module (No interaction with other module)

`$ ./start.sh`
