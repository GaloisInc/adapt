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
	('Nodes',QUERYV),
	('Edges',QUERYE)
]



if __name__ == '__main__':

    gremlin = TitanClient()
    
    if len(sys.argv) > 1:
        processors = int(sys.argv[1])
    else:
        processors = 1

    result1 = gremlin.execute_many(QUERIES,processors)
    for (n,q,r) in result1:
        print(n,"\t",q,"\n\t",r)

    query = 'g.V().has(label,x).count()'
	params = [{'x':'Entity-File'},
			  {'x':'Entity-NetFlow'},
			  {'x':'Entity-Memory'},
			  {'x':'Resources'},
			  {'x':'Subjects'},
			  {'x':'Hosts'},
			  {'x':'Agents'},
			  {'x':'Segments'}]
    result2 = gremlin.execute_many_params(query,params,processors)
    for (b,r) in result2:
        print(query,"\t",b,"\n\t",r)
									 




    gremlin.close()
