#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
import sys
import os
import time
sys.path.append(os.path.expanduser('~/adapt/pylib'))
from titanDB import TitanClient 

QUERYV = "g.V().count()"
QUERYE = "g.E().count()"
QUERIES = [
    ('Entity-File',"g.V().has(label, 'Entity-File').count()"),
    ('Entity-NetFlow',"g.V().has(label, 'Entity-NetFlow').count()"),
    ('Entity-Memory',"g.V().has(label, 'Entity-Memory').count()"),
    ('Resources',"g.V().has(label, 'Resource').count()"),
    ('Subjects',"g.V().has(label, 'Subject').count()"),
    ('Hosts',"g.V().has(label, 'Host').count()"),
    ('Agents',"g.V().has(label, 'Agent').count()"),
    ('Segments',"g.V().has(label, 'Segment').count()"),
    ('Vertices',"g.V().count()"),
    ('Edges',"g.E().count()"),
]


class GremlinQueryRunner:

    def __init__(self):
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(loop=self.loop)

    def fetch(self, query):
        return self.loop.run_until_complete(self.gc.execute(query))

    def fetch_many(self, queries, sem_num):
        sem=asyncio.Semaphore(sem_num)
        @asyncio.coroutine
        def fetch(name,query): 
            with (yield from sem):
                print("Starting: %s" % name)
                t1 = time.time()
                result = yield from self.gc.execute(query)
                t2 = time.time()
                print("Finished: %s in %fs" % (name,t2-t1))
                return (name,query,result)
            
        
        jobs = [fetch(name,query) for (name,query) in queries]
        results = self.loop.run_until_complete(asyncio.gather(*jobs))
        return results

    def close(self):
        self.loop.run_until_complete(self.gc.close())


if __name__ == '__main__':

    gremlin = TitanClient()
    
    if len(sys.argv) > 1:
        processors = int(sys.argv[1])
    else:
        processors = 1

    result = gremlin.fetch_many(QUERIES,processors)

    for (n,q,r) in result:
        print(n,"\t",q,"\n\t",r)


    gremlin.close()
