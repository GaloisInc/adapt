#! /usr/bin/env python3

import sys, getopt

options_list, _ = getopt.getopt(sys.argv[1:], 'i:o:')
options = dict(options_list)

if '-i' in options and '-o' in options:
    inputFile = open(options['-i'],'r')
    outputFile = open(options['-o'],'w')

    for line in inputFile.readlines():
        outputFile.write(str(abs(hash(line) / float(sys.maxsize))) + '\n')

    inputFile.close()
    outputFile.close()
else:
    print('Expected an input and output file')



