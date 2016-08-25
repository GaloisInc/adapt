#! /usr/bin/env python3
'''
Discards all nodes and edges from Titan,
to set up for another cycle of testing.
'''

import gremlin_query


def drop_all():
    # Even for very small transactions,
    # like "g.V().order().limit(20).drop().iterate()",
    # this sometimes reports GremlinServerError: Code [597]: SCRIPT_EVALUATION.
    #   The vertex or type has been removed [v[1831104]]
    # in which case verify no background tasks are inserting,
    # and if necessary then: stop, /opt/titan/bin/titan.sh clean, start.
    with gremlin_query.Runner() as gremlin:
        for x in ['V().has(\'segment:name\',within(\'byPID\',\'byTime\')).has(label,\'Segment\')']:
            for q in ['g.%s.drop().iterate()  ' % x,
                      'graph.tx().commit()     ']:
                print(q, gremlin.fetch(q))


if __name__ == '__main__':
    drop_all()
