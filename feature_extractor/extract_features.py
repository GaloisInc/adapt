#! /usr/bin/env python3

import asyncio
from aiogremlin import GremlinClient
import sys


url='http://localhost:8182/'
loop = asyncio.get_event_loop()
gc = GremlinClient(url=url, loop=loop)


def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data


# feature headers
f_h = ['srcAddress', 'srcPort', 'dstAddress', 'dstPort']
# feature extraction queries
f_q = [	"values('srcAddress')",
        "values('srcPort')",
        "values('dstAddress')",
        "values('dstPort')"]


def process(q, ret):
    if ret == '':
        return 0
    if q == "values('srcAddress')" or q == "values('dstAddress')":
        s = ret.split('.')
        # convert first two bytes of ip into 16 bit number
        return (int(s[0]) * 256 + int(s[1]))
    return ret

def extract_features_and_write_to_file(out_file):
    print("Writing features to file: "+ out_file)
    f = open(out_file, "w")
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
            ret = run_query("g.V().has('ident','"+ ident +"')." + q)[0]
            res = process(q, ret)
            f.write("," + str(res))
        f.write("\n")
    f.close()
    print("Writing Finished")


# main
out_file = sys.argv[1]
extract_features_and_write_to_file(out_file)

loop.run_until_complete(gc.close())
loop.close()
