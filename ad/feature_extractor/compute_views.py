#! /usr/bin/env python3

import sys, os, csv, json, math, logging, kafka
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.FileHandler(os.path.expanduser('~/adapt/ad/AD.log'))
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)

consoleHandler = logging.StreamHandler()
consoleHandler.setFormatter(formatter)
log.addHandler(consoleHandler)

# varibale bindings for gremlin queries
bindings = {
    'ETYPE':'eventType',
    'STYPE':'subjectType',
    'SIZE':'size',
    'STIME':'startedAtTime',
    'DPORT':'dstPort',
    'SPORT':'srcPort',
    'DADDRESS':'dstAddress',

    'PROCESS':0,
    'EVENT':4,

    'CHECK_FILE_ATTRIBUTES':3,
    'CLOSE':5,
    'CONNECT':6,
    'EXECUTE':9,
    'UNLINK':12,
    'MODIFY_FILE_ATTRIBUTES':14,
    'OPEN':16,
    'READ':17,
    'RENAME':20,
    'WRITE':21,
    'EXIT':36,

    'E_E_A_F_I':'EDGE_EVENT_AFFECTS_FILE in',
    'E_E_A_F_O':'EDGE_EVENT_AFFECTS_FILE out',
    'E_E_A_N_I':'EDGE_EVENT_AFFECTS_NETFLOW in',
    'E_E_A_N_O':'EDGE_EVENT_AFFECTS_NETFLOW out',
    'E_E_G_B_S_I':'EDGE_EVENT_ISGENERATEDBY_SUBJECT in',
    'E_E_G_B_S_O':'EDGE_EVENT_ISGENERATEDBY_SUBJECT out'
}

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

    def compute_view_and_save(self):
        # extract features
        keys = sorted(self.features_queries.keys())
        QUERY = self.node_ids_query + ";if(IDS!=[]){"
        for i in range(0,len(keys)):
            if type(self.features_queries[keys[i]]) == type(dict()):
                QUERY += "yf{}=".format(i) + self.features_queries[keys[i]]['first'] + ";"
                if 'second' in self.features_queries[keys[i]].keys():
                    QUERY += "ys{}=".format(i) + self.features_queries[keys[i]]['second'] + ";"
                if 'third' in self.features_queries[keys[i]].keys():
                    QUERY += "yt{}=".format(i) + self.features_queries[keys[i]]['third'] + ";"
            else:
                QUERY += "x{}=".format(i) + self.features_queries[keys[i]] + ";"
        QUERY += "[IDS"
        for i in range(0,len(keys)):
            if type(self.features_queries[keys[i]]) == type(dict()):
                QUERY += ",[yf{}.toList()".format(i)
                if 'second' in self.features_queries[keys[i]].keys():
                    QUERY += ",ys{}.toList()".format(i)
                if 'third' in self.features_queries[keys[i]].keys():
                    QUERY += ",yt{}.toList()".format(i)
                QUERY += "]"
            else:
                QUERY += ",x{}.toList()".format(i)
        QUERY += "]}else [];"

        log.info("Extracting features for " + self.view_type + "...")
        try:
            result = self.gremlin.fetch_data(QUERY, bindings=bindings)
        except:
            log.exception("Exception at query:" + QUERY)
            return False

        if result == []:
            log.info("Found 0 " + self.view_type + " nodes")
            return False

        log.info("Writing " + self.view_type + " view features to file: " + self.feature_file)
        f = open(self.feature_file, "w")
        f.write("id")
        for k in keys:
            f.write("," + k)
        f.write("\n")
        for i in range(0,len(result[0])):
            f.write(str(result[0][i]))
            j = 1
            for k in keys:
                res = None
                if type(self.features_queries[k]) == type(dict()):
                    if self.features_queries[k]['operator'] == 'subTime':
                        res = (result[j][0][i] - result[j][1][i]) / 1.0e6
                    elif self.features_queries[k]['operator'] == 'div(SubTime)':
                        res = result[j][0][i] / ((result[j][1][i] - result[j][2][i]) / 1.0e6)
                    elif self.features_queries[k]['operator'] == '(SubTime)div':
                        res = ((result[j][0][i] - result[j][1][i]) / 1.0e6) / result[j][2][i]
                    else:
                        log.info("Unrecognized operator: " + self.features_queries[k]['operator'])
                else:
                    res = result[j][i]
                f.write(',' + str(res))
                j += 1
            f.write('\n')
        f.close()
        log.info("Writing " + self.feature_file + " Finished")
        return True

    def compute_anomaly_score(self):
        log.info("Computing anomaly scores...")
        os.system('./../osu_iforest/iforest.exe -i ' + self.feature_file + ' -o ' + self.score_file + ' -m 1 -t 100 -s 100')
        log.info("Anomaly scores written to " + self.score_file)

    def attach_scores_to_db(self, percentage = 5.0):
        with open(self.score_file) as f:
            for i, l in enumerate(f):
                pass
        total_nodes = i - 1
        cutoff = math.ceil(total_nodes * (percentage / 100.0))
        max_score = 0
        max_id = 0
        cnt = 0
        QUERY = ""
        binds = {'atype':'anomalyType', 'ascore':'anomalyScore', 'sin':'segment:includes'}
        with open(self.score_file, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                QUERY += "x={id};t='{type}';g.V(x).property(atype,t);".format(id=row['id'], type=self.view_type)
                QUERY += "x={id};s={score};g.V(x).property(ascore,s);".format(id=row['id'], score=row['anomaly_score'])
                log.info("Adding anomaly scores to id " + row['id'] + " (" + self.view_type + ", " + row['anomaly_score'] + ")")
                if float(row['anomaly_score']) > max_score:
                    max_score = float(row['anomaly_score'])
                    max_id = row['id']
                cnt = cnt + 1
                if cnt >= cutoff:
                    break
            QUERY += "x={id};t='{type}';g.V(x).in(sin).property(atype,t);".format(id=max_id, type=self.view_type)
            QUERY += "x={id};s={score};g.V(x).in(sin).property(ascore,s);".format( id=max_id, score=max_score)
            log.info("Adding anomaly scores to segment id " + str(max_id) + " (" + self.view_type + ", " + str(max_score) + ")")
        log.info("size of QUERY = " + str(len(QUERY)))
        log.info("Attaching anomaly scores to top " + str(cutoff) + " anomalous nodes (threshold=" + str(percentage) + "%)...")
        try:
            self.gremlin.fetch_data(QUERY, binds)
        except:
            log.exception("Exception attaching score")
        log.info('Anomaly score attachment done for view ' + self.view_type)



if __name__ == '__main__':
    in_json = sys.argv[1]
    producer = kafka.KafkaProducer(bootstrap_servers=['localhost:9092'])
    with open(in_json) as f:
        views = json.loads(f.read())
    with gremlin_query.Runner() as gremlin:
        i = 1
        for view_type in sorted(views.keys()):
            producer.send("ad-log", bytes('Processing Anomaly View ' + view_type + ' (' + str(i) + '/' + str(len(views.keys())) + ')', encoding='utf-8'))
            view_data = views[view_type]
            view = AnomalyView(gremlin, view_type, view_data['instance_set'], view_data['feature_set'])
            success = view.compute_view_and_save()
            if success == True:
                view.compute_anomaly_score()
                view.attach_scores_to_db()
            i += 1
