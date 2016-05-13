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
    reader = csv.reader(csvfile, delimiter=',')
    for row in reader:
        if row[0] != 'segment_id':
            run_query("g.V().has('vertexType','segment').hasLabel('" + str(row[0]) + "')[0].property('anomalyScore','" + str(row[8]) + "')")
    print('Anomaly score attach finished')

loop.run_until_complete(gc.close())
loop.close()
