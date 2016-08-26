#! /usr/bin/env python3

import sys, os, csv, json, math, logging
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.FileHandler(os.path.expanduser('~/adapt/ad/VIEW.log'), "w")
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)

consoleHandler = logging.StreamHandler()
consoleHandler.setFormatter(formatter)
log.addHandler(consoleHandler)

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

    def __init__(self, grem, vt, nq, fq):
        self.gremlin = grem
        self.view_type = vt
        self.node_ids_query = nq
        self.features_queries = fq
        self.feature_file = 'features/' + self.view_type + '.csv'
        self.score_file = 'scores/' + self.view_type + '.csv'
        self.total_nodes = 0

    def compute_view_and_save(self):
        # extract features
        keys = sorted(self.features_queries.keys())
        try:
            ids = self.gremlin.fetch_data(self.node_ids_query)
        except:
            log.exception("Exception at query:" + self.node_ids_query)
            return False

        self.total_nodes = len(ids)
        log.info("Found " + str(self.total_nodes) + " " + view_type + " nodes")
        if self.total_nodes == 0:
            return False
        log.info("Extracting features...")
        res = {}
        for k in keys:
            res[k] = [0.0] * len(ids)
            idx = 0
            for id in ids:
                try:
                    res[k][idx] = self.gremlin.fetch_data( self.features_queries[k].format(id=id) )[0]
                except:
                    log.exception("Exception at query:" + self.features_queries[k].format(id=id))
                    return False
                idx += 1

        log.info("Writing " + self.view_type + " view features to file: " + self.feature_file)
        f = open(self.feature_file, "w")
        f.write("id")
        for k in keys:
            f.write("," + k)
        f.write("\n")
        for i in range(0,self.total_nodes):
            f.write(str(ids[i]))
            for k in keys:
                f.write(',' + str(res[k][i]))
            f.write('\n')
        f.close()
        log.info("Writing " + self.feature_file + " Finished")
        return True

    def compute_anomaly_score(self):
        log.info("Computing anomaly scores...")
        os.system('./../osu_iforest/iforest.exe -i ' + self.feature_file + ' -o ' + self.score_file + ' -m 1 -t 100 -s 100')
        log.info("Anomaly scores written to " + self.score_file)

    def attach_scores_to_db(self, percentage = 5.0):
        cutoff = math.ceil(self.total_nodes * (percentage / 100.0))
        log.info("Attaching anomaly scores to top " + str(cutoff) + " anomalous nodes (threshold=" + str(percentage) + "%)...")
        max_score = 0
        max_id = 0
        cnt = 0
        try:
            with open(self.score_file, 'r') as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    self.gremlin.fetch_data("g.V({id}).property('anomalyType','{type}')".format(id=row['id'], type=self.view_type))
                    self.gremlin.fetch_data("g.V({id}).property('anomalyScore',{score})".format(id=row['id'], score=row['anomaly_score']))
                    if float(row['anomaly_score']) > max_score:
                        max_score = float(row['anomaly_score'])
                        max_id = row['id']
                    cnt = cnt + 1
                    if cnt >= cutoff:
                        break
                self.gremlin.fetch_data("g.V({id}).in('segment:includes').property('anomalyType','{type}')".format(id=max_id, type=self.view_type))
                self.gremlin.fetch_data("g.V({id}).in('segment:includes').property('anomalyScore',{score})".format(id=max_id, score=max_score))
                log.info('Anomaly score attachment done for view ' + self.view_type)
        except:
            log.exception("Exception attaching score")



if __name__ == '__main__':
    in_json = sys.argv[1]
    with open(in_json) as f:
        views = json.loads(f.read())
    with gremlin_query.Runner() as gremlin:
        for view_type in sorted(views.keys()):
            view_data = views[view_type]
            view = AnomalyView(gremlin, view_type, view_data['instance_set'], view_data['feature_set'])
            success = view.compute_view_and_save()
            if success == True:
                view.compute_anomaly_score()
                view.attach_scores_to_db()
