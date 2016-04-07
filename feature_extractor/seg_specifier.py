import asyncio
from aiogremlin import GremlinClient
from math import floor

loop = asyncio.get_event_loop()
gc = GremlinClient(loop=loop)

def run_query(q):
	execute = gc.execute(q)
	result = loop.run_until_complete(execute)
	return result[0].data[0]

nodes = run_query("g.V().count()")
loop.run_until_complete(gc.close())
loop.close()

print('segment_id,segment_type,segment_type_instance')

num_segments = 100
inc = floor(nodes/num_segments)
sid = 1
st = 0
for end in range(inc, nodes, inc):
	st_end = str(st) + "-" + str(end)
	print('seg_' + str(sid) + ',VRangeType,' + st_end)
	sid = sid + 1
	st = end + 1

