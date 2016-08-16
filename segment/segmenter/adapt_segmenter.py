#! /usr/bin/env python3
'''
Naive implementation of a DB-side segmenter (only the segmentation by PID-like properties is supported for now)
Based on parts of TitanClient (by Adria Gascon)
TODO: code refactoring to eliminate duplication and preserve in-memory implementation alongside DB-side segmentation
JSON specification handling

'''
from aiogremlin import GremlinClient
from titanDB import titanClient as tclient
import argparse
import logging
import os
import pprint
import re
import sys
import asyncio
import kafka
import time

property_segmentNodeName='segment:name'
property_segmentEdgeLabel='segment:includes'
property_seg2segEdgeLabel='segment:edge'


def arg_parser():
	p = argparse.ArgumentParser(description='A simple DB-side segmenter')
	p.add_argument('--broker', '-b', 
				   help='The broker to the Titan DB',
				   required=True)
	p.add_argument('--criterion', '-c', 
				   help='The segmentation criterion (e.g PID)',
				   default='pid')
	p.add_argument('--radius', '-r', 
				   help='The segmentation radius', 
				   type=int, default=2)
	p.add_argument('--directionEdges', '-e',
				   help='Direction of the edges to be traversed (incoming, outgoing or both). Possible values: in, out, both. Default value: both', 
				   choices=['in','out','both'],default='both')
	p.add_argument('--verbose','-v', 
				   action='store_true',help='Verbose mode')
	group = p.add_mutually_exclusive_group()
	group.add_argument('--drop-db', 
					   action='store_true',
					   help='Drop DB and quit, no segmentation performed')
	group.add_argument('--store-segment', 
					   help='Possible values: Yes,No,OnlyNodes. If No, only prints the details of the segments without creating them in Titan DB. If Yes, also stores the segments (nodes and edges) in Titan DB. If OnlyNodes, only stores the segment nodes in Titan DB (does not create segment edges) and prints the segment details', 
					   choices=['Yes','No','OnlyNodes'],
					   default='Yes')
	p.add_argument('--log-to-kafka', action='store_true',
				   help='Send logging information to kafka server')
	p.add_argument('--kafka',
				   help='location of the kafka server',
				   default='localhost:9092')
	p.add_argument('--spec',
				   help='A segment specification file in json format')
	return p

class SimpleTitanGremlinSegmenter:
	def __init__(self,args):
		self.args = args
		self.drop_db = args.drop_db
		self.broker = args.broker
		self.titanclient=tclient(self.broker)
		self.criterion=args.criterion
		self.segmentName='byPID' # TODO: parameterize
		self.type_criterion=None
		self.radius=args.radius
		self.verbose=args.verbose
		self.directionEdges=args.directionEdges
		self.store_segment = args.store_segment
		logging.basicConfig(level=logging.INFO)
		self.logger = logging.getLogger(__name__)
		self.logToKafka = args.log_to_kafka
		if self.logToKafka:
			self.producer = kafka.KafkaProducer(bootstrap_servers=[args.kafka])

	def createSegmentVertices(self):
		'''
		sends a query to Titan that:
		-  gets all the nodes n in the graph that have a 
		property (segmentation criterion) P (with value v_n)
		-  for each such vertex, creates a segment vertex s_n
		and gives it property P with value v_n
		'''
		query="""\
for (i in g.V().has('%(criterion)s').id()) {\
graph.addVertex(label,'segment',\
'%(segmentNodeName)','%(segmentName)s',\
'%(criterion)s+',g.V(i).values('%(criterion)s').next())\
}""" % {'criterion': self.criterion, 
		'segmentNodeName': property_segmentNodeName,
		'segmentName': self.segmentName}
		return self.titanclient.execute(query)

	def getVerticesWithProperty(self):
		'''
		query to Titan that retrieves all the nodes that have 
		a certain property (segmentation criterion)
		'''
		query="g.V().has('%(criterion)s')" % {'criterion' : self.criterion}
		return self.titanclient.execute(query)

	def getNumberVerticesWithProperty(self):
		'''
		query to Titan that retrieves the number of nodes that have a certain property (segmentation criterion)
		'''
		query="g.V().has('%(criterion)s').count()" % {'criterion' : self.criterion}
		return self.titanclient.execute(query)

	def getVerticesWithPropertyIds(self):
		'''
		query to Titan that retrieves the ids of all the nodes that have a certain property (segmentation criterion)
		'''
		query="g.V().has('%(criterion)s').id().fold().next()" % {'criterion' : self.criterion}
		return self.titanclient.execute(query)

	def getSubgraphFromVertexId(self,vertexId):
		'''
		query Titan to retrieve the ids of nodes within a set radius of the node with id vertexId
		'''
		subgraph_query = """\
subGraph=g.V(%(vertexId)d).repeat(__.%(directionEdges)sE()\
.subgraph('subGraph').bothV())\
.times(%(radius)d.cap('subGraph').next()\
""" % {'directionEdges' : self.directionEdges,
	   'vertexId' : vertexId,
	   'radius' : radius}
		subgraph_idRetrieval_query="subGraphtr=subGraph.traversal();subGraphtr.V().id().fold().next()"
		return self.titanclient.execute(subgraph_query+";"+subgraph_idRetrieval_query)

	def getSegments(self):
		count = self.getNumberVerticesWithProperty()[0]
		if count == 0:
			return 0

		seedVertices="""\
g.V().has(\'%(criterion)s\').id().fold().next()\
""" % {"criterion" : self.criterion}
		subgraphQuery="""\
sub=g.V(i).repeat(__.%(directionEdges)sE().subgraph('sub').bothV())\
.times(%(radius)d).cap('sub').next()\
""" % {'directionEdges' : self.directionEdges, 'radius': self.radius}
		segmentInfo="""\
'segment s'+g.V(i).id().next().toString()+ \' %(criterion)s value \' \
+g.V(i).values(\'%(criterion)s\').next().toString()\
""" % {"criterion" : self.criterion}
		query="""\
result=[];\
for (i in %(seedVertices)s) {%(subgraphQuery)s;\
subtr=sub.traversal();\
result.add([%(segmentInfo)s,subtr.V().valueMap(true).fold().next()])};\
return result\
""" % {"seedVertices" : seedVertices, 
	   "subgraphQuery" : subgraphQuery,
	   "segmentInfo" : segmentInfo}
		return self.titanclient.execute(query)

	def printSegments(self):
		'''
		prints the results of segmentation
		'''
		res=self.getSegments()
		sys.stdout.write('*'*30+'\n')
		sys.stdout.write('Summary of segmentation\n')
		sys.stdout.write('*'*30+'\n')
		sys.stdout.write('\n')
		if res==0:
			sys.stdout.write('No nodes with property: '+self.criterion+'. No segmentation performed.\n')
			return "segmentation criterion unknown"
		else:
			sys.stdout.write('Number of nodes with '+self.criterion+': '+str(len(res))+'\n')
			reg=re.compile("segment\s*s(?P<id>\d+)\s*"+self.criterion+"\s*value\s*(?P<criterion>\d+)")
			if len(res)>0:
				for n in res:
					sys.stdout.write('*'*30+'\n')
					sys.stdout.write(n[0]+'\n')
					r=reg.match(n[0])
					sys.stdout.write('Number of segment elements centered around node with id '+str(r.group('id'))+' and with '+str(self.criterion)+' '+str(r.group('criterion'))+': '+str(len(n[1]))+'\n')
					for subn in n[1]:
						print(subn)
						sys.stdout.write('\n')
						sys.stdout.write('*'*30+'\n')
		return "segmentation summary printed"

	def createSchemaVertexLabel(self,vertexLabel):
		query="""\
mgmt=graph.openManagement();\
if (mgmt.getVertexLabel(\'%(vertexLabel)s\')==null) {\
test=mgmt.makeVertexLabel(\'%(vertexLabel)s\').make();\
mgmt.commit();\
mgmt.close()\
}""" % {"vertexLabel": vertexLabel}
		self.titanclient.execute(query)

	def createSchemaVertexProperty(self,vertexProperty,vertexType,cardinality):
		query="""\
mgmt=graph.openManagement();\
if (mgmt.getPropertyKey(\'%(vertexProperty)s\')==null) {\
test=mgmt.makePropertyKey(\'%(vertexProperty)s\')\
.dataType(%(vertexType)s.class)\
.cardinality(Cardinality.%(cardinality)s).make();\
mgmt.commit();\
mgmt.close()\
}""" % {"vertexProperty":vertexProperty,
		"vertexType": vertexType,
		"cardinality": cardinality}
		self.titanclient.execute(query)

	def createSchemaEdgeLabel(self,edgeLabel):
		query="""\
mgmt=graph.openManagement();\
if (mgmt.getEdgeLabel(\'%(edgeLabel)s\')==null) {\
test=mgmt.makeEdgeLabel(\'%(edgeLabel)s\').make();\
mgmt.commit();\
mgmt.close()\
}""" % {"edgeLabel" : edgeLabel}
		self.titanclient.execute(query)

	def createSchemaElements(self):
		self.createSchemaVertexLabel('Segment')
		self.createSchemaVertexProperty(property_segmentNodeName,
						'String','SINGLE')
		self.createSchemaVertexProperty('parentVertexId',
						'Integer','SINGLE')
		self.createSchemaVertexProperty(self.criterion,
						self.type_criterion,'SINGLE')
		self.createSchemaEdgeLabel(property_segmentEdgeLabel)

	def checkCriterionType(self):
		if isinstance(self.criterion,str):
			self.type_criterion='String'
		elif isinstance(self.criterion,int):
			self.type_criterion='Integer'
		elif isinstance(self.criterion,float):
			self.type_criterion='Float'
		elif isinstance(self.criterion,datetime.datetime):
			self.type_criterion='Date'
		else:
			self.type_criterion=None
			return False
		return True


	def createVertices_query(self):
		createVertices_query="""\
idWithProp=g.V().has('%(criterion)s',gte(0)).has(label,neq('Segment')).id().fold().next(); \
existingSegNodes_parentIds=g.V().has('%(segmentNodeName)','%(segmentName)s').values('parentVertexId').fold().next();\
idsToStore=idWithProp-existingSegNodes_parentIds; \
if (idsToStore!=[]){\
for (i in idsToStore) {\
graph.addVertex(label,'Segment',\
'parentVertexId',i,\
'%(segmentNodeName)s','%(segmentName)s',\
'%(criterion)s',g.V(i).values('%(criterion)s').next())}\
}\
""" % {'criterion': self.criterion,
	   'segmentNodeName': property_segmentNodeName,
	   'segmentName': self.segmentName}
		return createVertices_query

	def addEdges_query(self):
		addEdges_query ="""\
idWithProp=g.V().has('%(criterion)s',gte(0)).has(label,neq('Segment')).id().fold().next(); \
existingSegNodes_parentIds=g.V().has('%(segmentNodeName)s','%(segmentName)s').values('parentVertexId').fold().next();\
idsToStore=idWithProp-existingSegNodes_parentIds; \
for (i in idWithProp) {sub=g.V(i).repeat(__.%(directionEdges)sE().subgraph('sub').bothV().has(label,neq('Segment'))).times(%(radius)d).cap('sub').next();\
subtr=sub.traversal(); \
if (i in idsToStore) {\
s=graph.addVertex(label,'Segment',\
'%(segmentNodeName)s','%(segmentName)s',\
'%(criterion)s',g.V(i).values('%(criterion)s').next(),\
'parentVertexId',i)\
} else {\
s = g.V().has('%(segmentNodeName)s','%(segmentName)s').has('parentVertexId',i).next()
}; \
idNonLinkedNodes=subtr.V().id().fold().next()-g.V().has('%(segmentNodeName)s','%(segmentName)s').has('parentVertexId',i).outE('%(segmentEdgeLabel)s').inV().id().fold().next();\
for (node in idNonLinkedNodes) {
s.addEdge('%(segmentEdgeLabel)s',g.V(node).next())
}
}""" % {'criterion': self.criterion,
		'segmentNodeName': property_segmentNodeName,
		'segmentEdgeLabel': property_segmentEdgeLabel,
		'directionEdges': self.directionEdges,
		'radius' : self.radius,
		'segmentName' : self.segmentName}
		return addEdges_query

	def addSeg2SegEdges_query(self): 
		addSeg2SegEdges_query="""\
for (snode in g.V().has('%(segmentNodeName)s','%(segmentName)s').id().fold().next()){\
linkedSeg=g.V(snode).as('a').out('%(segmentEdgeLabel)s').out().in('%(segmentEdgeLabel)s').dedup().where(neq('a')).id().fold().next()-\
g.V(snode).out('%(seg2segEdgeLabel)s').id().fold().next();\
for (s in linkedSeg){\
g.V(snode).next().addEdge('%(seg2segEdgeLabel)s',g.V(s).next())\
}\
}""" % {'segmentEdgeLabel':property_segmentEdgeLabel,
	'seg2segEdgeLabel':property_seg2segEdgeLabel}
	

		return addSeg2SegEdges_query

	def storeSegments(self):
		'''
		creates segments in the database (only segment nodes 
		when '--store-segment' is equal to 'OnlyNodes' and 
		full segments when it is equal to 'Yes')
		'''
		self.createSchemaElements()
		
		count=self.getNumberVerticesWithProperty()[0]
		if count>0:
			if (self.checkCriterionType() == False):
				print('The segments cannot be created or stored. The segment criterion type is not defined.')
				return "Undefined criterion type"
			else:
				if self.store_segment=='OnlyNodes':
					t1 = time.time()
					createSegmentNodes=self.titanclient.execute(self.createVertices_query())
					t2 = time.time()
					sys.stdout.write('Segment nodes created in %fs' % (t2 - t1))
					return "Nodes created"
				elif self.store_segment=='Yes':
					
					t1 = time.time()
					createFullSegments=self.titanclient.execute(self.addEdges_query())
					t2 = time.time()
					sys.stdout.write('Segments created in %fs\n' % (t2-t1))
					addSeg2SegEdges=self.titanclient.execute(self.addSeg2SegEdges_query())
					t3 = time.time()
					sys.stdout.write('Segment edges created in %fs\n' % (t3-t2))
					sys.stdout.write('Total segmentation time %fs\n' % (t3-t1))
					return "Segments created"
				else:
					sys.stdout.write('No segment to store.\n')
					return "No segment"
		else: # count == 0
			sys.stdout.write("No node with property: %s. Nothing to store.\n" % self.criterion)
			return "Unknown segmentation criterion"
		
	def log(self,type_log,text):
		if type_log=='info':
			self.logger.info(text)
		if type_log=='error':
			self.logger.error(text)
		if self.logToKafka:
			self.producer.send("se-log", str.encode(text))

	def run(self):
		sys.stdout.write('*' * 30 + '\n')
		sys.stdout.write('Running DB side segmenter\n')
		sys.stdout.write('*' * 30 + '\n')
		if self.drop_db:
			tc=self.titanclient
			tc.drop_db()
			self.log('info','Database dropped\n')
			tc.close()
			sys.exit()
		if self.store_segment=='No':
			printres=self.printSegments()
			self.titanclient.close()
			if "summary printed" in printres:
				self.log('info','Segmentation done\n')
			else:
				self.log('error','Unknown segmentation criterion\n')
			sys.exit()
		else:
			if self.verbose:
				self.printSegments()
			storageres=self.storeSegments()
			if ("Unknown" in storageres) or ("Undefined" in storageres):
				self.log('error',storageres+"\n")
			else:
				if self.store_segment=='Yes':
					self.log('info','Full segments (nodes and edges) stored in Titan DB\n')
				elif self.store_segment=='OnlyNodes':
					self.log('info','Segment nodes stored in Titan DB\n')
			self.titanclient.close()
			self.log('info','\nSegmentation finished\n')
			sys.exit()
			
		
		

		










if __name__ == '__main__':
	args = arg_parser().parse_args()
	segrun=SimpleTitanGremlinSegmenter(args)
	print('Segmenter ready to run\n')
	segrun.run()
	





