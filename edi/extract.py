import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.extract.extract as ext
import edi.util.database as database

parser = argparse.ArgumentParser(description='script to extract contexts')
parser.add_argument('--port','-p',
					help = 'Query port. Default: 8080',
					type = int,
					default = 8080)
parser.add_argument('--url','-u',
					help = 'Query url. Default: http://localhost',
					type = str,
					default = 'http://localhost')
parser.add_argument('--input','-i',
					help = 'Input context specification file.',
					type = str,
					required = True)
parser.add_argument('--output','-o',
					help = 'Output context filename. ',
					type = str,
					required = True)
parser.add_argument('--format', '-f',
					choices = ['csv','json'],
					help = 'output format CSV or JSON',
					default = 'csv')
parser.add_argument('--json', '-j',
					help = 'JSON input filename')
parser.add_argument('--database','-d',
					choices = ['neo4j','adapt', 'json'],
					help = 'neo4j or adapt or json',
					default = 'adapt')
parser.add_argument('--provider','-r',
					help = 'provider name',
					default = None)
parser.add_argument('--verbose','-v',
					help = 'Print verbose output',
					action = 'store_true')


if __name__ == '__main__':
	args = parser.parse_args()
	url = args.url
	port = args.port
	spec_file = args.input
	provider = args.provider
	output_file = args.output
	json_file = args.json
	if args.verbose:
		print('URL: %s' % url)
		print('Port: %d' % port)
		print('Specification: %s' % spec_file)
		print('Provider: ' + str(provider))
		print('Context output: %s' % output_file)
		print('JSON input: %s' % json_file)
	if not(os.path.exists(spec_file)):
		sys.exit('Context specification file not found: %s' % spec_file)
	if args.database == 'json':
		if args.json == None:
			sys.exit('If using JSON cached result, must specify JSON file')
		if args.format != 'csv':
			sys.exit('If using JSON cached result, output must be CSV')

	def extractFromDB(format,db):
		if format == 'csv':
			ext.convert2CSV(spec_file,output_file,db,provider)
		elif format == 'json':
			ext.convert2JSON(spec_file,output_file,db,provider)

	if args.database == 'json':
		ext.convertJSON2CSV(spec_file,output_file,json_file)
	elif args.database == 'adapt':
		db = database.AdaptDatabase(url=url,port=port)
		extractFromDB(args.format,db)
	elif args.database == 'neo4j':
		db = database.Neo4jDatabase(url=url,port=port)
		extractFromDB(args.format,db)
