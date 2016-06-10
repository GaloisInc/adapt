#! /usr/bin/env python3

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.


import asyncio
from aiogremlin import GremlinClient


class Runner:

    def __init__(self):
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(loop=self.loop)

    def fetch(self, query):
        return self.loop.run_until_complete(self.gc.execute(query))

    def close(self):
        self.loop.run_until_complete(self.gc.close())

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        self.close()
