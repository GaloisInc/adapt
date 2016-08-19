# -*- coding: utf-8 -*-

"""Low-level Titan database client.
"""

from aiogremlin import GremlinClient
import asyncio
import aiohttp

class TitanDatabase(object):
    def __init__(self, broker = 'ws://localhost:8182/'):
        self.broker = broker
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(url = broker, loop = self.loop)

    def close(self):
        self.loop.run_until_complete(self.gc.close())
        self.loop.close()

    def execute(self, gremlin_query_str, bindings={}):
        @asyncio.coroutine
        def stream(gc):
            result_data = []
            response = yield from gc.submit(gremlin_query_str, bindings=bindings)
            while True:
                result = yield from response.stream.read()
                if result is None:
                    break
                assert(result.status_code in [206, 200, 204])
                if not result.status_code == 204:
                    result_data += result.data
            return result_data

        result = self.loop.run_until_complete(stream(self.gc))

        return result
