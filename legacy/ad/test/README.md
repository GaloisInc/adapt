### Affects on database
After anomaly detection module is done, anomaly score is attached into corresponding nodes i.e. file/process/netflow. Anomaly score from different views are attached into the same node as a list of scores. For example:

```
gremlin> g.V().has('subjectType',0).has('anomalyScore').limit(1).valueMap('anomalyType','anomalyScore')
==>[anomalyScore:[0.867162], anomalyType:[process_connected_with_netflow]]
```

When there are more than one scores in the 'anomalyScore' list, the order of the 'anomalyType' determines the type of the score. 

Here is a query to get top 10 anomalous nodes:

```
gremlin> g.V().has('anomalyScore').as('anomalyNode').order().by(values('anomalyScore').max(),decr).limit(10).as('maxScore').select('anomalyNode','maxScore').by(id).by(values('anomalyScore').max())
==>[anomalyNode:274488, maxScore:0.867162]
==>[anomalyNode:163976, maxScore:0.776461]
==>[anomalyNode:69688, maxScore:0.764868]
==>[anomalyNode:241800, maxScore:0.758556]
==>[anomalyNode:8248, maxScore:0.757831]
==>[anomalyNode:24800, maxScore:0.757831]
==>[anomalyNode:57592, maxScore:0.7573]
==>[anomalyNode:147552, maxScore:0.7573]
==>[anomalyNode:8167520, maxScore:0.73702]
==>[anomalyNode:372960, maxScore:0.713892]
```

### Run Anomaly Detection Module (Sample output from 'youtube_ie_update.bin')

```
$ ./start.sh
...
...
...

Writing files_connected_with_netflow view features to file: features/files_connected_with_netflow.csv
Found 905 files_connected_with_netflow nodes
Extracting features...
10.06% done
20.11% done
30.17% done
40.22% done
50.28% done
60.33% done
70.39% done
80.44% done
90.50% done
100.00% done
Writing features/files_connected_with_netflow.csv Finished
Computing anomaly scores...
# Trees     = 100
# Samples   = 100
Original Data Dimension: 905,9
Anomaly scores written to scores/files_connected_with_netflow.csv
Attaching anomaly scores to top 46 anomalous nodes (threshold=5.0%)...
Anomaly score attachment done for view files_connected_with_netflow

Writing netflow view features to file: features/netflow.csv
Found 268 netflow nodes
Extracting features...
10.07% done
20.15% done
30.22% done
40.30% done
50.37% done
60.45% done
70.52% done
80.60% done
90.67% done
100.00% done
Writing features/netflow.csv Finished
Computing anomaly scores...
# Trees     = 100
# Samples   = 100
Original Data Dimension: 268,7
Anomaly scores written to scores/netflow.csv
Attaching anomaly scores to top 14 anomalous nodes (threshold=5.0%)...
Anomaly score attachment done for view netflow

...
...
...

```

### Manually Building (builds automatically but only if needed)

```
$ cd ../osu_iforest
$ make
```

### Test Only Anomaly Detection Module (No interaction with other module)

`$ ./start.sh`
