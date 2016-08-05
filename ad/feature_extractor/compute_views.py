#! /usr/bin/env python3

import sys
import os
import csv
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

"""
    fields:
        view_type : string, specifies the name of the view
        node_ids_query : string, query to get ids of the prospective nodes
        features_queries : map<string, string>, set of feature names and
            it's corresponding gremlin query to compute that feature.
            Each query must be applicable to the individual nodes
            returned by node_ids_query above
"""
class AnomalyView:

    def __init__(self, vt, nq, fq):
        self.view_type = vt
        self.node_ids_query = nq
        self.features_queries = fq


    def compute_view_and_save(self):
        out_file = self.view_type + '_view_features.csv'

        print("Writing " + self.view_type + " view features to file: " + out_file)
        f = open(out_file, "w")

        # write feature headers
        f.write("id")
        for k in sorted(self.features_queries.keys()):
            f.write("," + k)
        f.write("\n")

        # extract and write features
        with gremlin_query.Runner() as gremlin:
            ids = gremlin.fetch_data(self.node_ids_query)
            print("Found " + str(len(ids)) + " " + view_type + " nodes")
            for id in ids:
                print("Extracting features for: " + str(id))
                f.write(str(id))
                for k in sorted(self.features_queries.keys()):
                    res = gremlin.fetch_data( self.features_queries[k].format(id=id) )[0]
                    f.write("," + str(res))
                f.write("\n")
        f.close()
        print("Writing to " + out_file + " Finished")


    def compute_anomaly_score(self):
        os.system('')


    def attach_scores_to_db(self):
        in_file = self.view_type + '_view_features.csv'
        with gremlin_query.Runner() as gremlin:
            with open(in_file, 'r') as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    gremlin.fetch_data("g.V({id}).property('anomalyScore',{score})".format(id=row['id'], score=row['anomaly_score']))
                print('Anomaly scores attached for view ' + self.view_type)



if __name__ == '__main__':
    # netflow view
    view_type = "netflow"
    node_list_query = "g.V().hasLabel('Entity-NetFlow').id()"
    features_queries = {
        "writeBytes"    : "g.V({id}).both().both().has('eventType',21).values('size').sum()",
        "readBytes"     : "g.V({id}).both().both().has('eventType',17).values('size').sum()",
        "numWrites"     : "g.V({id}).both().both().has('eventType',21).count()",
        "numReads"      : "g.V({id}).both().both().has('eventType',17).count()"
    }
    v1 = AnomalyView(view_type, node_list_query, features_queries)
    v1.compute_view_and_save()

    # process view
    view_type = "process"
    node_list_query = "g.V().has('subjectType',0).id()"
    features_queries = {
        'nFilesOpenDuringWrToNetFlow':
            "g.V({id}).as('process').both().both().has('eventType',21).as('writeNetflow').both().both().has(label,'Entity-NetFlow')"
            ".select('process').both().both().has('eventType',16).as('openFile').both().both().has(label,'Entity-File')"
            ".select('writeNetflow','openFile').by('startedAtTime').where('writeNetflow',gte('openFile')).count()",
        'numDistDstPortAccess':
            "g.V({id}).both().both().has('subjectType',4).both().both().has(label,'Entity-NetFlow').values('dstPort').dedup().count()",
        'numDistSrcPortAccess':
            "g.V({id}).both().both().has('subjectType',4).both().both().has(label,'Entity-NetFlow').values('srcPort').dedup().count()",
        'tBytesSentToNetFlow':
            "g.V({id}).both().both().has('eventType',21).as('event').both().both().has(label,'Entity-NetFlow').select('event').by('size').sum()",
        'tBytesRcvFromNetFlow':
            "g.V({id}).both().both().has('eventType',17).as('event').both().both().has(label,'Entity-NetFlow').select('event').by('size').sum()",
        'numWritesToNetFlow':
            "g.V({id}).both().both().has('eventType',21).both().both().has(label,'Entity-NetFlow').count()",
        'numReadsToNetFlow':
            "g.V({id}).both().both().has('eventType',17).both().both().has(label,'Entity-NetFlow').count()"
    }
    v2 = AnomalyView(view_type, node_list_query, features_queries)
    v2.compute_view_and_save()
