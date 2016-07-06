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


def get_preferred_order():
    '''Gen classes in the order specified by Language.md (not CDM13.avdl).'''
    # $ echo `tr '()' '  ' < tools/cdm/enums.py | awk '/^class/ {print $2}'`
    return ('Instrumentationsource Principal Event Srcsink Integritytag'
            ' Confidentialitytag Subject Strength Derivation'
            ).split()[:-2]  # Suppress Strength & Derivation.


def get_cdm13_avdl_spec_order():
    # CDM13.avdl lacks Derivation & Strength, and adds
    # Edge, LocalAuth, TagOpCode, Value & Valuedata.
    # Well, actually TagOpCode subsumes Strength plus Derivation.TAG_OP_ENCODE.
    # Too bad that compression and encryption both come out as TAG_OP_ENCODE.
    # Presumably TAG_OP_ENCODE is lossless (reversible) unlike strong/med/weak.
    return ('Subject Srcsink Instrumentationsource Principal Event Edge Value'
            ' Valuedata Localauth Tagopcode Integritytag Confidentialitytag'
            ).split()


def get_grammar():
    return """
name = r'\w+'
enum = name '{' ( name ','? )+ '}' EOF
"""


def gen(fin, fout):
    parser = arpeggio.cleanpeg.ParserPEG(get_grammar(), 'enum')
    enum_re = re.compile(r'(```|\*/)\s*enum\s+([^}]+})')
    fout.write('from enum import Enum\n')
    out = {}
    for _, sect in enum_re.findall(fin.read()):
        parsed = list(filter(not_punct, parser.parse(strip_comments(sect))))
        klass = str(parsed[0]).replace('Type', '').capitalize()
        out[klass] = ('\n\nclass %s(Enum):\n    ' % klass
                      + '\n    '.join(fmt(parsed[1:])) + '\n')
    for klass in get_cdm13_avdl_spec_order():
        fout.write(out[klass])


def strip(s):
    '''Strip an overly verbose class name prefix from each identifier.'''
    # Note that CDM13 symbols are drawn from a global namespace rather than
    # from per-class namespaces, e.g. SOURCE_LINUX_AUDIT_TRACE does not
    # fall under SrcSinkType, and conversely SOURCE_ACCELEROMETER is not
    # an InstrumentationSource.
    # In the interest of being concise we strip name prefixes anyway,
    # as that happens to still leave us with globally unique names.
    if '_' in s:
        return re.sub(r'^[A-Z]+_', '', s)  # Class names lack underscore.
    # At this point we have an un-prefixed Strength (WEAK, MEDIUM, STRONG)
    # or a Derivation (COPY, ENCODE, COMPILE, ENCRYPT, OTHER).
    return s


def fmt(vals):
    return ['%s = %d' % (strip(str(val)), i)
            for i, val in enumerate(vals)]


if __name__ == '__main__':
    spec = os.path.expanduser(
        '~/Documents/bbn/ta3-serialization-schema/avro/CDM13.avdl')
    if not os.path.exists(spec):
        spec = os.path.expanduser('~/adapt/ingest/Ingest/Language.md')
    with open(os.path.expanduser('~/adapt/tools/cdm/enums.py'), 'w') as fout:
        gen(open(spec), fout)
