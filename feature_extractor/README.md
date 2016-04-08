## Segment Specification

Segment specification files identify segments in a provenance graph for feature extraction. The `seg_specifier.py` generates a sample semgment specification file, which runs inside the TC-in-a-box environment.

For Phase 1, we used a very simple segment type called `VRangeType` i.e. given a range of vertices, for example, `1-100`, it considers `vertex1` to `vertex100` in the graph as a segment. A sample segment specification file is generated below:
```
$ python3 seg_specifier.py > seg_spec.csv
$ cat seg_spec.csv
segment_id,segment_type,segment_type_instance
seg_1,VRangeType,0-360
seg_2,VRangeType,361-720
seg_3,VRangeType,721-1080
seg_4,VRangeType,1081-1440
seg_5,VRangeType,1441-1800
...
```

## Feature Extraction
Given a segment specification, we first need to extract appropriate features to feed to the anomaly detector for score calculation. Ideally, these set of features should depend on the segment type i.e. for a specific segment type we would have a specific set of queries that we can run on titan database.

For Phase 1, we defined some generic features that can be applied to any type of segment. The `extract_features.py`, which runs inside the TC-in-a-box, computes some of these predefined features: counting number of read or write events or counting number of threads that have been started in the specified segment. An example of feature extraction based on the above segment specification is given below:

```
$ python3 extract_features.py seg_spec.csv > seg_spec_features.csv
$ cat seg_spec_features.csv
segment_id,segment_type,segment_type_instance,EVENT_READ,EVENT_WRITE,EVENT_EXECUTE,SUBJECT_PROCESS,SUBJECT_THREAD,SUBJECT_EVENT,NUM_FILES,NUM_SUBJECTS
seg_1,VRangeType,0-360,189,0,0,0,0,0,171,189
seg_2,VRangeType,361-720,164,0,0,0,0,0,194,164
seg_3,VRangeType,721-1080,174,0,0,0,0,0,185,174
seg_4,VRangeType,1081-1440,181,0,0,0,0,0,178,181
seg_5,VRangeType,1441-1800,188,0,0,0,0,0,171,188
...
```
