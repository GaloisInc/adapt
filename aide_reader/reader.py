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
Parses an AIDE filesystem report. (http://aide.sf.net)
'''

__author__ = 'John.Hanley@parc.com'

import base64
import binascii
import gzip
import hashlib
import re


class AideReader:
    def __init__(self, fspec):
        self.fin = gzip.open(fspec, 'rt', encoding='utf')

    def __iter__(self):
        db_spec = ('@@db_spec name lname attr perm inode uid gid size lcount'
                   ' acl xattrs selinux e2fsattrs bcount mtime ctime'
                   ' crc32 sha256 \n')
        self.header_re = re.compile(r'^# |^@@(begin_db$|end_db$|db_spec)')
        for i in range(4):
            line = next(self.fin)
            assert self.header_re.search(line), line
            if line.startswith('@@db_spec '):
                assert line == db_spec, line

        return self

    def __next__(self):
        '''Picks out the interesting part of the DB, decoding as we go.'''
        line = '# '
        while self.header_re.search(line):
            line = next(self.fin)
        (name, lname, attr, perm, inode, uid, gid, size, lcount,
         acl, xattrs, selinux, e2fsattrs, bcount, mtime, ctime,
         crc32, sha256) = line.split()
        # if lname == '0':
        return (from_octal(perm), b64_to_hex(sha256),
                int(uid), int(gid), int(size), name, lname)


def from_octal(perm):
    '''Converts octal file permission to integer.
    >>> from_octal('777')
    511
    >>> from_octal('644')
    420
    >>> from_octal('100644')
    33188
    '''
    return int(perm, 8)


def b64_to_hex(hash):
    '''Converts message digest to usual ASCII representation.
    >>> b64_to_hex('P4Jz5+tD605AlX3NMpkE681pM+PBOoeEdXwpqSnZdgM=')
    '3f8273e7eb43eb4e40957dcd329904ebcd6933e3c13a8784757c29a929d97603'
    >>> b64_to_hex('0')
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
    '''
    if hash == '0':  # e.g. no digest is computed for directories
        return hashlib.sha256(b'').hexdigest()
    b = base64.b64decode(hash, validate=True)
    return binascii.hexlify(b).decode('utf8')
