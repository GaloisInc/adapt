import ast,re,itertools,operator
 

 
def subsearch(code_list,parse_options,regexp,jump_to_option_line):
	r2=re.compile("\$[a2]\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'")
	bugs=[]
	for i in range(len(code_list)):
		if re.search(regexp,code_list[i]):
			option=[e.strip() for e in code_list[i-jump_to_option_line].replace(')','').split('|')]
			line_number=i+1
			#print('line_number',line_number)
			tested_line=re.search(regexp,code_list[i]).group().replace('[[','[').replace(']]',']')
			tested_line=tested_line.replace(' || ',',').replace('"',"'")
			tested_line=tested_line.replace(')',"""\"""").replace('(',"""\"""")
			tested_line=ast.literal_eval(tested_line)
			tested_line=[e.strip() for e in tested_line]
			#print('tested_line',tested_line)
			opt_tested_line=[re.search(r2,e).group('opt') for e in tested_line]
			if len(set(parse_options)-set(option)-set(opt_tested_line))!=0:
				duplicate_option=set([x for x in opt_tested_line if opt_tested_line.count(x) > 1])-set(option)
				missing_option=set(parse_options)-set(option)-set(opt_tested_line)
				bugs.append([line_number,'Affected option: '+'|'.join(option)+' at line '+str(line_number-jump_to_option_line)+'\nBuggy line: '+str(line_number)+'\n Duplicate option: '+str(duplicate_option)+'\n Missing option: '+str(missing_option)])
	return bugs
				
def search_faulty_lines(code_list,parse_options):	
	r3=re.compile('\[{2}\s*((\\s*\|{2}\\s*)*\\(\\$2\\s+\\={2}\\s+"(?P<opt>-{1,2}[a-zA-Z_]+)"\\s*\\)(\\s*\\|{2}\s*)*)+\s*\]{2}')
	r4=re.compile('\[{2}\s*((\\s*\|{2}\\s*)*\\(\\$a\\s+\\={2}\\s+"(?P<opt>-{1,2}[a-zA-Z_]+)"\\s*\\)(\\s*\\|{2}\s*)*)+\s*\]{2}')
	bugs1=subsearch(code_list,parse_options,r3,2)
	#print('first subsearch done')
	bugs2=subsearch(code_list,parse_options,r4,10)
	bugs=bugs1+bugs2
	bugs.sort(key=operator.itemgetter(0))
	for b in bugs:
		print(b[1])
	#print('search done')
	
if __name__ == '__main__':
	with open('test_specconstruction.sh','r') as f:
		code=f.read()
	code_list=code.split('\n')
	r=re.compile('-{1}\w+\|-{2}[a-zA-Z_]+\){1}')
#r2=re.compile("\$2\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'")
	parse_opts=re.findall(r,code)
	parse_opts=list(itertools.chain.from_iterable([p.replace(')','').split('|') for p in parse_opts]))
	#print(parse_opts)
	search_faulty_lines(code_list,parse_opts)
