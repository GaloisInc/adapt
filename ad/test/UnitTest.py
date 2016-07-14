#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

def get_data(result):
    data = []
    for r in result:
        if r.data:
            data = data + r.data
    return data

# Unit test: Test that all Entity-NetFlow nodes has attached anomaly scores
with gremlin_query.Runner() as gremlin:
    n_net_ids = get_data(gremlin.fetch("g.V().hasLabel('Entity-NetFlow').count()"))[0]
    n_nodes_attach = get_data(gremlin.fetch("g.V().has('anomalyScore').count()"))[0]
    assert n_net_ids == n_nodes_attach, "Anomaly score not attached on all Entity-NetFlow nodes"
    print("Anomaly Score Test PASSED!")
