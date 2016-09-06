#! /usr/bin/env python3

import sys, os, csv, json, math, logging, kafka, statistics, numpy
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

    'F_A_E_I':'EDGE_FILE_AFFECTS_EVENT in',
    'F_A_E_O':'EDGE_FILE_AFFECTS_EVENT out',
    'E_A_F_I':'EDGE_EVENT_AFFECTS_FILE in',
    'E_A_F_O':'EDGE_EVENT_AFFECTS_FILE out',
    'N_A_E_I':'EDGE_NETFLOW_AFFECTS_EVENT in',
    'N_A_E_O':'EDGE_NETFLOW_AFFECTS_EVENT out',
    'E_A_N_I':'EDGE_EVENT_AFFECTS_NETFLOW in',
    'E_A_N_O':'EDGE_EVENT_AFFECTS_NETFLOW out',
    'E_G_B_S_I':'EDGE_EVENT_ISGENERATEDBY_SUBJECT in',
    'E_G_B_S_O':'EDGE_EVENT_ISGENERATEDBY_SUBJECT out'
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
                res = 0
                try:
                    if type(self.features_queries[k]) == type(dict()):
                        if self.features_queries[k]['operator'] == 'subTime':
                            res = (result[j][0][i] - result[j][1][i]) / 1.0e6
                        elif self.features_queries[k]['operator'] == 'RELUTime':
                            res = (result[j][0][i] - result[j][1][i]) / 1.0e6
                            if res < 0:
                                res = 0
                        elif self.features_queries[k]['operator'] == 'div(SubTime)':
                            res = result[j][0][i] / ((result[j][1][i] - result[j][2][i]) / 1.0e6)
                        elif self.features_queries[k]['operator'] == '(SubTime)div':
                            res = ((result[j][0][i] - result[j][1][i]) / 1.0e6) / result[j][2][i]
                        else:
                            log.info("Unrecognized operator: " + self.features_queries[k]['operator'])
                    else:
                        res = result[j][i]
                except:
                    log.exception("Exception: i=" + str(i) + ", j=" + str(j) + ", k=" + k)
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

    def attach_scores_to_db(self, view_stats, percentage = 5.0):
        with open(self.score_file) as f:
            for i, l in enumerate(f):
                pass
        total_nodes = i
        view_stats.number_nodes = total_nodes
        cutoff = math.ceil(total_nodes * (percentage / 100.0))
        max_score = 0
        max_id = 0
        max_feature = None
        cnt = 0
        QUERY = ""
        binds = {'atype':'anomalyType', 'ascore':'anomalyScore', 'sin':'segment:includes', 'afeature':'anomalyFeature'}
        with open(self.score_file, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                QUERY += "x={id};t='{type}';s={score};g.V(x).property(atype,t).next();g.V(x).property(ascore,s).next();".format(id=row['id'], type=self.view_type, score=row['anomaly_score'])
                feature = "["
                for k in sorted(row.keys()):
                    if k != 'id' and k != 'anomaly_score':
                        if feature != "[":
                            feature += ","
                        feature += k + ":" + str(row[k])
                feature += "]"
                feature += "Rnk:" + str(cnt+1) + "/" + str(total_nodes) + feature
                QUERY += "f='{feat}';g.V(x).property(afeature,f).next();".format(feat=feature)
                log.info("Adding anomaly scores to id " + row['id'] + " (" + self.view_type + ", " + row['anomaly_score'] + ")")
                if float(row['anomaly_score']) > max_score:
                    max_score = float(row['anomaly_score'])
                    max_id = row['id']
                    max_feature = feature
                cnt = cnt + 1
                if cnt >= cutoff:
                    break
            QUERY += "x={id};t='{type}';s={score};f='{feat}';IDS=g.V(x).in(sin).id().toList().toArray();if(IDS!=[]){{g.V(IDS).property(atype,t).next();g.V(IDS).property(ascore,s).next();g.V(IDS).property(afeature,f).next();}};".format(id=max_id, type=self.view_type, score=max_score, feat=max_feature)
            log.info("size of QUERY = " + str(len(QUERY)))
            log.info("Attaching anomaly scores to top " + str(cutoff) + " anomalous nodes (threshold=" + str(percentage) + "%)...")
            try:
                self.gremlin.fetch_data(QUERY, binds)
                view_stats.number_nodes_attached = cnt
            except:
                log.exception("Exception attaching score")
            log.info('Anomaly score attachment done for view ' + self.view_type)


class ViewStats:
    def __init__(self, vt, run_time_dir):
        self.view_type = vt
        self.feature_file_path = run_time_dir + '/features/' + self.view_type + '.csv'
        self.scores_file_path  = run_time_dir + '/scores/'   + self.view_type + '.csv'
        self.number_nodes = 0
        self.number_nodes_attached = 0
        self.score_range_min = -1
        self.score_range_max = -1
        self.value_list_for_features = {}
        self.histograms_for_features = {}
        self.features = []
        self.feature_means = {}
        self.feature_stdevs = {}

    def set_score_range(self):
        #print('setting score range...')
        with open(self.scores_file_path, 'r') as csvfile:
            lines = csvfile.readlines()
            i = 0;
            for line in lines:
                if i == 0:
                    pass
                    #print('skipping header {0}'.format(line))
                else:
                    parts = line.split(',')
                    score = parts[len(parts) - 1]
                    self.note_anomaly_score(score)
                i = i + 1
            #print('range of scores {0} - {1}'.format(self.score_range_min, self.score_range_max))

    def set_value_list_for_features(self):
        #print('setting value list for features...')
        with open(self.feature_file_path, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                features = row.keys()
                #print(features)
                #print(row)
                for feature in features:
                    if feature != 'id':
                        if not(feature in self.value_list_for_features):
                            self.value_list_for_features[feature] = []
                        list_for_feature = self.value_list_for_features[feature]
                        list_for_feature.append(float(row[feature]))
            self.features = list(self.value_list_for_features.keys())
            self.features.sort()
            #print('values for features {0}'.format(self.value_list_for_features))
            
    def get_stats_info(self):
        INFO="###########################################\nstatistics for view "+self.view_type+'\n'
        INFO+="###########################################\n"
        INFO+="# nodes         " + "{0}".format(self.number_nodes) + '\n'
        INFO+="# nodes attached" + "{0}".format(self.number_nodes_attached) + '\n'
        INFO+="features: " 
        #INFO+=', '.join(self.features) + '\n'
        for f in self.features:
            INFO+="\n\t" + f + "\tmean " + "{0:.2f}".format(self.feature_means[f]) + "\tstdev " + "{0:.2f}".format(self.feature_stdevs[f])
        INFO+="\n\nFEATURE HISTOGRAMS"
        for f in self.features:
            INFO+="\n\t" + f + "\t:  " + "{0}".format(self.histograms_for_features[f])
        INFO+="\n\nscore range - min {0} , max {1}\n".format(self.score_range_min, self.score_range_max)
        INFO+="\n"
        return INFO
                  
    def note_anomaly_score(self, score):
        score_as_float = float(score)
        if (self.score_range_min == -1):
            self.score_range_min = score_as_float
        else:
            if (score_as_float < self.score_range_min):
                self.score_range_min = score_as_float
        if (self.score_range_max == -1):
            self.score_range_max = score_as_float
        else:
            if (score_as_float > self.score_range_max):
                self.score_range_max = score_as_float

    def compute_feature_means(self):
        #print('computing means...')
        if (not(bool(self.value_list_for_features))):
            self.set_value_list_for_features()
        
        for feature in self.features:
            values = self.value_list_for_features[feature]
            mean = statistics.mean(values)
            self.feature_means[feature] = mean
        
    def compute_feature_stdevs(self):
        #print('computing standard deviation')
        if (not(bool(self.value_list_for_features))):
            self.set_value_list_for_features()
        
        for feature in self.features:
            values = self.value_list_for_features[feature]
            stdev = statistics.stdev(values)
            self.feature_stdevs[feature] = stdev
        
    def compute_feature_histograms(self):
        #print('computing feature histograms')
        if (not(bool(self.value_list_for_features))):
            self.set_value_list_for_features()
        
        for feature in self.features:
            values = self.value_list_for_features[feature]
            histogram = numpy.histogram(values,'auto', None, False, None, None)
            self.histograms_for_features[feature] = histogram

'''
if __name__ == '__main__':
    view_stats = ViewStats('statsTest','/home/vagrant/adapt/ad/test')
    view_stats.compute_feature_means()
    view_stats.compute_feature_stdevs()
    view_stats.compute_feature_histograms()
    view_stats.set_score_range()
    print(view_stats.get_stats_info())
'''

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
                try:
                    view.compute_anomaly_score()
                    ad_output_root = os.getcwd()
                    view_stats = ViewStats(view_type,ad_output_root)
                    view.attach_scores_to_db(view_stats)
                    view_stats.compute_feature_means()
                    view_stats.compute_feature_stdevs()
                    view_stats.compute_feature_histograms()
                    view_stats.set_score_range()
                    producer.send("ad-log", bytes(view_stats.get_stats_info()))
                    log.info(view_stats.get_stats_info(), encoding='utf-8')
                except:
                    producer.send("ad-log", bytes("error working with view {0} prevents statistics generation.".format(view_type), encoding='utf-8'))
                    log.exception("error working with view {0} prevents statistics generation.".format(view_type))
            i += 1
