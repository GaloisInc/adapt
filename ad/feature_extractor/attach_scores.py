#! /usr/bin/env python3

import csv
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


if __name__ == '__main__':
    in_file = sys.argv[1]
    with gremlin_query.Runner() as gremlin:
        with open(in_file, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                get_data(gremlin.fetch("g.V(" + str(row['id']) + ").property('anomalyScore'," + str(row['anomaly_score']) + ")"))
            print('Anomaly scores attached')
