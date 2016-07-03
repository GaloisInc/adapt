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
Parse the CDM13 Language spec. to generate python enum definitions.
'''
import re
import os
import arpeggio.cleanpeg

__author__ = 'John.Hanley@parc.com'


def strip_comments(s):
    '''Nuke double-slash comments of the form:  // foo'''
    return re.sub(r' // .*$', '', s, flags=re.MULTILINE)


def not_punct(terminal):
    '''Predicate, false for three kinds of punctuation: , { }'''
    s = str(terminal)
    return len(s) > 1 or (s not in ',{}')


def get_grammar():
    return """
name = r'\w+'
enum = 'enum' name '{' ( name ','? )+ '}' EOF
"""


def gen(fin, fout):
    parser = arpeggio.cleanpeg.ParserPEG(get_grammar(), 'enum')
    for sect in fin.read().split('```'):
        if not sect.lstrip().startswith('enum '):
            continue
        parsed = list(filter(not_punct, parser.parse(strip_comments(sect))))
        assert parsed[0] == 'enum'
        fout.write('\n'.join(fmt(parsed[1], parsed[2:])) + '\n')


def fmt(name, vals):
    prefix = ''
    if name == 'InstrumentationSource':
        prefix = 'INSTRUMENTATION'
        # Defining both SOURCE_LINUX_AUDIT_TRACE = 0
        # and SOURCE_ACCELEROMETER = 0 seems unfortunate, so change one.
    if name in ['Strength', 'Derivation']:  # These use regrettably short IDs.
        prefix = str(name).upper() + '_'  # Avoid defining names like 'COPY'.
    return ['%s%s = %d' % (prefix, val, i)
            for i, val in enumerate(vals)]


if __name__ == '__main__':
    spec = os.path.expanduser('~/adapt/ingest/Ingest/Language.md')
    with open(os.path.expanduser('~/adapt/tools/cdm/enums.py'), 'w') as fout:
        gen(open(spec), fout)
