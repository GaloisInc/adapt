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
#
'''
Export Titan nodes to stdout / text file.
'''

import aiogremlin
import argparse
import asyncio
import json
import logging
import pprint

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.INFO)


@asyncio.coroutine
def stream(db_client, query):
    expected_status = set([
        200,   # ok
        204,   # no content
        206])  # partial content, client sent Range header
    resp = yield from db_client.submit(query)
    while True:
        result = yield from resp.stream.read()
        if result is None:
            break
        assert result.status_code in expected_status, result
        if result.status_code == 204:
            log.info('no content')
            return []
        else:
            return result.data


class Exporter():

    def __init__(self, url):
        self.loop = asyncio.get_event_loop()
        self.db_client = aiogremlin.GremlinClient(url=url, loop=self.loop)

    def __del__(self):
        self.loop.run_until_complete(self.db_client.close())


    def export(self):
        query = "g.V()"
        nodes = self.loop.run_until_complete(stream(self.db_client, query))
        for node in nodes:
            assert node['id'] >= 0, node
            print('')
            print(pprint.pformat(node).replace("'", '"'))
            # print(json.dumps(node, sort_keys=True))

    def count(self):
        query = "g.V().count()"
        nodes = self.loop.run_until_complete(stream(self.db_client, query))
        assert len(nodes) == 1, nodes
        return int(nodes[0])


def arg_parser():
    p = argparse.ArgumentParser(
        description='Exports Titan nodes to stdout / text file.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    e = Exporter(args.db_url)
    log.info('count: %d', e.count())
    e.export()
