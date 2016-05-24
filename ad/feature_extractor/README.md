
## Feature Extraction
For Phase 2, we used a very simple segmentation criteria, VRangeType, which connects a range of vertices with a segment node. For example, vertex ranging from 1 to 10 is considered a segment and vertex range 11 to 20 is considererd as another segment and so on.

We defined some simple features that can be applied to any type of segment. The `extract_features.py`, which runs inside the TC-in-a-box, computes some of these predefined features for example counting number of read or write events. An example of feature extraction is given below:

```
$ python3 extract_features.py seg_spec_features.csv
$ cat seg_spec_features.csv
segment_id,segment_type,segment_type_instance,EVENT_READ,EVENT_WRITE,EVENT_EXECUTE,NUM_FILES,NUM_SUBJECTS
seg_0,VRangeType,1-11,0,1,0,3,2
seg_1,VRangeType,11-21,0,2,0,1,3
seg_2,VRangeType,21-31,0,0,0,3,4
seg_3,VRangeType,31-41,0,1,0,2,2
seg_4,VRangeType,41-51,0,1,0,2,4
seg_5,VRangeType,51-61,0,0,0,4,1
seg_6,VRangeType,61-71,0,4,0,1,5
seg_7,VRangeType,71-81,0,0,0,2,4
seg_8,VRangeType,81-91,0,0,0,1,1
seg_9,VRangeType,91-101,0,2,0,2,3
```
