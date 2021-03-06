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
	def emit(att,atts):
		for (a,n) in atts:
			if a == att:
				return n
		return 0
	attributes = sorted({e[0] for val in dictionary.values() for e in val})
	escaped_attributes = [escape(att) for att in attributes]
	with open(filename,'w') as f:
		f.write(','.join(['Object_ID']+escaped_attributes)+'\n')
		for obj,atts in dictionary.items():
			f.write(obj + ','
					+ ','.join([str(emit(att,atts))
							    for att in attributes]) + '\n')


def loadSpec(specfile):
	key = 'context specification'
	with open(specfile,'r') as f:
		spec = json.load(f)[key]
	return spec

def parseQuerySpec(spec):
	query = spec['query']
	obj_name = spec['objects']
	count_name = spec['counts'] if 'counts' in spec.keys() else None
	att_names = [att.strip() for att in spec['attributes']]
	endpoint = 'json'
	if 'endpoint' in spec.keys():
		endpoint = spec['endpoint']
	timeout = 600
	if 'timeout' in spec.keys():
		timeout = int(spec['timeout'])
	return {'query':query,
			'obj_name':obj_name,
			'att_names':att_names,
			'count_name':count_name,
			'endpoint':endpoint,
			'timeout':timeout}

def convertQueryRes2Dict(json,obj_name,att_names,count_name):
	dct = collections.defaultdict(list)
	if type(att_names)==list:
		for e in json:
			dct[e[obj_name]].extend([(escape(str(e[a])),
									  e[count_name] if count_name != None else 1)
									  for a in att_names])
	else:
		sys.exit('Expected list of attribute names in specification')
	return dct

def extractQueryRes(spec,db,provider):
	# plug in the hole with provider name if available
	if provider != None:
		query = spec['query'] % ('n.provider = "' + provider + '"')
	else:
		query = spec['query'] % ('TRUE')
	# run the query
	return db.getQuery(query,endpoint=spec['endpoint'],timeout=spec['timeout'])
	# convert query to a dictionary grouping by object ids

def convert2CSV(spec_file,output_file,db,provider):
	spec = loadSpec(spec_file)
	# extract the query from specification file and get the results
	spec = parseQuerySpec(spec)
	query_res = extractQueryRes(spec,db,provider)
	d = convertQueryRes2Dict(query_res,
								spec['obj_name'],spec['att_names'],spec['count_name'])
	# write out the dictionary to a context csv file
	convertDict2CSVFile(d,output_file)
	print('Extraction successful. File '+output_file+' created.')

def convert2JSON(spec_file,output_file,db,provider):
	spec = loadSpec(spec_file)
	# extract the query from specification file and get the results
	spec = parseQuerySpec(spec)
	query_res = extractQueryRes(spec,db,provider)
	# write out the dictionary to a json file
	with open(output_file,'w') as f:
		json.dump(query_res,f)
	print('Extraction successful. File '+output_file+' created.')

def convertJSON2CSV(spec_file,output_file,json_file):
	spec = loadSpec(spec_file)
	# extract the query from specification file and get the results
	spec = parseQuerySpec(spec)
	with open(json_file,'r') as f:
		query_res = json.load(f)
	d = convertQueryRes2Dict(query_res,
								spec['obj_name'],spec['att_names'],spec['count_name'])
	# write out the dictionary to a context csv file
	convertDict2CSVFile(d,output_file)
	print('Extraction successful. File '+output_file+' created.')
