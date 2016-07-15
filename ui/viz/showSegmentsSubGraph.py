#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

QUERYV = "g.V().hasLabel('Segment')"

QUERYE = "g.V({}).as('a').out('segment:includes').out().in('segment:includes').where(neq('a'))"

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

    vertices = gremlin.fetch(QUERYV)[0].data
    print("total segments: ", len(vertices))

    for v in vertices:
        print(v)
        print(">>>", gremlin.fetch(QUERYE.format(v['id'])))
        print("")





    gremlin.close()
