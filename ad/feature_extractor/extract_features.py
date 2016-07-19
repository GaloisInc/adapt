#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

# feature headers
f_h = ['writeBytes', 'readBytes', 'numWrites', 'numReads', 'ratioR2W']
# feature extraction queries
f_q = [	"g.V({id}).in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',21).values('size').sum()",
        "g.V({id}).in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',17).values('size').sum()",
        "g.V({id}).in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',21).count()",
        "g.V({id}).in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',17).count()"]

def extract_features_and_write_to_file(out_file):
    print("Writing features to file: " + out_file)
    f = open(out_file, "w")
    
    # write feature headers
    f.write("id")
    for h in f_h:
        f.write("," + h)
    f.write("\n")

    # write features
    with gremlin_query.Runner() as gremlin:
        ids = gremlin.fetch_data("g.V().hasLabel('Entity-NetFlow').id()")
        print("Found " + str(len(ids)) + " Entity-NetFlow nodes")
        for id in ids:
            print("Extracting features for: " + str(id))
            f.write(str(id))
            for q in f_q:
                res = gremlin.fetch_data( q.format(id=id) )[0]
                f.write("," + str(res))
            # Ratio of read to write
            rSize = gremlin.fetch_data( f_q[1].format(id=id) )[0]
            wSize = gremlin.fetch_data( f_q[0].format(id=id) )[0]
            r2wRatio = 0 if(wSize==0) else (rSize/wSize)
            f.write("," + str(r2wRatio))
            f.write("\n")
    f.close()
    print("Writing Finished")


if __name__ == '__main__':
    out_file = sys.argv[1]
    extract_features_and_write_to_file(out_file)
