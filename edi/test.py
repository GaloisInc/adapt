
import argparse
import timeit
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.simple.ad as ad
import edi.util.check as check

parser = argparse.ArgumentParser(description='Run tests')
parser.add_argument('--dir',
                    '-d',
                    help='Specify a test directory')
parser.add_argument('--mode', '-m',
					help='batch or stream',
					default='batch',
					choices=['batch','stream'])


tests = [{'dir': 'test',
		   'contexts': [('test1','anomaly'),
						('test2','anomaly'),
						('test3','anomaly'),
						('test4','anomaly')],
		   'groundtruth': ['gt']}
		  ]

methods = [{'name':'avf','covariant':False},
		   {'name':'avc','covariant':True},
		   {'name':'rasp','covariant':True}]


def runTest(test,method,mode):
	dir = test['dir']
	for (ctxt,ty) in test['contexts']:
		if (mode == 'batch'):
			ad.batch(dir + '/' + ctxt + '.csv',
					dir + '/' + ctxt + '.' + method['name'] + '.batch.scored.csv',
					method['name'])
		elif(mode == 'stream'):
			ad.stream(dir + '/' + ctxt + '.csv' ,
					  dir + '/' + ctxt + '.' + method['name'] + '.stream.scored.csv',
					  method['name'])
		for gt in test['groundtruth']:
			check.main(dir + '/' + ctxt + '.' + method['name'] + '.' + mode + '.scored.csv',
				   dir + '/' + ctxt + '.' + method['name'] + '.' + gt + '.' + mode + '.ranked.csv',
				   dir + '/' + gt + '.csv',
				   ty,
				   method['covariant'])


def runTests(tests):
	args = parser.parse_args()

	if args.dir == None:
		for test in tests:
			for method in methods:
				runTest(test,method,args.mode)
	else:
		for test in tests:
			if test['dir'] == args.dir:
				for method in methods:
					runTest(test,method,args.mode)


if __name__ == '__main__':
	runTests(tests)
