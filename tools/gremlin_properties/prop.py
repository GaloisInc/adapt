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

import cdm.enums


class Prop:
    '''Models a gremlin node attribute, the "properties" map.'''

    def __init__(self, item):
        self.prop = item['properties']

    def __contains__(self, key):
        return key in self.prop

    def __getitem__(self, key):
        return self.prop[key][0]['value']

    def __str__(self):
        return 'properties: ' + ' '.join([key
                                          for key in sorted(self.prop.keys())])

# Note that max Instrumentationsource is WINDOWS_DIFT_FAROS (7),
# while max Source is GPS(18).

    def source(self):
        if 'source' in self.prop:
            return cdm.enums.Source(self.__getitem__('source'))
        return None
