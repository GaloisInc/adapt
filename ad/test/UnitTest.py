#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

# Unit test: Test that all Entity-NetFlow nodes has attached anomaly scores
with gremlin_query.Runner() as gremlin:
    num_netflow = gremlin.fetch_data("g.V().hasLabel('Entity-NetFlow').count()")[0]
    num_netflow_attached = gremlin.fetch_data("g.V().hasLabel('Entity-NetFlow').has('anomalyScore').count()")[0]
    assert num_netflow == num_netflow_attached, "Anomaly score not attached on all Entity-NetFlow nodes"
    print("Anomaly Score Test for NetFlow PASSED!")

    num_process = gremlin.fetch_data("g.V().has('subjectType',0).count()")[0]
    num_process_attached = gremlin.fetch_data("g.V().has('subjectType',0).has('anomalyScore').count()")[0]
    assert num_process == num_process_attached, "Anomaly score not attached on all Process nodes"
    print("Anomaly Score Test for Process PASSED!")
