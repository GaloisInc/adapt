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

        def drop_db(self):
                r = self.execute('g.V().drop().iterate()')
                count=self.execute('g.V().count()')[0]
                assert count is 0
