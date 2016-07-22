#! /usr/bin/env python3

import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


def compute_view_netflow(out_file):
    # feature headers
    f_h = ['writeBytes', 'readBytes', 'numWrites', 'numReads', 'ratioR2W']
    # feature extraction queries
    f_q = [	"g.V({id}).both().both().has('eventType',21).values('size').sum()",
            "g.V({id}).both().both().has('eventType',17).values('size').sum()",
            "g.V({id}).both().both().has('eventType',21).count()",
            "g.V({id}).both().both().has('eventType',17).count()"]

    print("Writing netflow view features to file: " + out_file)
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

def compute_view_process(out_file):
    # feature headers
    f_h = ['nFilesOpenDuringWrToNetFlow', 'numDistDstPortAccess', 'numDistSrcPortAccess',
           'tBytesSentToNetFlow', 'tBytesRcvFromNetFlow', 'numWritesToNetFlow', 'numReadsToNetFlow', 'ratioR2WEvents']
    # feature extraction queries
    f_q = [ "g.V({id}).as('process').both().both().has('eventType',21).as('writeNetflow').both().both().has(label,'Entity-NetFlow')"
                    ".select('process').both().both().has('eventType',16).as('openFile').both().both().has(label,'Entity-File')"
                    ".select('writeNetflow','openFile').by('startedAtTime').where('writeNetflow',gte('openFile')).count()",
            "g.V({id}).both().both().has('subjectType',4).both().both().has(label,'Entity-NetFlow').values('dstPort').dedup().count()",
            "g.V({id}).both().both().has('subjectType',4).both().both().has(label,'Entity-NetFlow').values('srcPort').dedup().count()",
            "g.V({id}).both().both().has('eventType',21).as('event').both().both().has(label,'Entity-NetFlow').select('event').by('size').sum()",
            "g.V({id}).both().both().has('eventType',17).as('event').both().both().has(label,'Entity-NetFlow').select('event').by('size').sum()",
            "g.V({id}).both().both().has('eventType',21).both().both().has(label,'Entity-NetFlow').count()",
            "g.V({id}).both().both().has('eventType',17).both().both().has(label,'Entity-NetFlow').count()"]

    print("Writing process view features to file: " + out_file)
    f = open(out_file, "w")

    # write feature headers
    f.write("id")
    for h in f_h:
        f.write("," + h)
    f.write("\n")

    # write features
    with gremlin_query.Runner() as gremlin:
        max_time = gremlin.fetch_data("g.V().has('startedAtTime').values('startedAtTime').max()")[0]
        ids = gremlin.fetch_data("g.V().has('subjectType',0).id()")
        print("Found " + str(len(ids)) + " process nodes")
        for id in ids:
            print("Extracting features for: " + str(id))
            f.write(str(id))

#            started_at = gremlin.fetch_data("g.V({id}).values('startedAtTime')".format(id=id))
#            duration = (max_time - 0 if(len(started_at)==0) else started_at[0] )/1000000.0
#            f.write("," + str(duration))

            for q in f_q:
                res = gremlin.fetch_data( q.format(id=id) )[0]
                f.write("," + str(res))
            # Ratio of read to write
            rTimes = gremlin.fetch_data( f_q[6].format(id=id) )[0]
            wTimes = gremlin.fetch_data( f_q[5].format(id=id) )[0]
            r2wRatio = rTimes if(wTimes<=0) else (rTimes/wTimes)
            f.write("," + str(r2wRatio))
            f.write("\n")
    f.close()
    print("Writing Finished")

if __name__ == '__main__':
    compute_view_netflow(sys.argv[1])
    compute_view_process(sys.argv[2])
