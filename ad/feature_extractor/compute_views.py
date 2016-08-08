#! /usr/bin/env python3

import sys, os, csv, json, math
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
        self.feature_file = 'features/' + self.view_type + '.csv'
        self.score_file = 'scores/' + self.view_type + '.csv'
        self.total_nodes = 0

    def compute_view_and_save(self):
        print("\nWriting " + self.view_type + " view features to file: " + self.feature_file)
        f = open(self.feature_file, "w")

        # write feature headers
        f.write("id")
        for k in sorted(self.features_queries.keys()):
            f.write("," + k)
        f.write("\n")

        # extract and write features
        with gremlin_query.Runner() as gremlin:
            ids = gremlin.fetch_data(self.node_ids_query)
            cnt = 0
            self.total_nodes = len(ids)
            print("Found " + str(self.total_nodes) + " " + view_type + " nodes")
            print("Extracting features...")
            for id in ids:
                f.write(str(id))
                for k in sorted(self.features_queries.keys()):
                    res = gremlin.fetch_data( self.features_queries[k].format(id=id) )[0]
                    f.write("," + str(res))
                f.write("\n")
                cnt = cnt + 1
                if cnt % math.ceil(self.total_nodes/10) == 0 and cnt != self.total_nodes:
                    print("%.2f%% done" % round(cnt*100/self.total_nodes, 2))
            if self.total_nodes % 10 != 0:
                print("100.00% done")
        f.close()
        print("Writing " + self.feature_file + " Finished")


    def compute_anomaly_score(self):
        print("Computing anomaly scores...")
        with open(self.feature_file) as f:
            for i, l in enumerate(f):
                if i > 0:
                    break
        if(i > 0):
            os.system('./../osu_iforest/iforest.exe -i ' + self.feature_file + ' -o ' + self.score_file + ' -m 1 -t 100 -s 100')
            print("Anomaly scores written to " + self.score_file)
            return True
        else:
            print("No features found to score")
            return False


    def attach_scores_to_db(self, percentage = 5.0):
        cutoff = math.ceil(self.total_nodes * (percentage / 100.0))
        print("Attaching anomaly scores to top " + str(cutoff) + " anomalous nodes (" + str(percentage) + "%)...")
        cnt = 0
        with gremlin_query.Runner() as gremlin:
            with open(self.score_file, 'r') as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    gremlin.fetch_data("g.V({id}).property('anomalyType',{type})".format(id=row['id'], type=self.view_type))
                    gremlin.fetch_data("g.V({id}).property('anomalyScore',{score})".format(id=row['id'], score=row['anomaly_score']))
                    cnt = cnt + 1
                    if(cnt >= cutoff)
                        break
                print('Anomaly score attachment done for view ' + self.view_type)



if __name__ == '__main__':
    in_json = sys.argv[1]
    with open(in_json) as f:
        views = json.loads(f.read())
    for view_type,view_data in views.items():
        view = AnomalyView(view_type, view_data['instance_set'], view_data['feature_set'])
        view.compute_view_and_save()
        success = view.compute_anomaly_score()
        if success == True:
            view.attach_scores_to_db()
