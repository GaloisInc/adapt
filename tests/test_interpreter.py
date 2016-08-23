#! /usr/bin/python3

"""
An interpreter for executing tests from the associated json file.

** Depends upon 'gremlin_query' and finding it in the 'tools' folder.

"""

import argparse, json, os, sys, unittest
from time import sleep
# Add tools directory to path, for gremlin_query library.
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


class DynamicTestCase(unittest.TestCase):
    def __init__(self, *args, **kwargs):
        '''
        Shoehorn arguments in through keyword args
        so a unittest can be dynamically created from json.
        '''
        self.query = kwargs.pop("query")
        self.response = kwargs.pop("response")
        self.requester = kwargs.pop("requester_email")
        self.explanation = kwargs.pop("explanation")
        unittest.TestCase.__init__(self, *args, **kwargs)

    def test_json_condition(self):
        result = execute_graph_query(self.query)
        self.assertEqual(result, self.response,
                         "for test query:\n%s\n\nRequested by: %s\n Explanation: %s" % 
                         (self.query, self.requester, self.explanation))


def get_test_json(path):
    with open(path) as fd:
        return json.loads(fd.read())


def tests_for_module(module_key, 
                     json_path="./tests.json",
                     data_key="5d_youtube_ie_output-100.avro"):
    js = get_test_json(json_path)
    try:
        test_list = js[data_key][module_key]
    except KeyError:
        print("*** No tests defined for  { %s : %s }  in file: %s ***" % 
            (data_key, module_key, json_path))
        sys.exit(1)
    return test_list


def execute_graph_query(query):
    with gremlin_query.Runner() as g:
        return str(g.fetch(query))


def run_tests_for_module(module_key, 
                         json_path="./tests.json",
                         data_key="5d_youtube_ie_output-100.avro",
                         run_after_failure=False):
    suite = unittest.TestSuite()
    tests = tests_for_module(module_key, json_path, data_key)
    for t in tests:
        suite.addTest(DynamicTestCase("test_json_condition", **t))
    return unittest.TextTestRunner(failfast=not run_after_failure).run(suite)


def execute_module(module_key, data_path="~/adapt/example/5d_youtube_ie_output-100.avro"):
    top = os.path.expanduser('~/adapt')
    if module_key is "in":
        os.system('Trint -p ' + data_path)
        sleep(25)  # no way to know when Trint is finished?
# Trint -F signals the segmenter to start, but this is done again 
# explicitly by the segmenter test below, which causes a race condition.
#        os.system('Trint -F')
#        sleep(1)  # currently no way to know when other services are complete with their respective processing.
    elif module_key is "se":
        cmd = ('%s/segment/segmenter/adapt_segmenter.py'
               ' --broker http://localhost:8182/'
               ' --store-segment Yes --name byPID --spec %s/config/segmentByPID.json' % (top, top))
        # cmd = 'classifier/phase3/simple_segments_by_pid.py --drop'
        os.system(cmd)
    elif module_key is "ad":
        pass
    elif module_key is "ac":
        cmd = os.path.join(top, 'classifier/phase3/fg_classifier.py')
        os.system(cmd)
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


def get_args():
    parser = argparse.ArgumentParser(description='An Adapt module and test runner')
    parser.add_argument(
        "-m", "--module", type=str, default="dx",
        choices='pre in se ad ac dx'.split(),
        help="Tests will run through the specified module")
    parser.add_argument(
        "-t", "--tests", type=str, default=os.path.dirname(__file__) + "/tests.json",
        help="Path to a JSON file with test data of the expected structure.")
    parser.add_argument(
        "-q", "--query", type=str,
        help="A gremlin query which will execute AFTER a run of the post-tests"
        " for the chosen module.")
    parser.add_argument(
        "-d", "--data", type=str, 
        default=os.path.expanduser('~/adapt') + "/example/5d_youtube_ie_output-100.avro",
        help="Path to an Avro data file. This file name (not path) should match one"
        " of the keys listed as top-level keys in the associated JSON file.")
    parser.add_argument(
        "-a", "--all", dest="run_all", action="store_true",
        help="Set this flag to run all remaining tests after one test fails. By"
        " default, a failed test will cause this script to abort future tests"
        " (and remaining modules).")
    parser.set_defaults(run_all=False)
    return parser.parse_args()


def main(args):
    pipeline = module_sequence(args.module)
    data_key = args.data.split(os.sep)[-1]
    print("\n ==> Running tests from: %s"
          "\n ==> ...using data file: %s"
          "\n ==> ....through module: %s" %
        (args.tests, args.data, args.module))
    for module in pipeline:
        print("\n======================================================================\n"
              "Executing module: %s" % module)
        execute_module(module, args.data)
        print("======================================================================\n"
              "Running module tests: %s" % module)
        if run_tests_for_module(module, args.tests, data_key, args.run_all).shouldStop:
            sys.exit(1)
    if args.query is not None:
        print("Query: " + args.query)
        print("Result: " + execute_graph_query(args.query))


if __name__ == '__main__':
    main(get_args())
