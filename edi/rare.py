
"""
######################################################################
#### IMPORTANT: BEFORE USING THIS SCRIPT DO THE FOLLOWING STEPS:
#   $>  sudo pip3 install rpy2
#   $>  R
#   R prompt>  install.packages(c("stringr","ppls"))
##############
####python3 rare.py --input "./ProcessEvent.csv" --output "./scores.csv" --sup 0.05
##
#######################################################################
"""
import os
import sys
import argparse
import rpy2.robjects.packages as rpackages
import rpy2.robjects as robjects
from rpy2.robjects.packages import importr
from rpy2.robjects.vectors import StrVector

# save original cwd and R source cwd
oldcwd = os.getcwd()
rcwd = os.path.abspath(os.path.join(os.path.dirname(__file__),'R'))

#running an embedded R
robjects.r['options'](warn=0)
os.chdir(rcwd)
robjects.r('''source('rare.r')''')
r_rare_rule_mining_function = robjects.globalenv['rare.rules.ad']

parser = argparse.ArgumentParser(description='Rare Rule Mining')
parser.add_argument('--input', '-i',
					help='an input context file', required=True)
parser.add_argument('--output', '-o',
					help='an output scoring file', required=True)
parser.add_argument('--sup', '-s',
					help='support',
					default='0.05')


def extract_rare_rules(inputfile, outputscores, sup):
    print ("Running Association Rule Mining \n ")
    r_rare_rule_mining_function(inputfile, outputscores, sup)

if __name__ == "__main__" :
    args = parser.parse_args()
    os.chdir(oldcwd)
    print(args.input)
    input = os.path.abspath(args.input)
    print(input)
    scores = os.path.abspath(args.output)
    os.chdir(rcwd)
    extract_rare_rules(input,scores,args.sup)
