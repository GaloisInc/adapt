#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

QUERYV = "g.V().count()"
QUERYE = "g.E().count()"
QUERIES = [
    "g.V().has(label, 'Entity-File').count()",
    "g.V().has(label, 'Entity-NetFlow').count()",
    "g.V().has(label, 'Entity-Memory').count()",
    "g.V().has(label, 'Resource').count()",
    "g.V().has(label, 'Subject').count()",
    "g.V().has(label, 'Host').count()",
    "g.V().has(label, 'Agent').count()",
    "g.V().has(label, 'Segment').count()",
]


class GremlinQueryRunner:

    def __init__(self):
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(loop=self.loop)

    def fetch(self, query):
        return self.loop.run_until_complete(self.gc.execute(query))

    def close(self):
        self.loop.run_until_complete(self.gc.close())


if __name__ == '__main__':

    gremlin = GremlinQueryRunner()

    for q in QUERIES:
        print(q, "\n\t", gremlin.fetch(q))

    print("total nodes: ", gremlin.fetch(QUERYV))

    print("total edges: ", gremlin.fetch(QUERYE))

    gremlin.close()
