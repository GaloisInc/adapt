#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

Q1 = "graph.addVertex('Entity')" # should insert
Q2 = "graph.addVertex('badLabel')" # should fail to insert
QUERIES = [Q1, Q2]

if __name__ == '__main__':
	loop = asyncio.get_event_loop()
	gc = GremlinClient(loop=loop)

	for q in QUERIES:
		e = gc.execute(q)
		r = loop.run_until_complete(e)
		print(q, "\n\t", r)

	loop.run_until_complete(gc.close())
