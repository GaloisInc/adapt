#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import sys
filepath = sys.argv[1]
path, filename = os.path.split(filepath)
filename, ext = os.path.splitext(filename)
for i in os.listdir(os.getcwd()+'/'+path):
    file_i, ext = os.path.splitext(i)
    if i.startswith(filename+'_segmented') and ext == '.ttl':
        os.system('~/.virtualenvs/segmenter/bin/rdf2dot {2}/{0} > {2}{1}'.format(i, file_i+'.dot', os.getcwd()+'/'+path))
