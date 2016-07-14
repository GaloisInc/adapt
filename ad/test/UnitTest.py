#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

# Unit test: Test that all Entity-NetFlow nodes has attached anomaly scores
with gremlin_query.Runner() as gremlin:
    n_net_ids = gremlin.fetch_data("g.V().hasLabel('Entity-NetFlow').count()")[0]
    n_nodes_attach = gremlin.fetch_data("g.V().has('anomalyScore').count()")[0]
    assert n_net_ids == n_nodes_attach, "Anomaly score not attached on all Entity-NetFlow nodes"
    print("Anomaly Score Test PASSED!")
