import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.database as database

parser = argparse.ArgumentParser(description='script to extract times')
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
parser.add_argument('--database','-d',
					choices = ['neo4j','adapt'],
					help = 'neo4j or adapt',
					default = 'adapt')
parser.add_argument('--provider','-r',
					help = 'provider name',
					default = None)






if __name__ == '__main__':
	args = parser.parse_args()
	url = args.url
	port = args.port
	output_file = args.output
	if args.database == 'adapt':
		db = database.AdaptDatabase(url=url,port=port)
	elif args.database == 'neo4j':
		db = database.Neo4jDatabase(url=url,port=port)

	if args.provider == None:
		endTimeQuery = "MATCH (n:AdmSubject)<--(ex:AdmEvent) WHERE ex.eventType = 'EVENT_EXIT' RETURN distinct n.uuid as uuid, ex.latestTimestampNanos as endtime"
	else:
		endTimeQuery = "MATCH (n:AdmSubject)<--(ex:AdmEvent) WHERE ex.eventType = 'EVENT_EXIT' AND n.provider = '%s' RETURN distinct n.uuid as uuid, ex.latestTimestampNanos as endtime" % args.provider
	print(endTimeQuery)
	endtimes = db.getQuery(endTimeQuery,endpoint='cypher')
	endtimeDict = {}
	for et in endtimes:
		endtimeDict[et['uuid']] = et['endtime']
	print("end times done")

	if args.provider == None:
		startTimeQuery = "MATCH (n:AdmSubject)<--(ex:AdmEvent) RETURN distinct n.uuid as uuid, n.startTimestampNanos as starttime"
	else:
		startTimeQuery = "MATCH (n:AdmSubject) WHERE n.provider = '%s' RETURN distinct n.uuid as uuid, n.startTimestampNanos as starttime" % args.provider
	print(startTimeQuery)
	starttimes = db.getQuery(startTimeQuery,endpoint='cypher')
	starttimeDict = {}
	for st in starttimes:
		starttimeDict[st['uuid']] = st['starttime']
	print("start times done")

	with open(output_file,'w') as outfile:
		outfile.write("uuid,starttime,endtime\n")
		for x in starttimeDict.keys():
			if x in endtimeDict.keys():
				outfile.write("%s,%s,%s\n" % (x,starttimeDict[x],endtimeDict[x]))
			else:
				outfile.write("%s,%s,\n" % (x,starttimeDict[x]))
