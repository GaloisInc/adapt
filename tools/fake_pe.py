#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
from kafka import KafkaConsumer

QUERY="g.V().count()"
bad_ls_expected_result=70960
# Q1="g.V().has('type','file').count()"
# Q2="g.V().has('type','netflow').count()"
# Q3="g.V().has('type','memory').count()"
# Q4="g.V().has('type','resource').count()"
# Q5="g.V().has('type','subject').count()"
# Q6="g.V().has('type','host').count()"
# Q7="g.V().has('type','agent').count()"
# QUERIES = [Q1,Q2,Q3,Q4,Q5,Q6,Q7]
QUERIES = []

if __name__ == '__main__':
    chan = KafkaConsumer('pe')
    msg  = next(chan)
    print ("Received a signal: ", msg)

    loop = asyncio.get_event_loop()
    gc = GremlinClient(loop=loop)

    for q in QUERIES:
        e = gc.execute(q)
        r = loop.run_until_complete(e)
        print(q, "\n\t", r[0].data[0])

    execute = gc.execute(QUERY)
    result = loop.run_until_complete(execute)
    print("total: ", result[0].data[0])
    print("success rate: ", result[0].data[0] / bad_ls_expected_result)

    loop.run_until_complete(gc.close())
