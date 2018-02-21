import sys,json
num_kind_rules=int(input('Number of types of rules\n'))
path_json=input('Path to json file to be generated\n')
if num_kind_rules<=0:
	sys.exit('Invalid number of types of rules. This value should be an integer greater or equal to 1.')
else:
	json_dic={'rules':{}}
	for i in range(num_kind_rules):
		type_rule=input("Type of rule "+str(i+1)+":\n")
		min_thresh=input("Minimum threshold for "+type_rule+" rules (number between 0 and 1):\n")
		num_rules=input("Number of rule violations to be displayed (* for all)\n")
		if num_rules.isdigit()==False and num_rules!='*':
			sys.exit('Incorrect number of rule violations. Expecting an integer or *')
		elif (min_thresh.replace('.','',1).isdigit()==False) or (min_thresh.replace('.','',1).isdigit()==True and (float(min_thresh)>1 or float(min_thresh)<0)):
			sys.exit('Incorrect minimal threshold value. Expecting a number between 0 and 1.')
		else:
			json_dic['rules'][type_rule]={'min_threshold':float(min_thresh),'num_rules':(int(num_rules) if num_rules!='*' else '*')}
	json.dump(json_dic,open(path_json,'w'),indent=4)
	
