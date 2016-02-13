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
import os
import stat

__author__ = 'John.Hanley@parc.com'


class FsProxy(object):
    '''
    Allows a TA2 analysis host to interrogate files on TA1 Monitored Host.
    '''

    def __init__(self, db_client, prefix='aide.db_'):
        self.client = db_client
        self.prefix = prefix
        self.fields = 'name size mode hash uid gid'.split()
        self.system_file_owners = set([0])  # root, bin, admin, oracle, ...

    def _query_node(self, name):
        label = self.prefix + name
        query = 'g.V().hasLabel("%s")' % label
        return self.client.execute(query).data

    @functools.lru_cache()
    def is_present(self, name):
        '''Tests whether the named file exists in the filesystem.'''
        resp = self._query_node(name)  # Empty [] if not found.
        return len(resp) > 0

    @functools.lru_cache()
    def stat(self, name):
        resp = self._query_node(name)
        assert len(resp) > 0
        props = resp[0]['properties']
        assert props['vertexType'][0]['value'] == 'aide', props
        return dict([(f, props[f][0]['value'])
                     for f in self.fields])

    def is_locked_down(self, name):
        def _is_group_or_world_writable(name):
            mode = self.stat(name)['mode']
            group = bool(mode & stat.S_IWGRP)
            other = bool(mode & stat.S_IWOTH)
            if group or other:
                print('writable: ', name, mode, mode & stat.S_IWGRP, mode & stat.S_IWOTH)
            return group or other

        def _is_single_secure_dir(name):
            uid = self.stat(name)['uid']
            return (uid in self.system_file_owners and
                    not _is_group_or_world_writable(name))

        if name == '/':
            return _is_single_secure_dir(name)
        return (_is_single_secure_dir(name) and
                self.is_locked_down(os.path.dirname(name)))
