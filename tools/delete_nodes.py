#! /usr/bin/env python3
'''
Discards all nodes and edges from Titan,
to set up for another cycle of testing.
'''

import gremlin_query


def drop_all():
    with gremlin_query.Runner() as gremlin:
        for x in ['E', 'V']:
            q = 'g.%s().drop().iterate()' % x
            for msg in gremlin.fetch(q):
                print(x, msg)


if __name__ == '__main__':
    drop_all()
