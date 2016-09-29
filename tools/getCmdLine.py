#! /usr/bin/env python3

import os
import sys
import pprint
import asyncio
from aiogremlin import GremlinClient
import graphviz

sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


if __name__ == '__main__':

    with gremlin_query.Runner() as gremlin:
        QUERY = "g.V().has('subjectType', 0).dedup().by('commandLine').valueMap(true, 'commandLine')"

        vertices = gremlin.fetch_data(QUERY)

        for v in vertices:
            iden = v['id']
            cmdLine = v['commandLine']
            print('id:{}, cmdLine:{}'.format(iden, cmdLine))
