#! /usr/bin/env python3

import sys, os, csv, json
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
            print("Extracting features...")
            for id in ids:
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
    in_json = sys.argv[1]
    with open(in_json) as f:
        views = json.loads(f.read())
    for view_type,view_data in views.items():
        view = AnomalyView(view_type, view_data['instance_set'], view_data['feature_set'])
        view.compute_view_and_save()
