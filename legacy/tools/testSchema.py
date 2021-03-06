#! /usr/bin/env python3

import sys
import asyncio
from aiogremlin import GremlinClient, GremlinServerError

QSuccess = "graph.addVertex('Entity')" # should insert
QFail = "graph.addVertex('badLabel')" # should fail to insert

if __name__ == '__main__':
    loop = asyncio.get_event_loop()
    gc = GremlinClient(loop=loop)

    try:
            e = gc.execute(QSuccess)
            r = loop.run_until_complete(e)
            print("Successfully inserted vertex of type Entity as expected")
    except:
        print("Should be able to insert vertex of type 'Entity', schema creation failed: ", sys.exc_info()[0])

    try:
        e = gc.execute(QFail)
        r = loop.run_until_complete(e)
        print("Schema creation failed, successful insertion of vertex 'badLabel'")
    except GremlinServerError:
        print("Could not insert vertex of type 'badLabel' as expected")

    loop.run_until_complete(gc.close())
