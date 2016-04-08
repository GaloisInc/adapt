## Anomaly Detector

The Anomaly Detector called Isolation Forest accepts a set of feature vectors in the form of a csv file and assigns anomaly score to each feature vector. The anomaly score range is from 0 to 1, where scores close to 1 indicate higher chance of anomalousness.

## Instalation and Usage
```
$ cd osu_iforest
$ make
$ ./iforest.exe -h
Usage: ./iforest.exe [options]
Options:
        -i FILE, --infile=FILE
                Specify path to input data file. (Required).
        -o FILE, --outfile=FILE
                Specify path to output results file. (Required).
        -m COLS, --metacol=COLS
                Specify columns to preserve as meta-data. (Separated by ',' Use '-' to specify ranges).
        -t N, --ntrees=N
                Specify number of trees to build.
                Default value is 100.
        -s S, --sampsize=S
                Specify subsampling rate for each tree. (Value of 0 indicates to use entire data set).
                Default value is 2048.
        -d MAX, --maxdepth=MAX
                Specify maximum depth of trees. (Value of 0 indicates no maximum).
                Default value is 0.
        -H, --header
                Toggle whether or not to expect a header input.
                Default value is true.
        -v, --verbose
                Toggle verbose ouput.
                Default value is false.
        -w N, --windowsize=N
                specify window size.
                Default value is 512.
        -c N, --columns=N
                specify number of columns to use (0 indicates to use all columns).
                Default value is 0.
        -h, --help
                Print this help message and exit.
```

## Sample Input and Output

An example input file is given below where the rows represent the features correspond to segments.

```
segment_id,segment_type,segment_type_instance,EVENT_READ,EVENT_WRITE,EVENT_EXECUTE,SUBJECT_PROCESS,SUBJECT_THREAD,SUBJECT_EVENT,NUM_FILES,NUM_SUBJECTS
seg_1,VRangeType,0-360,189,0,0,0,0,0,171,189
seg_2,VRangeType,361-720,164,0,0,0,0,0,194,164
seg_3,VRangeType,721-1080,174,0,0,0,0,0,185,174
seg_4,VRangeType,1081-1440,181,0,0,0,0,0,178,181
seg_5,VRangeType,1441-1800,188,0,0,0,0,0,171,188
...
```

The corresponding output is given below:

```
$ iforest -i seg_spec_features.csv  -o seg_spec_features_score.csv -m 1-3 -t 100 -s 100
$ cat seg_spec_features_score.csv
segment_id,segment_type,segment_type_instance,EVENT_READ,EVENT_WRITE,EVENT_EXECUTE,SUBJECT_PROCESS,SUBJECT_THREAD,SUBJECT_EVENT,NUM_FILES,NUM_SUBJECTS,anomaly_score
seg_1,VRangeType,0-360,189,0,0,0,0,0,171,189,0.517051
seg_2,VRangeType,361-720,164,0,0,0,0,0,194,164,0.699086
seg_3,VRangeType,721-1080,174,0,0,0,0,0,185,174,0.524628
seg_4,VRangeType,1081-1440,181,0,0,0,0,0,178,181,0.551118
seg_5,VRangeType,1441-1800,188,0,0,0,0,0,171,188,0.451821
...
```
