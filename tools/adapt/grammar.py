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

import ometa.runtime
import parsley
import re


class Grammar:

    @staticmethod
    def get_grammar():
        gr = """
# Declare some terminals.

marker_events_begin = LEAF
marker_events_end = LEAF

unusual_file_access = LEAF

access_sensitive_file = LEAF
internet_access = LEAF
exfil_execution = access_sensitive_file internet_access

# Looking at `arp -a` or sending intranet packet probes.
possible_network_scan = LEAF
network_scan = LEAF

# Looking at other principals on the current system, e.g. `ps`.
possible_system_scan = LEAF
system_scan = LEAF

exfil_channel =   http_post_activity
                | ssh_activity
exfil_format = compress_activity? encrypt_activity?
"""
        # Turn abbreviated declarations of foo = LEAF into: foo = 'foo'
        leaf_re = re.compile(r'^(\w+)(\s*=\s*)LEAF$', flags=re.MULTILINE)
        return parsley.makeGrammar(leaf_re.sub("\\1\\2'\\1'", gr), {})

    @staticmethod
    def parsley_demo(apt_grammar):
        terminal = 'access_sensitive_file'  # An example matching the grammar.
        g = apt_grammar(terminal)
        assert terminal == g.access_sensitive_file()

        g = apt_grammar('random junk ' + terminal)
        try:
            print(g.access_sensitive_file())
        except ometa.runtime.ParseError:
            pass  # The parsed string didn't match the grammar, as expected.
