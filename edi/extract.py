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
parser.add_argument('--verbose','-v',
					help = 'Print verbose output',
					action = 'store_true')


if __name__ == '__main__':
	args = parser.parse_args()
	url = args.url
	port = args.port
	spec_file = args.input
	output_file = args.output
	if args.verbose:
		print('URL: %s' % url)
		print('Port: %d' % port)
		print('Specification: %s' % spec_file)
		print('Context output: %s' % output_file)
	if not(os.path.exists(spec_file)):
		sys.exit('Context specification file not found: %s' % spec_file)
	db = database.Database(url=url,port=port)
	ext.convert2InputCSV(spec_file,output_file,db)

