import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.context as context

parser = argparse.ArgumentParser(description='Join two contexts')
parser.add_argument('--left',
                    '-l',
                    help='left input context file',
					required = True)
parser.add_argument('--right', '-r',
                    help='right input context file',
					required = True)
parser.add_argument('--output', '-o',
                    help='output file',
					required = True)



def zeroVec(header):
	return {h:0 for h in header}

def combine(vec1,vec2):
	dict = {}
	for k in vec1.keys():
		dict['l_' + k] = vec1[k]
	for k in vec2.keys():
		dict['r_' + k] = vec2[k]
	return dict

def vec2str(header,dict):
	return ','.join([str(dict[k]) for k in header])


if __name__ == '__main__':
    args = parser.parse_args()
    leftfile = args.left
    outputfile = args.output
    rightfile = args.right

    with open(leftfile) as lfile:
        left = context.Context(csv.reader(lfile))

    lkeys = set(left.data.keys())
    print('Left file %s has %d keys' % (leftfile, len(lkeys)))

    with open(rightfile) as rfile:
        right = context.Context(csv.reader(rfile))

    rkeys = set(right.data.keys())
    print('Right file %s has %d keys' % (rightfile, len(rkeys)))


    keys = lkeys | rkeys
    print('%d keys appear in either one' % len(keys))

    output = {}
    for k in keys:
        if k in lkeys and not(k in rkeys):
            output[k] = combine(left.data[k],zeroVec(right.header))
        elif k in rkeys and not (k in lkeys):
            output[k] = combine(zeroVec(left.header),right.data[k])
        else:
            output[k] = combine(left.data[k], right.data[k])

    header = ['l_'+h for h in left.header] + ['r_' + h for h in right.header]
    with open(outputfile,'w') as outfile:
        print('UUID,%s' % ','.join(header),file=outfile)
        for k in output.keys():
            print('%s,%s' % (k,vec2str(header,output[k])),file=outfile)
