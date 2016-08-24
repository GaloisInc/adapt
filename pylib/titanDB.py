#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Based on `TitanClient` by Adria Gascon, 2016
(removes all functionality linked to in-memory segmentation)
"""
from aiogremlin import GremlinClient
import argparse
import asyncio
import logging
import os
import pprint
import re
import sys
import aiohttp
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class TitanClient:
	def __init__(self, broker='ws://localhost:8182/'):
		self.broker = broker
		self.loop = asyncio.get_event_loop()
		self.gc = GremlinClient(url=broker, loop=self.loop)

	def close(self):
		self.loop.run_until_complete(self.gc.close())
		self.loop.close()

	def execute(self, gremlin_query_str, bindings={}):
		@asyncio.coroutine
		def stream(gc):
			result_data = []
			try:
				resp = yield from gc.submit(gremlin_query_str, bindings=bindings)
			except aiohttp.errors.ClientOSError as e:
				sys.stdout.write("Cannot connect to "+self.broker+"\n")
				sys.exit()
			else:
				while True:
					result = yield from resp.stream.read()
					if result is None:
						break
					assert result.status_code in [206, 200, 204], result.status_code
					if not result.status_code == 204:
						result_data += result.data
			return result_data
		result = self.loop.run_until_complete(stream(self.gc))
		return result

	def execute_many(self,queries, sem_num):
		'''
		Execute many queries simultaneously, with up to sem_num 
		concurrent requests.
		'''
		sem=asyncio.Semaphore(sem_num)
		@asyncio.coroutine
		def fetch(name,query): 
			with (yield from sem):
				print("Starting: %s" % name)
				t1 = time.time()
				result = yield from self.gc.execute(query)
				t2 = time.time()
				print("Finished: %s in %fs" % (name,t2-t1))
				return (name,query,result)
			
		jobs = [fetch(name,query) for (name,query) in queries]
		results = self.loop.run_until_complete(asyncio.gather(*jobs))
		return results   

	def execute_many_params(self,query,params, sem_num):
		'''
		Execute many variants of the same query simultaneously, 
		with up to sem_num concurrent requests, using the parameter bindings
		given in the list params.
		'''
		sem=asyncio.Semaphore(sem_num)
		@asyncio.coroutine
		def fetch(bindings): 
			with (yield from sem):
				print("Starting: %a" % (bindings))
				t1 = time.time()
				result = yield from self.gc.execute(query,bindings=bindings)
				t2 = time.time()
				print("Finished: %a in %fs" % (bindings,t2-t1))
				return (name,bindings,result)
			
		print("For query: %s" % (query))
		jobs = [fetch(bindings) for (bindings) in params]
		results = self.loop.run_until_complete(asyncio.gather(*jobs))
		return results   


	def drop_db(self):
		r = self.execute('g.V().drop().iterate()')
		count=self.execute('g.V().count()')[0]
		assert count is 0
