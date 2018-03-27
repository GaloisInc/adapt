import re,itertools

def getShort(characters):
	reg=re.compile('(?P<short>\-{1}\w+)((\|.*)|\){1})')
	return re.match(reg,characters).group('short')

def checkIrrelevant(optionDesc,irrelevant_list):
	if any([i==getShort(optionDesc) for i in irrelevant_list])==True:
		return True
	else:
		return False


def createCodeFragment(code_fragment,optionDesc,irrelevant_list,list_opts,corr,corr2):
	if checkIrrelevant(optionDesc,irrelevant_list)==False:
		conds=set(list_opts)-set(optionDesc.replace(')','').split('|'))
		conds1='\t\t if [[ '+' || '.join([ """($2 == \""""+e+"""\")""" for e in conds])+' ]]'
		conds2='\t\t\t\t\t if [[ '+' || '.join([ """($a == \""""+e+"""\")""" for e in conds])+' ]]'
		code_list=code_fragment.replace('PORT+=(8080)',corr2[getShort(optionDesc)]).replace('PORT',corr[getShort(optionDesc)]).split('\n')
		code_list[1]=conds1
		code_list[9]=conds2
		code_list=[optionDesc]+code_list
		code='\n'.join(code_list)
		return code
		
if __name__ == '__main__':
	with open('fullParallelizedFCAPipeline.sh','r') as f:
		code=f.read()
	with open('/home/gberrada/code_fragment.txt','r') as f:
		code_fragment=f.read()
		
	r=re.compile('-{1}\w+\|-{2}[a-zA-Z_0-9]+\){1}|-{1}\w+\){1}')
	fullparse_opts=re.findall(r,code)
	parse_opts=list(itertools.chain.from_iterable([p.replace(')','').split('|') for p in fullparse_opts]))
	
	corr={'-pv':'PATH2CSV','-E':'REGEXP','-ms':'MIN_SUPPORT','-nr':'NUMBER_RULES','-fi':'FILES','-p':'PORT','-m':'MEM','-db':'DBKEYSPACE','-x':'CONTEXT_NAMES','-xs':'CONTEXT_SPEC_DIR','-fci':'FCA_INPUT','-rs':'RULE_SPEC_FILES','-rn':'RULE_NAMES','-rt':'RULE_MIN_THRESH','-oa':'ANALYSIS_OUTPUT','-cf':'CONCEPT_FILES','-co':'CONCEPT_OUTPUT'}
	irrelevant=['-i','-r','-seq','-vd','-ad','-cd','-cod','-ui','-fca','-c']
	corr2={'-pv':'','-E':"REGEXP+=( '*cadets-*cdm17.bin' '*trace-*cdm17.bin' '*five*cdm17.bin' )",'-ms':'MIN_SUPPORT+=(0)','-nr':"""NUMBER_RULES+=( "'*'" )""",'-fi':'','-p':'PORT+=(8080)','-m':'MEM+=(3000)','-db':"""DBKEYSPACE+=( "neo4j" )""",'-x':"""CONTEXT_NAMES+=( "ProcessEvent" )""",'-xs':"""CONTEXT_SPEC_DIR+=( "./fca/contextSpecFiles" )""",'-fci':'','-rs':"""RULE_SPEC_FILES+=( "./rulesSpecs/rules_positive_implication.json" )""",'-rn':"""RULE_NAMES+=( 	"implication"	)""",'-rt':'RULE_MIN_THRESH+=(0)','-oa':'','-cf':'','-co':''}			
	new_code=[createCodeFragment(code_fragment,o,irrelevant,parse_opts,corr,corr2) for o in fullparse_opts]
	with open('part_bash_code.sh','w') as f:
		for e in new_code:
			if e!=None:
				f.write(e)
	
	
	
	
	
	

