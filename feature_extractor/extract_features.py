import asyncio
from aiogremlin import GremlinClient
from math import floor
import sys
import csv

in_file = sys.argv[1]

feature_header = [	'EVENT_READ', 'EVENT_WRITE', 'EVENT_EXECUTE', 'SUBJECT_PROCESS',
					'SUBJECT_THREAD', 'SUBJECT_EVENT', 'NUM_FILES', 'NUM_SUBJECTS']

loop = asyncio.get_event_loop()
gc = GremlinClient(loop=loop)

def run_query(q):
	execute = gc.execute(q)
	result = loop.run_until_complete(execute)
	return result[0].data[0]

with open(in_file, 'r') as csvfile:
	reader = csv.reader(csvfile, delimiter=',')
	for row in reader:
		print(row[0] + ',' + row[1] + ',' + row[2], end = '')
		if row[1] == 'segment_type':
			for feat in feature_header:
				print(',' + feat, end = '')
		if row[1] == 'VRangeType':
			range = row[2].split('-')
			st = range[0]
			end = range[1]
			st_end = str(st) + "," + str(end)
			queries = [	"g.V().range(" + st_end + ").has('eventType',  16).count()", # EVENT_READ
						"g.V().range(" + st_end + ").has('eventType',  17).count()", # EVENT_WRITE
						"g.V().range(" + st_end + ").has('eventType',   8).count()", # EVENT_EXECUTE
						"g.V().range(" + st_end + ").has('subjectType', 0).count()", # SUBJECT_PROCESS
						"g.V().range(" + st_end + ").has('subjectType', 1).count()", # SUBJECT_THREAD
						"g.V().range(" + st_end + ").has('subjectType', 4).count()", # SUBJECT_EVENT
						"g.V().range(" + st_end + ").has('type',   'file').count()", # NUM_FILES
						"g.V().range(" + st_end + ").has('type', 'subject').count()"]# NUM_SUBJECTS
			for q in queries:
				res = run_query(q)
				print("," + str(res), end='')
		print("")

loop.run_until_complete(gc.close())
loop.close()

