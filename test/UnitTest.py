#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

loop = asyncio.get_event_loop()
gc = GremlinClient(loop=loop)

def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data

# Unit test
# Test that all Entity-NetFlow nodes has attached anomaly scores
n_net_idents = run_query("g.V().hasLabel('Entity-NetFlow').values('ident')")[0]
n_nodes_attach = run_query("g.V().has('anomalyScore').count()")[0]
assert len(n_net_idents) == n_nodes_attach, "Anomaly score not attached on all Entity-NetFlow nodes"
print("Anomaly Score Test PASSED!")

loop.run_until_complete(gc.close())
loop.close()
