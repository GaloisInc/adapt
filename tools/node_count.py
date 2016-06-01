#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

QUERYV="g.V().count()"
QUERYE="g.E().count()"
Q1="g.V().has(label,'File').count()"
Q2="g.V().has(label,'Entity-NetFlow').count()"
Q3="g.V().has(label,'Entity-Memory').count()"
Q4="g.V().has(label,'Resource').count()"
Q5="g.V().has(label,'Subject').count()"
Q6="g.V().has(label,'Host').count()"
Q7="g.V().has(label,'Agent').count()"
QUERIES = [Q1,Q2,Q3,Q4,Q5,Q6,Q7]

if __name__ == '__main__':
    loop = asyncio.get_event_loop()
    gc = GremlinClient(loop=loop)

    for q in QUERIES:
        e = gc.execute(q)
        r = loop.run_until_complete(e)
        print(q, "\n\t", r)

    execute = gc.execute(QUERYV)
    result = loop.run_until_complete(execute)
    print("total nodes: ", result)

    execute = gc.execute(QUERYE)
    result = loop.run_until_complete(execute)
    print("total edges: ", result)

    loop.run_until_complete(gc.close())
