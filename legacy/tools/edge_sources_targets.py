#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
import cdm.enums

QUERY = "g.V().hasLabel('EDGE_{}').limit(5000).{}.label().dedup().sort()"

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

    for edge in cdm.enums.Edge:
        for direction in ['in()', 'out()']:
            print("EDGE_{} {}:".format(edge.name, direction))
            print(gremlin.fetch(QUERY.format(edge.name, direction))[0].data)
        print()

    gremlin.close()
