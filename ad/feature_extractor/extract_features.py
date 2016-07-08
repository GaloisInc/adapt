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
f_h = ['eventType', 'srcAddress', 'srcPort', 'dstAddress', 'dstPort']
# feature extraction queries
f_q = [	"inE('EDGE_EVENT_AFFECTS_NETFLOW in').outV().inE('EDGE_EVENT_AFFECTS_NETFLOW out').outV().values('eventType')",
        "values('srcAddress')",
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
    print("Writing features to file: " + out_file)
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

def convert_to_binary_features(in_file, out_file):
    with open(in_file, 'r') as csvfile:
        reader = csv.DictReader(csvfile)
        data = {}
        for row in reader:
            for header, value in row.items():
                try:
                    data[header].append(value)
                except KeyError:
                    data[header] = [value]
        for h in f_h:
            data[h] = list(set(data[h]))
        f = open(out_file, "w")
        f.write("ident")
        for h in f_h:
            for item in data[h]:
                f.write("," + h + "_" + str(item))
        f.write("\n")
        csvfile.seek(0)
        reader = csv.DictReader(csvfile)
        for row in reader:
            f.write(row['ident'])
            for h in f_h:
                for item in data[h]:
                    f.write("," + str(1 if(item == row[h]) else 0))
            f.write("\n")


# main
out_file = sys.argv[1]
temp_file = 'temp.csv'
extract_features_and_write_to_file(temp_file)
convert_to_binary_features(temp_file, out_file)
loop.run_until_complete(gc.close())
loop.close()
