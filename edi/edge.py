import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.check as check
import edi.util.database as database

parser = argparse.ArgumentParser(description='Finding edges between anomalous processes')
parser.add_argument('--input',
                    '-i',
                    help='input score file',
                    required=True)
parser.add_argument('--output', '-o',
                    help='output file',
                    required=True)
parser.add_argument('--threshold','-t',
					help='filtering threshold (bits over entropy)',
					default=6.0,type=float)

if __name__ == '__main__':
    args = parser.parse_args()

    db = database.AdaptDatabase()

    pc = db.getQuery("MATCH (c:AdmSubject)-->(p:AdmSubject) RETURN  c.uuid as child, p.uuid as parent", endpoint="cypher")

    with open(args.input) as infile:
        scores = check.Scores(csv.reader(infile),True)

    scoreDict = {}
    sumScores = 0.0
    for x in scores.data:
        scoreDict[x[0]] = x[1]
        sumScores = sumScores + x[1]
    avgScore = sumScores / len(scores.data)
    print("Entropy: %f" % avgScore)
    with open(args.output,'w') as outfile:
        outfile.write("uuid,score,parent\n")
        for x in pc:
            if scoreDict[x['child']] > avgScore+args.threshold and scoreDict[x['parent']] > avgScore+args.threshold:
                outfile.write("%s,%f,%s\n" % (x['child'], scoreDict[x['child']],x['parent']))
