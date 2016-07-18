#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

def get_data(result):
    data = []
    for r in result:
        if r.data:
            data = data + r.data
    return data

# feature headers
f_h = ['writeBytes', 'readBytes', 'numWrites', 'numReads', 'ratioR2W']
# feature extraction queries
f_q = [	"in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',21).values('size').sum()",
        "in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',17).values('size').sum()",
        "in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',21).count()",
        "in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',17).count()"]

def extract_features_and_write_to_file(out_file):
    print("Writing features to file: " + out_file)
    f = open(out_file, "w")
    
    # write file header
    f.write("id")
    for h in f_h:
        f.write("," + h)
    f.write("\n")
    with gremlin_query.Runner() as gremlin:
        ids = get_data(gremlin.fetch("g.V().hasLabel('Entity-NetFlow').id()"))
        print("Found " + str(len(ids)) + " Entity-NetFlow nodes")
        for id in ids:
            print("Extracting feature for: " + str(id))
            f.write(str(id))
            for q in f_q:
                res = get_data(gremlin.fetch("g.V(" + str(id) + ")." + q))[0]
                f.write("," + str(res))
            # Ratio of read to write
            rSize = get_data(gremlin.fetch("g.V("+ str(id) +")." + f_q[1]))[0]
            wSize = get_data(gremlin.fetch("g.V("+ str(id) +")." + f_q[0]))[0]
            r2wRatio = 0 if(wSize==0) else (rSize/wSize)
            f.write("," + str(r2wRatio))
            f.write("\n")
    f.close()
    print("Writing Finished")


if __name__ == '__main__':
    out_file = sys.argv[1]
    extract_features_and_write_to_file(out_file)
