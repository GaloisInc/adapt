#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient

QUERY="g.V().count()"

if __name__ == '__main__':
    loop = asyncio.get_event_loop()
    gc = GremlinClient(loop=loop)
    execute = gc.execute(QUERY)
    result = loop.run_until_complete(execute)

    print("result: ", result)

    loop.run_until_complete(gc.close())
