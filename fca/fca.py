import input_processing as ip
import fcbo
#import argparse
import json
import os
import sys
import numpy
import concept_analysis as analysis
from distutils.util import strtobool
import collections
import subprocess
import shutil
import math
import re
from concurrent.futures import ThreadPoolExecutor
import itertools


code_flag_options=['python','C']
FCAoutput=collections.namedtuple('FCAoutput',['concepts','context','naming','itemsets'])


def namingByFileExtension(extension,disable_naming=False):#case where we use the query results directly
	if 'fimi' not in extension:
		if disable_naming==False:
			named=True
		else:
			named=False
	else: #fimi file case
		named=False
	return named


def naming(*disable_naming):
	if len(disable_naming)==0:
		named=False
	else:
		if disable_naming[0]==False:
			named=True
		else:
			named=False
	return named

def fca(fca_context,min_support,named,file_out,proceed_flag=True,quiet_flag=True): #run FCbO on context fca_context
	if min_support==0:
		concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
	else:
		concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
	if ((file_out!=sys.stdout) or (file_out==sys.stdout and quiet_flag==False)): 
		fca_context.printConcepts(concepts,outputfile=file_out,namedentities=named)#output the concepts
	if proceed_flag==True:
		trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
		return FCAoutput(concepts=trans_concepts,context=fca_context,naming=named,itemsets=set())
	else:
		return "FCA done. Stopping."

def copyCfcaOutput2File(concept_file,names,outputfile=sys.stdout,namedentities=False):
	itemsets=parseFimiConceptFile(concept_file)
	if namedentities==True:
		itemsets_named={ip.indicesToNames(e,names) for e in itemsets}
	else:
		itemsets_named=itemsets
	if outputfile!=sys.stdout:
		output=open(outputfile,'w')
	else:
		output=sys.stdout
	#print(itemsets_named)
	preamble=['Named:'+str(namedentities)+'\n']
	body=['#'.join([str(i) for i in item])+'\n' for item in itemsets_named]
	filecontent=''.join(preamble+body)
	print(filecontent,file=output)
	if output!=sys.stdout:
		output.close()




def fcaCReturn(fca_context,itemsets,proceed_flag,named,attributes):
	if proceed_flag==True:
		if named==True:
			trans_itemsets={frozenset(ip.indicesToNames(tup,attributes)) for tup in itemsets}
		else:
			trans_itemsets={frozenset(c) for c in itemsets}
		return FCAoutput(concepts=set(),context=fca_context,naming=named,itemsets=trans_itemsets)
	else:
		return "FCA done. Stopping."

def produceConceptfileName(sourcefile,outputfile):
	if outputfile!=sys.stdout:
		concepts_file=outputfile
	else:
		concepts_file=os.path.splitext(sourcefile)[0]+'_concepts.fimi'
	return concepts_file

def fcaC(fcbo_path,fca_context,min_support,sourcefile,fimifile,named,output,proceed_flag,fimi_flag,csv_flag=False,parallel_flag=False,cpus=1):
	concepts_file=produceConceptfileName(sourcefile,output)
	csv=csv_flag
	if fimi_flag==False:
		fileext=os.path.splitext(sourcefile)[1].replace('.','')
		res=(ip.getFimiFromQuery(sourcefile,fimifile,namedentities=named,csv_flag=csv) if fileext=='json' else ip.convertCxtFileToFimi(sourcefile,fimifile))
	if parallel_flag==True:
		subprocess.call([fcbo_path,'-P'+str(cpus),'-L4','-S'+str(min_support),'-V2',fimifile,concepts_file])
	else:
		subprocess.call([fcbo_path,'-S'+str(min_support),'-V2',fimifile,concepts_file])
	itemsets=parseFimiConceptFile(concepts_file)
	attributes=(fca_context.attributes if fimi_flag==False else [])
	copyCfcaOutput2File(concepts_file,attributes,outputfile=output,namedentities=named)
	if fimi_flag==False:
		result=fcaCReturn(fca_context,itemsets,proceed_flag,named,fca_context.attributes)
	else:
		result=fcaCReturn(fca_context,itemsets,proceed_flag,False,[])
	return result

def runFCA(specfile,inputfile,queryres,min_support,code_flag,fcbo_path,proceed_flag=True,outputfile=sys.stdout,quiet=True,disable_naming=False,csv_flag=False,parallel_flag=False,cpus=1,port=8080):
	#print('runFCA queryres',queryres)
	#add code_flag switch to allow bypass of my python code+add naming of c fimi results
	if code_flag not in code_flag_options:
		flag='python' #if the user specifies an algorithm that doesn't exist, the FCA code that will be used is the python one
	else:
		flag=code_flag
	if specfile!='':
		fileext=os.path.splitext(specfile)[1].replace('.','')
	else:
		fileext=os.path.splitext(inputfile)[1].replace('.','')
	csv=csv_flag
	parallel=parallel_flag
	ncpus=cpus
	named=namingByFileExtension(fileext,disable_naming)
	port_val=port
	type_json=('csv' if csv_flag==True else ('query' if queryres!='' else 'context'))
	#print('runFCA type_json',type_json)
	if inputfile=='':
		#print('runFCA constructing context')
		fca_context=fcbo.Context(specfile,queryres,type_json,port=port_val)#we query localhost based on the specification file), parse it to a context
		print('context constructed. Dimension',fca_context.num_objects,'x',fca_context.num_attributes)
		print('case where only specification supplied')#if no input file is specified (which means a specification has been specified),
		if flag=='python':
			#print('fca_context context', type(fca_context.context), len(fca_context.context))
			#print('fca_context attributes', type(fca_context.attributes), len(fca_context.attributes))
			#print('fca_context objects', type(fca_context.objects), len(fca_context.objects))
			result=fca(fca_context,min_support,named,outputfile,proceed_flag,quiet_flag=quiet)
		elif flag=='C':
			min_support=math.floor(min_support*fca_context.num_objects)
			if type_json=='context' or type_json=='query':
				print('case where the specification describes a query')#if no input file is specified (which means a specification has been specified),
				fimifile=os.path.splitext(specfile)[0]+'.fimi'
			elif type_json=='csv':
				print('case where the specification describes csv files')
				fimifile=ip.getFimiFromCSVSpec(specfile)
			result=fcaC(fcbo_path,fca_context,min_support,specfile,fimifile,named,outputfile,proceed_flag,False,csv,parallel,ncpus)
	elif specfile=='': #if no specification file is supplied
		print('case where only input file supplied')
		fca_context=fcbo.Context(inputfile,queryres,type_json,port=port_val)#a context input file has to be specified (in cxt or fimi format)
		print('context constructed. Dimension',fca_context.num_objects,'x',fca_context.num_attributes)
		if flag=='python':
			result=fca(fca_context,min_support,named,outputfile,proceed_flag,quiet_flag=quiet)
		elif flag=='C':
			min_support=math.floor(min_support*fca_context.num_objects)
			if fileext=='fimi':
				result=fcaC(fcbo_path,fca_context,min_support,inputfile,inputfile,named,outputfile,proceed_flag,True,parallel,ncpus)
			else:
				fimifile=os.path.splitext(inputfile)[0]+'.fimi'
				result=fcaC(fcbo_path,fca_context,min_support,inputfile,fimifile,named,outputfile,proceed_flag,False,parallel,ncpus)
	else: #case where both a query specification file and a context input file are specified
		print('case where both specification and input file supplied')
		fca_context=fcbo.Context(specfile,queryres,type_json,port=port_val) # a context is generated using the query specification file
		if flag=='python':
			if csv==False:
				res,obj_name,att_name=(ip.getQueryResFromSpecFile(specfile,port=port_val) if type_json=='context' else ip.getQueryResFromJsons(specfile,queryres))
				#if type_json=='context':
					#res,obj_name,att_name=ip.getQueryResFromSpecFile(specfile)
				#elif type_json=='query':
					#res,obj_name,att_name=ip.getQueryResFromJsons(specfile,queryres)
				ip.convertQueryRes(res,obj_name,att_name,inputfile) #generation of a context file saved in inputfile
			else:
				ip.convertCSVRes(specfile,inputfile)
			result=fca(fca_context,min_support,named,outputfile,proceed_flag,quiet_flag=quiet)
		elif flag=='C':
			min_support=math.floor(min_support*fca_context.num_objects)
			if os.path.splitext(inputfile)[1].replace('.','')=='fimi':
				fimifile=inputfile
			else:
				fimifile=os.path.splitext(specfile)[0]+'.fimi'
			result=fcaC(fcbo_path,fca_context,min_support,inputfile,fimifile,named,outputfile,proceed_flag,False,csv,parallel,ncpus)
			if os.path.splitext(inputfile)[1].replace('.','')=='cxt':
				fca_context.writeContext2Cxt(inputfile)
	return result


def runAnalysis_old(fca_context,trans_concepts,min_rule_conf,max_rule_conf,num_rules,rule_type=['implication','antiimplication','disjointness'],namedentities=False,c_flag=False,output=sys.stdout):
	min_conf=min_rule_conf
	max_conf=max_rule_conf
	num_rules=num_rules
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	if 'implication' in rule_type:
		analysis.doImpRules(fca_context,itemsets,min_conf,num_rules,namedentities,output)
	if 'anti-implication' in rule_type:
		analysis.doAntiImpRules(fca_context,itemsets,max_conf,num_rules,namedentities,output)
	if 'disjointness' in rule_type:
		analysis.doDisjRules(fca_context,itemsets,num_rules,namedentities,output)


def runAnalysisParallel_old(fca_context,trans_concepts,min_rule_conf,max_rule_conf,num_rules,rule_type=['implication','anti-implication','disjointness'],namedentities=False,c_flag=False,output=sys.stdout):
	min_conf=min_rule_conf
	max_conf=max_rule_conf
	num_rules=num_rules
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	executors_list = []
	with ThreadPoolExecutor(max_workers=5) as executor:
		if 'implication' in rule_type:
			executors_list.append(executor.submit(analysis.doImpRules,fca_context,itemsets,min_conf,num_rules,namedentities))
		if 'anti-implication' in rule_type:
			executors_list.append(executor.submit(analysis.doAntiImpRules,fca_context,itemsets,max_conf,num_rules,namedentities))
		if 'disjointness' in rule_type:
			executors_list.append(executor.submit(analysis.doDisjRules,fca_context,itemsets,num_rules,namedentities))
	for x in executors_list:
		analysis.printComputedRules(x.result(),fca_context,output,json_flag=True)



ruleFunction={'implication':analysis.doImpRules,'anti-implication':analysis.doAntiImpRules,'disjointness':analysis.doDisjRules}
supported_rules=['implication','anti-implication','disjointness','kulczynski','lift','leverage','imbalance']
def unpackRules(rules_spec_file):
	with open(rules_spec_file,'r') as f:
		rule_spec=json.load(f)
	return rule_spec['rules']

def runAnalysis(fca_context,trans_concepts,rule_spec_file,namedentities=False,c_flag=False,output=sys.stdout):
	rule_spec=unpackRules(rule_spec_file)
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	measureRules={k:v for k,v in rule_spec.items() if k in supported_rules and not('implication' in k or 'disjointness' in k)}
	imp_rules={k:v for k,v in rule_spec.items() if k in supported_rules and 'implication' in k}
	disjointness={k:v for k,v in rule_spec.items() if k in supported_rules and 'disjointness' in k}
	if imp_rules!={}:
		imp_res=[ruleFunction[rule](fca_context,itemsets,imp_rules[rule]['min_threshold'],imp_rules[rule]['num_rules'],namedentities) for rule in imp_rules.keys()]
	if disjointness!={}:
		dis_res=[analysis.doDisjRules(fca_context,itemsets,disjointness[rule]['num_rules'],namedentities,output) for rule in disjointness]
	if measureRules!={}:
		measure_res=analysis.doMeasureRules(measureRules,fca_context,itemsets,namedentities)
	analysis_res=measure_res+imp_res+dis_res
	analysis.printComputedRules(analysis_res,fca_context,output,json_flag=True)




def runAnalysisParallel(fca_context,trans_concepts,rule_spec_file,namedentities=False,c_flag=False,output=sys.stdout):
	rule_spec=unpackRules(rule_spec_file)
	print('rule_spec',rule_spec)
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	measureRules={k:v for k,v in rule_spec.items() if k in supported_rules and not('implication' in k or 'disjointness' in k)}
	print('measureRules',measureRules)
	imp_rules={k:v for k,v in rule_spec.items() if k in supported_rules and 'implication' in k}
	disjointness={k:v for k,v in rule_spec.items() if k in supported_rules and 'disjointness' in k}
	executors_list = []
	with ThreadPoolExecutor(max_workers=5) as executor:
		if measureRules!={}:
			executors_list+=[executor.submit(analysis.doMeasureRules,measureRules,fca_context,itemsets,namedentities)]
		if imp_rules!={}:
		   executors_list+=[executor.submit(ruleFunction[rule],fca_context,itemsets,imp_rules[rule]['min_threshold'],imp_rules[rule]['num_rules'],namedentities) for rule in imp_rules.keys()]
		if disjointness!={}:
		   executors_list+=[executor.submit(analysis.doDisjRules,fca_context,itemsets,disjointness[rule]['num_rules'],namedentities,output) for rule in disjointness]

		#for rule in rule_type:
			#if rule in ruleFunction and 'disjointness' not in rule:
				#executors_list.append(executor.submit(ruleFunction[rule],fca_context,itemsets,rule_thresholds[rule],num_rules,namedentities))
			#else:
				#executors_list.append(executor.submit(analysis.doDisjRules,fca_context,itemsets,num_rules,namedentities))
	for x in executors_list:
		analysis.printComputedRules(x.result(),fca_context,output,json_flag=True)

def parseConceptFile(concept_file):
	with open(concept_file,'r') as f:
		content=f.read().translate({ord('\n'):'#'})
	named_regexp=re.compile('#Named:(?P<named>True|False)')
	named=bool(strtobool(re.search(named_regexp,content).group('named')))
	split_content=content.split('#')
	concept_regexp=re.compile('Extent:\s*(?P<extent>(\{(.*)\}|\{\}))\s*Intent:\s*(?P<intent>(\{(.*)\}|\{\}))')
	trans_concepts=set()
	for l in split_content:
		m=re.search(concept_regexp,l)
		if m!=None:
			if named==True:
				key=tuple(m.group('extent').translate({ord('{'):'',ord('}'):''}).split(','))
				if key==('',):
					key=()
				value=tuple(m.group('intent').translate({ord('{'):'',ord('}'):''}).split(','))
				if value==('',):
					value=()
			else:
				extent=m.group('extent').translate({ord('{'):'',ord('}'):''}).split(',')
				intent=m.group('intent').translate({ord('{'):'',ord('}'):''}).split(',')
				key=tuple([int(i) if i!='' else '' for i in extent])
				if key==('',):
					key=()
				value=tuple([int(i) if i!='' else '' for i in intent])
				if value==('',):
					value=()
			trans_concepts.add((key,value))
	return {'transconcepts':trans_concepts,'named':named}

def parseFimiConceptFile(concept_file):
	itemsets=set()
	with open(concept_file,'r') as f:
		fimi=f.read().splitlines()
	for l in fimi:
		if l=='':
			itemsets.add(tuple())
		else:
			split_l=l.split()
			itemsets.add(tuple(int(i) for i in split_l))
	return itemsets

def parseCfcaOutput(cfca_output):
	itemsets=set()
	with open(cfca_output,'r') as f:
		fimi=f.read().splitlines()
	for l in fimi:
		if 'Named' in l:
			named_regexp=re.compile('Named:\s*(?P<named>True|False)')
			named=bool(strtobool(re.search(named_regexp,l).group('named')))
		elif l=='':
			itemsets.add(tuple())
		else:
			split_l=l.split('#')
			if named==False:
				itemsets.add(tuple(int(i) for i in split_l))
			else:
				itemsets.add(tuple(i for i in split_l))
	return {'itemsets':itemsets,'named':named}


def generateContextFiles(inputfile,outputfile=sys.stdout,csv_flag=False):
	ext_input=os.path.splitext(inputfile)[1][1:]
	ext_output=(os.path.splitext(outputfile)[1][1:] if outputfile==sys.stdout else ' ')
	if ext_input==ext_output:
		print('Nothing to do. Both input and output files have the same extension.')
		return
	else:
		csv=csv_flag
		if ext_input=='json':
			if ext_output=='fimi':
				ip.getFimiFromSpec(inputfile,outputfile,csv_flag=csv)
			elif ext_output=='cxt' or outputfile==sys.stdout:
				ip.convertSpec2File(inputfile,outputfile,csv_flag==csv)
		elif ext_input=='cxt':
			if ext_output!='': #write a fimi file by default even if the extension is not fimi
				ip.convertCxtFileToFimi(inputfile,outputfile)
			else:
				with open(inputfile, "r") as f:
					shutil.copyfileobj(f, sys.stdout)
		elif ext_input=='fimi':
			if ext_output!='': #write a cxt file by default even if the extension is not cxt
				ip.convertFimiFileToCxt(inputfile,outputfile)
			else:
				with open(inputfile, "r") as f:
					shutil.copyfileobj(f, sys.stdout)
		else:
			print('Cannot convert ',inputfile,' to ',outputfile,'. Inputfile format not supported.')
			
		
			
def fca_execution(inputfile,specfile,workflow,quiet_flag=True,port=8080,fca_algo='python',fcbo_path='',csv=False,ncpus=1,min_support=0,disable_naming=False,queryres='',output=sys.stdout,analysis_output=sys.stdout,rules_spec='./rulesSpecs/rules_implication_all.json',concept_file=''):
	port_val=port
	type_json=('context' if csv==False else 'csv')
	parallel=(True if ncpus>1 else False)
	if inputfile=='' and specfile=='':#at least a specification file or an input (context file) must be supplied)
		sys.exit('Either --specfile/-s or --inputfile/-i is required.')# if not an error is thrown
	if workflow=='context': #generate context files. By default, if no outputfile is specified, the input is formatted like a cxt file and printed on the screen.
		#Otherwise the output format is the format corresponding to the extension of the provided outputfile
		if inputfile!='' and specfile!='':
			sys.exit('--specfile/-s and --inputfile/-i cannot both be specified.')
		to_convert=(inputfile if inputfile!='' else specfile)
		outputfile=(output if output!=sys.stdout else sys.stdout)
		generateContextFiles(to_convert,outputfile,csv_flag=csv)
	elif (workflow!='context') and (workflow!='analysis'):
		print('Workflow',workflow)
		print('Starting FCA. Generating concepts...\n')
		output_file=output
		proceed_flag=(True if workflow=='both' else False)
		if disable_naming:
			naming=disable_naming
		else:
			naming=False
		res=runFCA(specfile,inputfile,queryres,min_support,fca_algo,fcbo_path,proceed_flag,outputfile=output_file,quiet=quiet_flag,disable_naming=naming,csv_flag=csv,parallel_flag=parallel,cpus=ncpus,port=port_val)
		print('FCA finished. Concepts generated')
		if workflow=='both':
			fca_context=res.context
			if res.concepts!=set():
				trans_concepts=res.concepts
			else:
				trans_concepts=res.itemsets
			named=res.naming
			runAnalysisParallel(fca_context,trans_concepts,rules_spec,namedentities=named,c_flag=(fca_algo=='C'),output=analysis_output)
	else: #run the analysis of the concepts if the 'analysis' flag found
		print('workflow',workflow)
		if concept_file=='':
			sys.exit('A concept file (--concept_file/-cf) is required for the analysis')
		if inputfile!='':
			fca_context=fcbo.Context(inputfile)
		else:
			fca_context=fcbo.Context(specfile,query_port)
		if fca_algo=='python':
			parsed_concepts=fca.parseConceptFile(concept_file)
			trans_concepts=parsed_concepts['transconcepts']
			named=parsed_concepts['named']
		else:
			parsed_concepts=fca.parseCfcaOutput(concept_file)
			trans_concepts=parsed_concepts['itemsets']
			named=parsed_concepts['named']
		runAnalysisParallel(fca_context,trans_concepts,rules_spec,namedentities=named,c_flag=(fca_algo=='C'),output=analysis_output)







