import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.database as database

parser = argparse.ArgumentParser(description='script to extract edge counts')
parser.add_argument('--port','-p',
					help = 'Query port. Default: 8080',
					type = int,
					default = 8080)
parser.add_argument('--url','-u',
					help = 'Query url. Default: http://localhost',
					type = str,
					default = 'http://localhost')
parser.add_argument('--output','-o',
					help = 'Output context filename. ',
					type = str,
					required = True)






if __name__ == '__main__':
	args = parser.parse_args()
	url = args.url
	port = args.port
	output_file = args.output
	db = database.AdaptDatabase(url=url,port=port)

	edgeCountQuery = "MATCH (n:AdmSubject)-[e]-() RETURN n.uuid as uuid, COUNT(e) as count"
	edgeCounts = db.getQuery(edgeCountQuery,endpoint='cypher',timeout=10000)
	edgeCountDict = {}
	for ec in edgeCounts:
		edgeCountDict[ec['uuid']] = ec['count']

	with open(output_file,'w') as outfile:
		outfile.write("uuid,edgeCount\n")
		for x in edgeCountDict.keys():
			outfile.write("%s,%s\n" % (x,edgeCountDict[x]))
