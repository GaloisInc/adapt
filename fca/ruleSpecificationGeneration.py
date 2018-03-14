import sys,json
import argparse
from joblib import Parallel,delayed

def specGeneration(dict_rule_properties,path_json):
	json_dic={'rules':{}}
	#print(paths)
	#print(dict_rule_properties)
	for k,v in dict_rule_properties.items():
		type_rule=k
		min_thresh=v[0]
		num_rules=v[1]
		if num_rules.isdigit()==False and num_rules!='*':
			sys.exit('Incorrect number of rule violations. Expecting an integer or *')
		elif (min_thresh.replace('.','',1).isdigit()==False) or (min_thresh.replace('.','',1).isdigit()==True and (float(min_thresh)>1 or float(min_thresh)<0)):
			sys.exit('Incorrect minimal threshold value. Expecting a number between 0 and 1.')
		else:
			json_dic['rules'][type_rule]={'min_threshold':float(min_thresh),'num_rules':(int(num_rules) if num_rules!='*' else '*')}
	#print('json_dic',json_dic)
	json.dump(json_dic,open(path_json,'w'),indent=4)
	
parser = argparse.ArgumentParser()
parser.add_argument('--rule_properties','-r',help="Properties associated with a rule (rule name, minimum threshold associated with rule, number of rules to output, location where rule specification should be saved). If trying to create more than one rule specification, use this option multiple times", nargs=4, action='append')

if __name__ == '__main__':
	args=parser.parse_args()
	#generate dictionary from command line arguments
	rule_properties=args.rule_properties
	paths=dict((v[0],v[-1]) for v in rule_properties)
	dict_rule_properties=(dict((v[0],v[1:-1]) for v in rule_properties))
	#print(paths)
	#print(dict_rule_properties)
	#map(specGeneration(dict_rule_properties,paths))
	Parallel(n_jobs=-1)(delayed(specGeneration)({k:dict_rule_properties[k]},paths[k]) for k in paths.keys())
