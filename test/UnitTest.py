import asyncio
from aiogremlin import GremlinClient

loop = asyncio.get_event_loop()
gc = GremlinClient(loop=loop)

def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data

# Unit test
# Test that all segment nodes has attached anomaly scores
n_seg_nodes = run_query("g.V().has('vertexType','segment').count()")[0]
n_nodes_attach = run_query("g.V().has('anomalyScore').count()")[0]
assert n_seg_nodes == n_nodes_attach, "Anomaly score not attached on all segment nodes"
print("Unit Test PASSED!")

loop.run_until_complete(gc.close())
loop.close()
