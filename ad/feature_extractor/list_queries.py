#! /usr/bin/env python3

import sys, os, csv, json, math, logging
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


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
class ListQueries:

    def __init__(self, vt, nq, fq):
        self.view_type = vt
        self.node_ids_query = nq
        self.features_queries = fq
        
    def list_view(self):
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
        queries = QUERY.split(';')
        print("VIEW " + self.view_type + "\n")
        for q in queries:
            print("{0}".format(q))
        
        return True

  

if __name__ == '__main__':
    in_json = sys.argv[1]
    with open(in_json) as f:
        views = json.loads(f.read())
    for view_type in sorted(views.keys()):
        view_data = views[view_type]
        view = ListQueries(view_type, view_data['instance_set'], view_data['feature_set'])
        view.list_view()
        