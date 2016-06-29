import unittest
import os
from time import sleep
import asyncio
from aiogremlin import GremlinClient

class GremlinQandR:
    def __init__(self, query, response):
        self.q = query
        self.r = response

    def query(self):
        return self.q

    def response(self):
        return self.r

class PipelineSmokeTests(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        # Start from a clean state
        os.system('~/adapt/tools/stop_services.sh')
        os.system('rm -rf /opt/titan/db/*')
        sleep(20)
        os.system('~/adapt/start_daemons.sh')
        # Ingest small smoke test file
        self.loop = asyncio.get_event_loop()
        self.gremlin = GremlinClient(loop=self.loop)
        os.system('Trint -p ~/adapt/example/5d_youtube_ie_output-100.avro')
        sleep(10)
        os.system('Trint -f')

#    def test_pre_ingest(self):
#        queries = [ GremlinQandR("g.V().count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
#                    GremlinQandR("g.E().count()", "[Message(status_code=200, data=[0], message='', metadata={})]")]
#        for q in queries:
#            self.assertEqual(self.loop.run_until_complete(self.gremlin.execute(q.query())), q.response())

    def test_post_ingest(self):
        queries = [ GremlinQandR("g.V().has(label, 'Entity').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-File').count()", "[Message(status_code=200, data=[18], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Netflow').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Memory').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Resource').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Subject').count()", "[Message(status_code=200, data=[28], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Host').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Agent').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                  ]
        for q in queries:
            self.assertEqual(str(self.loop.run_until_complete(self.gremlin.execute(q.query()))), q.response(), msg=q.query())
        
    def test_segment(self):
        queries = [ GremlinQandR("g.V().has(label, 'Entity').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-File').count()", "[Message(status_code=200, data=[18], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Netflow').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Memory').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Resource').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Subject').count()", "[Message(status_code=200, data=[28], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Host').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Agent').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Segment').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                  ]
        for q in queries:
            self.assertEqual(str(self.loop.run_until_complete(self.gremlin.execute(q.query()))), q.response(), msg=q.query())

    def test_ad(self):
        queries = [ GremlinQandR("g.V().has(label, 'Entity').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-File').count()", "[Message(status_code=200, data=[18], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Netflow').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Memory').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Resource').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Subject').count()", "[Message(status_code=200, data=[28], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Host').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Agent').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Segment').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyScore').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"), # fails
                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyType').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"), # specified as output in Language.md but not currently in schema
                  ]
        for q in queries:
            self.assertEqual(str(self.loop.run_until_complete(self.gremlin.execute(q.query()))), q.response(), msg=q.query())

    def test_classifier(self):
        queries = [ GremlinQandR("g.V().has(label, 'Entity').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-File').count()", "[Message(status_code=200, data=[18], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Netflow').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Memory').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Resource').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Subject').count()", "[Message(status_code=200, data=[28], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Host').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Agent').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Segment').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Activity').count()", "[Message(status_code=200, data=[1], message='', metadata={})]"), # fails
                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyScore').count()", "[Message(status_code=200, data=[3], message='', metadata={})]"), # fails
#                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyType').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"), # specified as output in Language.md but not currently in schema
                  ]
        for q in queries:
            self.assertEqual(str(self.loop.run_until_complete(self.gremlin.execute(q.query()))), q.response(), msg=q.query())

    def test_dx(self):
        queries = [ GremlinQandR("g.V().has(label, 'Entity').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-File').count()", "[Message(status_code=200, data=[18], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Netflow').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Entity-Memory').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Resource').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Subject').count()", "[Message(status_code=200, data=[28], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Host').count()", "[Message(status_code=200, data=[0], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Agent').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Segment').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Phase').count()", "[Message(status_code=200, data=[1], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'APT').count()", "[Message(status_code=200, data=[1], message='', metadata={})]"),
                    GremlinQandR("g.V().has(label, 'Activity').count()", "[Message(status_code=200, data=[1], message='', metadata={})]"), # fails
                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyScore').count()", "[Message(status_code=200, data=[3], message='', metadata={})]"), # fails
#                    GremlinQandR("g.V().has(label, 'Segment').has('anomalyType').count()", "[Message(status_code=200, data=[2], message='', metadata={})]"), # specified as output in Language.md but not currently in schema
                  ]
        for q in queries:
            self.assertEqual(str(self.loop.run_until_complete(self.gremlin.execute(q.query()))), q.response(), msg=q.query())
        

    @classmethod
    def tearDownClass(self):
        self.loop.run_until_complete(self.gremlin.close())


if __name__ == '__main__':
    unittest.main()
