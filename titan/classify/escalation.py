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

import os

__author__ = 'John.Hanley@parc.com'


class Escalation(object):
    '''
    Detects privilege escalation events (specifically escalation to root).
    '''

    def __init__(self, fs_proxy):
        self.fs = fs_proxy

    def is_escalation(self, event):
        assert event['vertexType'][0]['value'] == 'unitOfExecution', event
        uids = event['UID'][0]['value']  # four of them
        executing_uid = int(uids.split()[0])
        if 'CWD' not in event:
            # Grrrr. Sometimes this happens when programName is 'ls'.
            return False
        cwd = event['CWD'][0]['value']
        prog = event['programName'][0]['value']
        fspec = os.path.normpath(os.path.join(cwd, prog))
        # print(fspec)
        if not self.fs.is_present(fspec):
            return False  # e.g. /usr/share/ls where cwd is /usr/share
        if executing_uid != 0:
            return False  # We detect alice -> root, but not alice -> bob.
        return not self.fs.is_locked_down(fspec)
