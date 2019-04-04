import argparse
import csv
import numpy

from . import groundtruth


class Scores:

    def __init__(self, reader, reverse=True):
        self.header = next(reader)[1:]
        #for row in reader:
        #    print(row[0] +","+ str(row[1]))
        self.data = [(row[0], float(row[1]))
                     for row in reader]
        self.data = sorted(self.data,
                    key=lambda x: x[1],
                    reverse=reverse)




def rankScores(scores,gt):
    ranks = []
    rank = 1
    for (uuid,score) in scores.data:
        if uuid in gt.data:
            ranks = ranks + [(uuid,score,rank)]
        rank = rank + 1
    return ranks

# Calculate discounted cumulative gain of a list of ranks
def discounted_cumulative_gain(ranks):
    dcg = 0.0
    for rank in ranks:
        dcg = dcg + 1.0/numpy.log2(rank+1)
    return dcg

# Calculate max possible DCG and ratio
def normalized_discounted_cumulative_gain(ranks,num_gt):
    dcg = discounted_cumulative_gain(ranks)
    maxdcg = 0.0
    for i in range(1,num_gt+1):
        maxdcg = maxdcg + 1.0/numpy.log2(i+1)
    return (dcg/maxdcg)

# Calculate area under ROC curve
def area_under_curve(ranks, num_gt, num_trans):
    area = 0.0
    increment = 1.0/(num_gt)
    for i in range(0,num_trans):
        for r in ranks:
            if r < i:
                area = area + increment
    return area / num_trans



def main(inputfile, outfile, ground_truth, gtType,reverse=True):
    with open(inputfile) as infile:
        scores = Scores(csv.reader(infile),reverse)

    print('Read scores file: %s' % inputfile)
    num_trans = len(scores.data)
    print('Number of transactions: %d' % num_trans)

    with open(ground_truth) as gtfile:
        gt = groundtruth.GroundTruth(csv.reader(gtfile), gtType)

    print('Read ground truth file: %s' % ground_truth)
    num_gt = len(gt.data)
    print('Number of %s elements: %d' % (gtType, num_gt))


    with open(outfile, 'w') as outfile:
        outfile.write("uuid,score,rank\n")
        uuidScoreRanks = rankScores(scores,gt)
        ranks = [rank for (uuid,score,rank) in uuidScoreRanks]
        ndcg = normalized_discounted_cumulative_gain(ranks,num_gt)
        print('NDCG: %f' % ndcg)
        auc = area_under_curve(ranks,num_gt,num_trans)
        print('AUC: %f' % auc)
        for (uuid, score,rank) in uuidScoreRanks:
            outfile.write("%s,%f,%d\n" % (uuid, score, rank))



