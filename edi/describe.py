import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.database as db
import edi.util.groundtruth as groundtruth
import edi.util.describe as describe
import edi.util.check as check

parser = argparse.ArgumentParser(description='Describe top-k or ground truth processes')
parser.add_argument('--port','-p',
					help = 'Query port. Default: 8080',
					type = int,
					default = 8080)
parser.add_argument('--url','-u',
					help = 'Query url. Default: http://localhost',
					type = str,
					default = 'http://localhost')
parser.add_argument('--database','-d',
					choices = ['neo4j','adapt','json'],
					help = 'neo4j or adapt or json',
					default = 'adapt')

parser.add_argument('--json_path',
                    '-j',
                    help='path to JSON context files',
					default='contexts/')
parser.add_argument('--input',
                    '-i',
                    help='input score file')
parser.add_argument('--output', '-o',
                    help='output description file')
parser.add_argument('--groundtruth', '-g',
                    help='ground truth file')
parser.add_argument('--topk', '-k',
					help='Number k of top scoring processes to describe',
					default=10,
					type=int)
parser.add_argument('--ty', '-t',
                    help='type of ground truth to use',
                    default=None)
parser.add_argument('--reverse','-r',
					help='sort anomaly scores in decreasing order',
					action='store_true')
parser.add_argument('--verbose', '-v',
					help="print detailed messages",
					action='store_true')

if __name__ == '__main__':
	args = parser.parse_args()
	if args.database == 'adapt':
		database = db.AdaptDatabase(url=args.url,port=args.port)
		getSummary = lambda x: describe.getSummary(database,x)
	elif args.database == 'neo4j':
		database = db.Neo4jDatabase(url=args.url,port=args.port)
		getSummary = lambda x: describe.getSummary(database,x)
	elif args.database == 'json':
		# todo: specify these externally
		summaries = describe.loadSummaries('%s/ProcessEvent.json' % args.json_path,
										   '%s/ProcessExec.json' % args.json_path,
										   '%s/ProcessParent.json' % args.json_path,
										   '%s/ProcessChild.json' % args.json_path,
										   '%s/ProcessNetflow.json' % args.json_path)
		getSummary = lambda x: describe.getSummaryFromFiles(summaries,x)

	if args.topk != None and args.input != None:
		scores = check.getScores(args.input,reverse=args.reverse)
		with open(args.output, 'w') as outfile:
			for i in range(args.topk):
				print(scores.data[i][0])
				outfile.write('======================================================================\n')
				outfile.write('uuid: %s, score: %f\n' % (scores.data[i][0],scores.data[i][1]))
				outfile.write('======================================================================\n')

				describe.writeSummary(outfile,getSummary(scores.data[i][0]))

	elif args.groundtruth != None:
		gt = groundtruth.getGroundTruth(args.groundtruth,args.ty)
		with open(args.output, 'w') as outfile:
			for x in gt.data:
				print(x)
				outfile.write('======================================================================\n')
				outfile.write('uuid: %s\n' % (scores.data[i][0],scores.data[i][1]))
				outfile.write('======================================================================\n')
				describe.writeSummary(outfile,getSummary(x))
