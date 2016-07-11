#!/usr/bin/python3

""" 
An interpreter for executing tests from the associated json file. 

** Depends upon 'gremlin_query' and finding it by a relative path to the 'tools' directory!

"""

import sys, os, json, unittest, argparse
from time import sleep
sys.path.insert(0, '../tools')  # add tools directory to path for gremlin_query library
import gremlin_query


class DynamicTestCase(unittest.TestCase):

	def __init__(self, *args, **kwargs):
		"""
		Shoehorn arguments in through keyword args so a unittest can be dynamically created from json.
		"""
		self.query = kwargs.pop('query')
		self.response = kwargs.pop('response')
		unittest.TestCase.__init__(self, *args, **kwargs)


	def test_json_condition(self):
		result = execute_graph_query(self.query)
		self.assertEqual(result, self.response, "for test query:\n%s" % self.query)


def get_test_json(path):
	with open(path) as fd:
		return json.loads(fd.read())


def tests_for_module(module_key, data_key):
	js = get_test_json(test_json_file_path)
	test_list = js[data_key][module_key]
	return test_list


def execute_graph_query(query):
	with gremlin_query.Runner() as g:
		return str(g.fetch(query))


def run_tests_for_module(module_key, data_key="5d_youtube_ie_output-100.avro"):
	suite = unittest.TestSuite()
	tests = tests_for_module(module_key, data_key)
	for t in tests:
		suite.addTest(DynamicTestCase("test_json_condition", **t))
	return unittest.TextTestRunner(failfast=True).run(suite)


def execute_module(module_key, data_key="5d_youtube_ie_output-100.avro"):
	if module_key is "in":
		os.system('Trint -p ~/adapt/example/' + data_key)
		sleep(5)
		os.system('Trint -f')
	elif module_key is "se":
		pass
	elif module_key is "ad":
		pass
	elif module_key is "ac":
		pass
	elif module_key is "dx":
		pass
	else:
		pass


def module_sequence(until="dx"):
	module_seq = ["pre", "in", "se", "ad", "ac", "dx"]
	try:
		idx = module_seq.index(until)
		return module_seq[:idx+1]
	except ValueError:
		return []


if __name__ == '__main__':
	parser = argparse.ArgumentParser(description='A module and test runner')
	parser.add_argument("--module", "-m", type=str, help="A module key from the set: pre, in, se, ad, ac, dx", default="dx")
	parser.add_argument("--tests", "-t", type=str, help="Path to a JSON file with test data of the expected structure.", default="./tests.json")
	args = parser.parse_args()
	test_json_file_path = args.tests
	
	pipeline = module_sequence(args.module)
	for module in pipeline:
		print("Executing module: %s" % module)
		execute_module(module)
		print("Running module tests: %s" % module)
		if run_tests_for_module(module).shouldStop:
			sys.exit(1)

