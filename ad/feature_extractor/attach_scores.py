#! /usr/bin/env python3

import csv
import sys
import os
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

if __name__ == '__main__':
    in_file = sys.argv[1]
    with gremlin_query.Runner() as gremlin:
        with open(in_file, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                gremlin.fetch_data("g.V({id}).property('anomalyScore',{score})".format(id=row['id'], score=row['anomaly_score']))
            print('Anomaly scores attached')
