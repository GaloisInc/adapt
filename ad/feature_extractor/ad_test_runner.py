#! /usr/bin/env python3

import json, sys, os, requests


class Runner:
	"""Query runner that queries the database exposed by the Adapt Scala program once it is done
	ingesting data."""

	def __init__(self):
		self.url = 'http://localhost:8080/'

	def fetch(self, query):
		"""get the array of nodes that is the result of a query"""
		return self.fetch_nodes(query, {})

	def fetch_data(self, query, bindings = {}):
		"""get the array of nodes that is the result of a query"""
		return self.fetch_nodes(query, bindings)

	def fetch_generic(self, query, bindings = {}):
		"""get the raw result of a custom query"""
		return self.make_request('query/generic', query, bindings).text

	def fetch_nodes(self, query, bindings = {}):
		"""get the array of nodes that is the result of a query"""
		return self.make_request('query/nodes', query, bindings).json

	def fetch_edges(self, query, bindings = {}):
		"""get the array of edges that is the result of a query"""
		req = self.make_request('query/edges', query, bindings).json

	def make_request(self, endpoint, query, bindings = {}):
		"""splice in to the query the given bindings, POST to the given endpoint, and assert the
		response code is a 200"""

		# Strings need to have quotes added around them, numbers are turned into strings,
		# lists are converted to `[]` comma delimited strings
		def printLit(v):
			if isinstance(v, str):
				return '"' + v + '"'
			elif isinstance(v, list):
				return '[' + ', '.join(map(printLit,v)) + ']'
			else:
				return str(v)


		# Add the bindings to the front of the query
		query = '; '.join([ str(k) + " = " + printLit(v) for k, v in bindings.items() ]) + "; " + query

		# Query the 'nodes' REST api, and fail unless you get a 200 response
		req = requests.post(self.url + endpoint, data={'query': query})
		if req.status_code != requests.codes.ok:
			print("Request to " + self.url + endpoint + " failed")
			raise Exception("Request to " + self.url + endpoint + " failed")

		return req

	def close(self): pass

	def __enter__(self):
		return self



def queries_for_view(view_name = "netflow_connect_process_exec", json_view_defn_file_path = "/Users/atheriault/adapt/ad/feature_extractor/view_specification.json"):
	with open(json_view_defn_file_path, "r") as fd:
		view_dict = json.loads(fd.read())
		this_view = view_dict[view_name]
		node_collection_q = this_view['instance_set']
		feature_dict = this_view['feature_set']
		queries = []
		for key, val in feature_dict.items():
			queries.append(node_collection_q + "; " + val)
		return queries



def run_queries(query_list):
	not_gremlin = Runner()
	results = []
	for q in query_list:
		print(q)
		try:
			results.append(json.loads(not_gremlin.fetch_generic(q, bindings=bindings))[0])
		except:
			print("Exception at query: " + q)
			return False
	return results



bindings = {
	'ETYPE':'eventType',
	'STYPE':'subjectType',
	'SIZE':'size',
	'STIME':'startedAtTime',
	'DPORT':'dstPort',
	'SPORT':'srcPort',
	'DADDRESS':'dstAddress',

	'PROCESS':0,
	'EVENT':4,

	'ACCEPT': 0,
	'CHECK_FILE_ATTRIBUTES':3,
	'CLOSE':5,
	'CONNECT':6,
	'EXECUTE':9,
	'UNLINK':12,
	'MODIFY_FILE_ATTRIBUTES':14,
	'OPEN':16,
	'READ':17,
	'RENAME':20,
	'WRITE':21,
	'EXIT':36,

	'F_A_E_I':'EDGE_FILE_AFFECTS_EVENT in',
	'F_A_E_O':'EDGE_FILE_AFFECTS_EVENT out',
	'E_A_F_I':'EDGE_EVENT_AFFECTS_FILE in',
	'E_A_F_O':'EDGE_EVENT_AFFECTS_FILE out',
	'N_A_E_I':'EDGE_NETFLOW_AFFECTS_EVENT in',
	'N_A_E_O':'EDGE_NETFLOW_AFFECTS_EVENT out',
	'E_A_N_I':'EDGE_EVENT_AFFECTS_NETFLOW in',
	'E_A_N_O':'EDGE_EVENT_AFFECTS_NETFLOW out',
	'E_G_B_S_I':'EDGE_EVENT_ISGENERATEDBY_SUBJECT in',
	'E_G_B_S_O':'EDGE_EVENT_ISGENERATEDBY_SUBJECT out'
}



if __name__ == '__main__':
	queries = queries_for_view()
	print(run_queries(queries))
