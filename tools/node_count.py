#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

QUERYV = "g.V().count()"
QUERYE = "g.E().count()"
Q1 = "g.V().has(label, 'Entity-File').count()"
Q2 = "g.V().has(label, 'Entity-NetFlow').count()"
Q3 = "g.V().has(label, 'Entity-Memory').count()"
Q4 = "g.V().has(label, 'Resource').count()"
Q5 = "g.V().has(label, 'Subject').count()"
Q6 = "g.V().has(label, 'Host').count()"
Q7 = "g.V().has(label, 'Agent').count()"
QUERIES = [Q1, Q2, Q3, Q4, Q5, Q6, Q7]


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
