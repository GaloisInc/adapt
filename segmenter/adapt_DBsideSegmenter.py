#! /usr/bin/env python3
'''
Naive implementation of a DB-side segmenter (only the segmentation by PID-like properties is supported for now)
Based on parts of TitanClient (by Adria Gascon)
TODO: code refactoring to eliminate duplication and preserve in-memory implementation alongside DB-side segmentation
JSON specification handling

'''
from aiogremlin import GremlinClient
from bareBonesTitanDB import BareBonesTitanClient as tclient
import argparse
import logging
import os
import pprint
import re
import sys
import asyncio
import kafka

property_segmentNodeName='segment:name'
property_segmentEdgeLabel='segment:includes'

def escape(s):
    """
    A simple-minded function to escape strings for use in Gremlin queries.
    Replaces quotes with their escaped versions.  Other escaping
    may be necessary to avoid problems.
    """
    return s.replace("\'", "\\\'").replace("\"", "\\\"")

def arg_parser():
	p = argparse.ArgumentParser(description='A simple DB-side segmenter')
	p.add_argument('--broker', '-b', help='The broker to the Titan DB',required=True)
	p.add_argument('--criterion', '-c', help='The segmentation criterion (e.g PID)',default='pid')
	p.add_argument('--radius', '-r', help='The segmentation radius', type=int, default=2)
	p.add_argument('--directionEdges', '-e',help='Direction of the edges to be traversed (incoming, outgoing or both). Possible values: in, out, both. Default value: both', choices=['in','out','both'],default='both')
	group = p.add_mutually_exclusive_group()
	group.add_argument('--drop-db', action='store_true',help='Drop DB and quit, no segmentation performed')
	group.add_argument('--store-segment', help='Possible values: Yes,No,OnlyNodes. If No, only prints the details of the segments without creating them in Titan DB. If Yes, also stores the segments (nodes and edges) in Titan DB. If OnlyNodes, only stores the segment nodes in Titan DB (does not create segment edges) and prints the segment details', choices=['Yes','No','OnlyNodes'],default='Yes')
	p.add_argument('--log-to-kafka', action='store_true',help='Send logging information to kafka server')
	p.add_argument('--kafka',help='location of the kafka server',default='localhost:9092')
	return p

class SimpleTitanGremlinSegmenter:
	def __init__(self,args):
		self.args = args
		self.drop_db = args.drop_db
		self.broker = args.broker
		self.titanclient=tclient(self.broker)
		self.criterion=args.criterion
		self.radius=args.radius
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
		-gets all the nodes n in the graph that have a property (segmentation criterion) P (with value v_n)
		-for each such vertex, creates a segment vertex s_n and gives it property P with value v_n
		'''
		print('Constructing query\n')
		query="for (i in g.V().has('"+self.criterion+"').id()) graph.addVertex(label,'segment','"+property_segmentNodeName+"','s'+i.toString(),'"+self.criterion+"',g.V(i).values('"+self.criterion+"').next())"
		print('Query sent for execution\n')
		return self.titanclient.execute(query)

	def getVerticesWithProperty(self):
		'''
		query to Titan that retrieves all the nodes that have a certain property (segmentation criterion)
		'''
		query="g.V().has('"+self.criterion+"')"
		return self.titanclient.execute(query)

	def getNumberVerticesWithProperty(self):
                '''
                query to Titan that retrieves the number of nodes that have a certain property (segmentation criterion)
                '''
                query="g.V().has('"+self.criterion+"').count()"
                return self.titanclient.execute(query)

	def getVerticesWithPropertyIds(self):
		'''
		query to Titan that retrieves the ids of all the nodes that have a certain property (segmentation criterion)
		'''
		query="g.V().has('"+self.criterion+"').id().fold().next()"
		return self.titanclient.execute(query)

	def getSegmentNodeFromVertexId(self,vertexId):
		'''
		query Titan to get the segment node corresponding to a vertex identified by its id (assumes the name of the segment (i.e 'segment:name') is of the format 's'+id e.g s1)
		'''
		query="g.V().has('"+property_segmentNodeName+"','s'+g.V("+str(vertexId)+").id().next().toString())"
		return self.titanclient.execute(query)

	def getSubgraphFromVertexId(self,vertexId):
		'''
		query Titan to retrieve the ids of nodes within a set radius of the node with id vertexId
		'''
		subgraph_query="subGraph=g.V("+str(vertexId)+").repeat(__."+self.directionEdges+"E().subgraph('subGraph').bothV()).times("+str(self.radius)+".cap('subGraph').next()"
		subgraph_idRetrieval_query="subGraphtr=subGraph.traversal();subGraphtr.V().id().fold().next()"
		return self.titanclient.execute(subgraph_query+";"+subgraph_idRetrieval_query)

	def addSegmentEdges(self,segmentNodeName,subgraph_ids,segmentEdgeLabel):
		'''
		query Titan to add edges between a segment node with name 'segmentNodeName' and nodes that are in the segment. Requires segment nodes to have been created
		'''
		query="for (node in "+subgraph_ids+") g.V().has('"+property_segmentNodeName+"','"+segmentNodeName+"').next().addEdge('"+segmentEdgeLabel+"',g.V(i).next())"
		return self.titanclient.execute(query)

	def getSegments(self):
		count=self.getNumberVerticesWithProperty()[0]
		if count>0:
			seedVertices="""g.V().has(\'"""+self.criterion+"""\').id().fold().next()"""
			subgraphQuery="""sub=g.V(i).repeat(__."""+self.directionEdges+"""E().subgraph('sub').bothV()).times("""+str(self.radius)+""").cap('sub').next()"""
			segmentInfo="""'segment s'+g.V(i).id().next().toString()+ \' """+self.criterion+""" value \' +g.V(i).values(\'"""+self.criterion+"""\').next().toString()"""
			query="""result=[];for (i in """+seedVertices+""") {"""+subgraphQuery+""";subtr=sub.traversal();result.add(["""+segmentInfo+""",subtr.V().valueMap(true).fold().next()])};return result"""
			res=self.titanclient.execute(query)
		else:
			res=0
		return res

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
		query="""mgmt=graph.openManagement();if (mgmt.getVertexLabel(\'"""+vertexLabel+"""\')==null) {test=mgmt.makeVertexLabel(\'"""+vertexLabel+"""\').make();mgmt.commit();mgmt.close()}"""
		self.titanclient.execute(query)

	def createSchemaVertexProperty(self,vertexProperty,vertexType,cardinality):
                query="""mgmt=graph.openManagement();if (mgmt.getPropertyKey(\'"""+vertexProperty+"""\')==null) {test=mgmt.makePropertyKey(\'"""+vertexProperty+"""\').dataType("""+vertexType+""".class).cardinality(Cardinality."""+cardinality+""").make();mgmt.commit();mgmt.close()}"""
                self.titanclient.execute(query)

	def createSchemaEdgeLabel(self,edgeLabel):
                query="""mgmt=graph.openManagement();if (mgmt.getEdgeLabel(\'"""+edgeLabel+"""\')==null) {test=mgmt.makeEdgeLabel(\'"""+edgeLabel+"""\').make();mgmt.commit();mgmt.close()}"""
                self.titanclient.execute(query)


	def storeSegments(self):
		'''
		creates segments in the database (only segment nodes when '--store-segment' is equal to 'OnlyNodes' and full segments when it is equal to 'Yes')
		'''
		count=self.getNumberVerticesWithProperty()[0]
		if count>0:
			if isinstance(self.criterion,str):
				type_criterion='String'
			elif isinstance(self.criterion,int):
				type_criterion='Integer'
			elif isinstance(self.criterion,float):
				type_criterion='Float'
			elif isinstance(self.criterion,datetime.datetime):
				type_criterion='Date'
			else:
		        	type_criterion='Not supported for now'
		
			if 'Not supported' in type_criterion:
				print('The segments cannot be created or stored. The segment criterion type is not defined.')
				return "Undefined criterion type"
			else:
				self.createSchemaVertexLabel('Segment')
				self.createSchemaVertexProperty(property_segmentNodeName,'String','SINGLE')
				self.createSchemaVertexProperty('parentVertexId','Integer','SINGLE')
				self.createSchemaVertexProperty(self.criterion,type_criterion,'SINGLE')
				createVertices_query="idWithProp=g.V().has('"+self.criterion+"').has(label,neq('Segment')).id().fold().next(); existingSegNodes_parentIds=g.V().hasLabel('Segment').values('parentVertexId').fold().next();idsToStore=idWithProp-existingSegNodes_parentIds; if (idsToStore!=[]){for (i in idsToStore) {graph.addVertex(label,'Segment','parentVertexId',i,'"+property_segmentNodeName+"','s'+i.toString(),'"+self.criterion+"',g.V(i).values('"+self.criterion+"').next())}}"
				segmentNodes_created="g.V().hasLabel('Segment').valueMap(true)"
				addEdges_query="""idWithProp=g.V().has(\'"""+self.criterion+"""\').has(label,neq('Segment')).id().fold().next(); existingSegNodes_parentIds=g.V().hasLabel('Segment').values('parentVertexId').fold().next(); ;idsToStore=idWithProp-existingSegNodes_parentIds; for (i in idWithProp) {sub=g.V(i).repeat(__."""+self.directionEdges+"""E().subgraph('sub').bothV().has(label,neq('Segment'))).times("""+str(self.radius)+""").cap('sub').next();subtr=sub.traversal(); if (i in idsToStore) {s=graph.addVertex(label,'Segment',\'"""+property_segmentNodeName+"""\','s'+i.toString(),\'"""+self.criterion+"""\',g.V(i).values(\'"""+self.criterion+"""\').next(),'parentVertexId',i)} else {s=g.V().hasLabel('Segment').has('parentVertexId',i).next()};for (node in subtr.V().id().fold().next()) {if (g.V().hasLabel('Segment').has('parentVertexId',i).outE('prov-tc:partOfSegment').as('e').inV().hasId(node).select('e')==[]) {s.addEdge(\'"""+property_segmentEdgeLabel+"""\',g.V(node).next())}}}"""
				if self.store_segment=='OnlyNodes':
					createSegmentNodes=self.titanclient.execute(createVertices_query)
					sys.stdout.write('Segment nodes created')
					return "Nodes created"
				elif self.store_segment=='Yes':
					self.createSchemaEdgeLabel(property_segmentEdgeLabel)
					createFullSegments=self.titanclient.execute(addEdges_query)
					sys.stdout.write('Segments (including edges) created\n')
					return "Segments created"
				else:
					sys.stdout.write('No segment to store.\n')
					return "No segment"
		else:
			sys.stdout.write("No node with property: "+self.criterion+". Nothing to store.\n" )
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
			sys.stdout.write('\nSegmentation finished\n')
			sys.exit()
			
		
		

		










if __name__ == '__main__':
	args = arg_parser().parse_args()
	segrun=SimpleTitanGremlinSegmenter(args)
	print('Segmenter ready to run\n')
	segrun.run()
	





