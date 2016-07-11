#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
import sys
import csv

url='http://localhost:8182/'
loop = asyncio.get_event_loop()
gc = GremlinClient(url=url, loop=loop)


def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data

        
# feature headers
f_h = ['writeBytes', 'readBytes', 'numWrites', 'numReads', 'ratioR2W']
# feature extraction queries
f_q = [	"inE('EDGE_EVENT_AFFECTS_NETFLOW in').outV().inE('EDGE_EVENT_AFFECTS_NETFLOW out').outV().has('eventType',21).values('size').sum()",
        "inE('EDGE_EVENT_AFFECTS_NETFLOW in').outV().inE('EDGE_EVENT_AFFECTS_NETFLOW out').outV().has('eventType',17).values('size').sum()",
        "inE('EDGE_EVENT_AFFECTS_NETFLOW in').outV().inE('EDGE_EVENT_AFFECTS_NETFLOW out').outV().has('eventType',21).count()",
        "inE('EDGE_EVENT_AFFECTS_NETFLOW in').outV().inE('EDGE_EVENT_AFFECTS_NETFLOW out').outV().has('eventType',17).count()"]

def extract_features_and_write_to_file(out_file):
    print("Writing features to file: " + out_file)
    f = open(out_file, "w")
    
    # write file header
    f.write("ident")
    for h in f_h:
        f.write("," + h)
    f.write("\n")
    
    idents = run_query("g.V().hasLabel('Entity-NetFlow').values('ident')")
    print("Found " + str(len(idents)) + " idents")
    for ident in idents:
        print("Extracting feature for: " + ident)
        f.write(ident)
        for q in f_q:
            res = run_query("g.V().has('ident','"+ ident +"')." + q)[0]
            f.write("," + str(res))
        # Ratio of read to write
        rSize = run_query("g.V().has('ident','"+ ident +"')." + f_q[1])[0]
        wSize = run_query("g.V().has('ident','"+ ident +"')." + f_q[0])[0]
        r2wRatio = 0 if(wSize==0) else (rSize/wSize)
        f.write("," + str(r2wRatio))
        f.write("\n")
    f.close()
    print("Writing Finished")

# main
out_file = sys.argv[1]
extract_features_and_write_to_file(out_file)

loop.run_until_complete(gc.close())
loop.close()
