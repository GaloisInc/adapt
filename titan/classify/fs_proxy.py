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

import functools
import logging

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())  # log to console (stdout)
log.setLevel(logging.INFO)


class FsProxy(object):
    '''
    Allows a TA2 analysis host to interrogate files on TA1 Monitored Host.
    '''

    def __init__(self, db_client, prefix='aide.db_'):
        self.client = db_client
        self.prefix = prefix
        self.cache = {}
        self.fields = 'name size mode hash uid gid'.split()

    @functools.lru_cache()
    def stat(self, name):
        label = self.prefix + name
        query = 'g.V().hasLabel("%s")' % label
        resp = self.client.execute(query).data
        assert len(resp) == 1, resp
        props = resp[0]['properties']
        assert props['vertexType'][0]['value'] == 'aide', props
        return dict([(f, props[f][0]['value'])
                     for f in self.fields])
