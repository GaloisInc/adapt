import argparse
import collections
import json
import os
import sys


# This module really does several things:
# 1. Load in a json context specification file and parse it
# 2. Run the query and map the results into a dictionary representing the context
# 3. Emit the dictionary as a CSV file.
# The first part is an example of a common pattern when using json
# for configuration.  Is there a standard library for parsing json templates?
# The first two parts should probably be packaged together.  We might
# want to define other ways of extracting contexts, that don't fit the
# current model.
# The third part is generic and probably belongs in a separate module for
# reading and writing contexts represented in different ways.

def escape(attr):
	if ',' in attr or '"' in attr:
		return ('"' + attr.replace('"','""') + '"')
	else:
		return attr

def convertDict2CSVFile(dictionary, filename):
	attributes = sorted({escape(e) for val in dictionary.values() for e in val})
	with open(filename,'w') as f:
		f.write(','.join(['Object_ID']+attributes)+'\n')
		for obj,atts in dictionary.items():
			f.write(obj + ','
					+ ','.join(['1' if att in atts else '0'
							    for att in attributes]) + '\n')


def loadSpec(specfile):
	key = 'context specification'
	with open(specfile,'r') as f:
		spec = json.load(f)[key]
	return spec

def parseQuerySpec(spec):
	query = spec['query']
	obj_name = spec['objects']
	att_names = [att.strip() for att in spec['attributes']]
	endpoint = 'json'
	if 'endpoint' in spec.keys():
		endpoint = spec['endpoint']
	timeout = 300
	if 'timeout' in spec.keys():
		timeout = int(spec['timeout'])
	return (query,obj_name,att_names,endpoint,timeout)

def convertQueryRes2Dict(json,obj_name,att_names):
	dict = collections.defaultdict(list)
	if type(att_names)==list:
		for e in json:
			dict[e[obj_name]].extend([str(e[a]) for a in att_names])
	else:
		sys.exit('Expected list of attribute names in specification')
	return dict

def convert2InputCSV(spec_file,output_file,db):
	spec = loadSpec(spec_file)
	# extract the query from specification file and get the results
	(query,obj_name,att_names,endpoint,timeout) = parseQuerySpec(spec)
	# run the query
	query_res = db.getQuery(query,endpoint=endpoint,timeout=timeout)
	# convert query to a dictionary grouping by object ids
	dict = convertQueryRes2Dict(query_res,obj_name,att_names)
	# write out the dictionary to a context csv file
	convertDict2CSVFile(dict,output_file)
	print('Extraction successful. File '+output_file+' created.')

