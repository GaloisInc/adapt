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
Classifies activities found in subgraphs of a SPADE trace.
'''

# from prov.model import Identifier, Literal, Namespace, PROV, ProvBundle, XSD

# c.f. http://gremlinrestclient.readthedocs.org/en/latest/
# and  https://github.com/davebshow/gremlinrestclient
# sudo -H pip3 install gremlinrestclient
import gremlinrestclient
import argparse
import classify
import logging
import re

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.INFO)


def get_nodes(db_client):
    '''Returns the interesting part of each node (its properties).'''

    # sri_label_re = re.compile(r'^http://spade.csl.sri.com/#:[a-f\d]{64}$')

    edges = list(db_client.execute("g.E()").data)
    assert len(edges) > 0, len(edges)

    nodes = db_client.execute("g.V()").data
    for node in nodes:
        assert node['id'] >= 0, node
        assert node['type'] == 'vertex', node
        # assert sri_label_re.search(node['label']), node
        yield node['properties']


def get_re_classifier():
    c = []
    for rex, classification in [
            (r'audit:commandline="cat /tmp/timestamp.txt",',
             'step3_distractor'),
            ]:
        c.append((re.compile(rex), classification))
    return c


def add_vertex(client, cmd, classification):
    bindings = {'p1': cmd, 'p2': cmd, 'p3': classification}
    resp = client.execute("graph.addVertex(label, p1,"
                          " 'name', p2,"
                          " 'classification', p3,"
                          " 'vertexType', 'classification')",
                          bindings=bindings)
    log.debug(repr(resp.data))


def classify_provn_events(url):
    c = get_re_classifier()
    del(c)
    exfil_detect = classify.ExfilDetector()
    exfil_detect.test_is_sensitive_file()
    esc_detect = classify.Escalation()
    client = gremlinrestclient.GremlinRestClient(url=url)

    # Edges currently are one of { used, wasGeneratedBy, wasInformedBy }.

    # Iterate through all TA1-observed event nodes.
    for event in get_nodes(client):
        if ('programName' in event and
                'commandLine' in event):
            cmds = event['commandLine']
            assert len(cmds) == 1, cmds  # Actually, there's just a single cmd.

            cmd = cmds[0]['value']
            if exfil_detect.is_exfil(cmd):
                # assert exfil_detect.cmd == 'nc', cmd
                classification = 'step4_exfiltrate_sensitive_file'
                sudo_env = r'sudo env PATH=[/\w:\.-]+ LD_LIB[=/\w:-]+ +'
                cmd = re.sub(sudo_env, '', cmd)
                add_vertex(client, cmd, classification)
                print('\n' + classification)
            exfil_detect.remember(cmd)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in subgraphs of a SPADE trace.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    classify_provn_events(args.db_url)
