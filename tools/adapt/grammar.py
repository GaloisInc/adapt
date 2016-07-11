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

from enum import Enum
from parsley import makeGrammar


def any_(*args):
    return ' | '.join([str(arg)
                       for arg in args])


# from https://docs.python.org/3/library/enum.html#autonumber
class AutoNumber(Enum):
    def __new__(cls):
        value = len(cls.__members__) + 1
        obj = object.__new__(cls)
        obj._value_ = value
        return obj


class Grammar(Enum):
    '''
    Terminals of our APT Attack Grammar.
    They are simply unique symbols; numeric value is ignored.
    '''
    http_post_activity = 1
    ssh_activity = 2
    exfil_channel = makeGrammar('x = ' + any_(http_post_activity, ssh_activity), {})
# exfil_execution = exfil_channel 'TCP'
# exfil_format = 'compress_activity'? 'encrypt_activity'?
