#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
from math import floor
import sys
import csv

in_file = sys.argv[1]

loop = asyncio.get_event_loop()
gc = GremlinClient(loop=loop)

def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data

with open(in_file, 'r') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        run_query("g.V().has('ident','" + str(row['ident']) + "').property('anomalyScore'," + str(row['anomaly_score']) + ")")
    print('Anomaly scores attached')

loop.run_until_complete(gc.close())
loop.close()
