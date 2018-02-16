import query_handling as q
import fcbo_core as fcbo
import argparse
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
import itertools
import glob
import pathos.multiprocessing as mp
from concurrent.futures import ThreadPoolExecutor


code_flag_options=['python','C']
FCAoutput=collections.namedtuple('FCAoutput',['concepts','context','naming','itemsets'])



def namingByFileExtension(extension,disable_naming=False):
	#specifies whether context attributes (and objects) should be identified by their position in the context or by their name
	if 'fimi' not in extension:#case where the inputs are not in FIMI format
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
	
def fca(fca_context,min_support,named,file_out,proceed_flag=True): #run FCbO on context fca_context (based on the ported python code)
	#print('file_out',file_out)
	if min_support==0:
		concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
	else:
		concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
	fca_context.printConcepts(concepts,outputfile=file_out,namedentities=named)#output the concepts
	if proceed_flag==True:
		trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
		return FCAoutput(concepts=trans_concepts,context=fca_context,naming=named,itemsets=set())
	else:
		return "FCA done. Stopping."

def copyCfcaOutput2File(concept_file,names,outputfile=sys.stdout,namedentities=False):
	itemsets=parseFimiConceptFile(concept_file)
	if namedentities==True:
		itemsets_named={q.indicesToNames(e,names) for e in itemsets}
	else:
		itemsets_named=itemsets
	if outputfile!=sys.stdout:
		output=open(outputfile,'w')
	else:
		output=sys.stdout
	print('Named:'+str(namedentities)+'\n',file=output)
	for item in itemsets_named:
		print('#'.join([str(i) for i in item])+'\n',file=output)
	if output!=sys.stdout:
		output.close()
			

		
						
def fcaCReturn(fca_context,itemsets,proceed_flag,named,attributes):
	if proceed_flag==True:
		if named==True:
			trans_itemsets={frozenset(q.indicesToNames(tup,attributes)) for tup in itemsets}
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

def fcaC(fcbo_path,fca_context,min_support,sourcefile,fimifile,named,output,proceed_flag,fimi_flag,csv_flag=False,parallel_flag=False,cpus=1): #run FCbO/PCbO on context fca_context (using the original FCbO/PCbO C code)
	#if parallel_flag is set to True, PCbO is run, otherwise FCbO is run
	#fcbo_path is the the absolute path to the FCbO/PCbO executable
	#proceed_flag is set to True if concepts are to be analyzed and implication rules generated after concept generation with FCA
	#fimi_flag indicates whether the input file (sourcefile) is in FIMI format or not
	concepts_file=produceConceptfileName(sourcefile,output)
	csv=csv_flag
	if fimi_flag==False:
		fileext=os.path.splitext(sourcefile)[1].replace('.','')
		res=(q.getFimiFromQuery(sourcefile,fimifile,namedentities=named,csv_flag=csv) if fileext=='json' else q.convertCxtFileToFimi(sourcefile,fimifile))
	if parallel_flag==True:
		#print('applying PCbO\n')
		#print('command line subprocess',subprocess.list2cmdline([fcbo_path,'-P'+str(cpus),'-L4','-S'+str(min_support),'-V2',fimifile,concepts_file]))
		subprocess.call([fcbo_path,'-P'+str(cpus),'-L4','-S'+str(min_support),'-V2',fimifile,concepts_file]) #call to the PCbO executable
	else:
		#print('applying FCbO\n')
		#print('command line subprocess',subprocess.list2cmdline([fcbo_path,'-S'+str(min_support),'-V2',fimifile,concepts_file]))
		subprocess.call([fcbo_path,'-S'+str(min_support),'-V2',fimifile,concepts_file])#call to the FCbO executable
	itemsets=parseFimiConceptFile(concepts_file) #parsing the file resulting from the execution of FCbO or PCbO
	attributes=(fca_context.attributes if fimi_flag==False else []) #retrieving the attribute names
	copyCfcaOutput2File(concepts_file,attributes,outputfile=output,namedentities=named)
	if fimi_flag==False:	
		result=fcaCReturn(fca_context,itemsets,proceed_flag,named,fca_context.attributes)
	else:
		result=fcaCReturn(fca_context,itemsets,proceed_flag,False,[])	
	return result

#@profile	
def runFCA(specfile,inputfile,min_support,code_flag,fcbo_path,proceed_flag=True,outputfile=sys.stdout,disable_naming=False,csv_flag=False,parallel_flag=False,cpus=1):
	#min_support defines the minimum support (in percentage of the total number of objects) of the concepts to be generated. range of min_support: [0-1]
	#if parallel_flag is set to True, PCbO is executed, otherwise FCbO is executed (this is only valid if fca_algo is set to 'C', i.e if the original C code for FCbO/PCbO is used)
	#cpus is the number of processes to use for PCbO.
	if code_flag not in code_flag_options:#specifies whether we run FCA based on the ported Python code or the original C code
		flag='python' #if the user specifies an algorithm that doesn't exist, the FCA code that will be used is the python one
	else:
		flag=code_flag
	if specfile!='':
		fileext=os.path.splitext(specfile)[1].replace('.','')
	else:
		fileext=os.path.splitext(inputfile)[1].replace('.','')
	csv=csv_flag
	parallel=parallel_flag #indicates whether we run FCbO or PCbO
	ncpus=cpus
	named=namingByFileExtension(fileext,disable_naming)
	type_json=('context' if csv_flag==False else 'csv')
	if inputfile=='':
		fca_context=fcbo.Context(specfile,type_json)#we query localhost based on the specification file, parse it to a context or use the CSV files described in the specification file to produce a context
		print('case where only specification supplied')#if no input file is specified (which means a specification has been specified),
		if flag=='python':
			result=fca(fca_context,min_support,named,outputfile,proceed_flag)
		elif flag=='C':
			min_support=math.floor(min_support*fca_context.num_objects)# The original FCbO/PCbO defines minimum support as the minimum number of objects a concept should have.
			if type_json=='context': 
				print('case where the specification describes a query')#if no input file is specified (which means a specification has been specified),			
				fimifile=os.path.splitext(specfile)[0]+'.fimi'
			elif type_json=='csv':
				print('case where the specification describes csv files')
				fimifile=q.getFimiFromCSVSpec(specfile)
			result=fcaC(fcbo_path,fca_context,min_support,specfile,fimifile,named,outputfile,proceed_flag,False,csv,parallel,ncpus)
	elif specfile=='': #if no specification file is supplied 		
		print('case where only input file supplied')
		fca_context=fcbo.Context(inputfile,type_json)#a context input file has to be specified (in cxt or fimi format)
		if flag=='python':
			result=fca(fca_context,min_support,named,outputfile,proceed_flag)
		elif flag=='C':
			min_support=math.floor(min_support*fca_context.num_objects)
			if fileext=='fimi':
				result=fcaC(fcbo_path,fca_context,min_support,inputfile,inputfile,named,outputfile,proceed_flag,True,parallel,ncpus)
			else:
				fimifile=os.path.splitext(inputfile)[0]+'.fimi'
				result=fcaC(fcbo_path,fca_context,min_support,inputfile,fimifile,named,outputfile,proceed_flag,False,parallel,ncpus)		
	else: #case where both a query specification file and a context input file are specified
		print('case where both specification and input file supplied')
		fca_context=fcbo.Context(specfile,type_json) # a context is generated using the query specification file
		if flag=='python':
			if csv==False:#generation of a context file (in CXT or FIMI format) saved in inputfile (this file can be used as input for future experiments)
				res,obj_name,att_name=q.getQueryResFromSpecFile(specfile)
				q.convertQueryRes(res,obj_name,att_name,inputfile) 
			else:
				q.convertCSVRes(specfile,inputfile)
			result=fca(fca_context,min_support,named,outputfile,proceed_flag)
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
	
	
def runAnalysis(fca_context,trans_concepts,min_rule_conf,max_rule_conf,num_rules,namedentities=False,c_flag=False,output=sys.stdout):
	min_conf=min_rule_conf
	max_conf=max_rule_conf
	num_rules=num_rules
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	analysis.doImpRules(fca_context,itemsets,min_conf,num_rules,namedentities,output)
	analysis.doAntiImpRules(fca_context,itemsets,max_conf,num_rules,namedentities,output)
	analysis.doDisjRules(fca_context,itemsets,num_rules,namedentities,output)
	
def runAnalysisParallel(fca_context,trans_concepts,min_rule_conf,max_rule_conf,num_rules,namedentities=False,c_flag=False,output=sys.stdout):
	min_conf=min_rule_conf
	max_conf=max_rule_conf
	num_rules=num_rules
	if c_flag==False:
		itemsets={frozenset(c[1]) for c in trans_concepts}
	else:
		itemsets={frozenset(c) for c in trans_concepts}
	executors_list = []
	with ThreadPoolExecutor(max_workers=5) as executor:
		executors_list.append(executor.submit(analysis.doImpRules,fca_context,itemsets,min_conf,num_rules,namedentities))
		executors_list.append(executor.submit(analysis.doAntiImpRules,fca_context,itemsets,max_conf,num_rules,namedentities))
		executors_list.append(executor.submit(analysis.doDisjRules,fca_context,itemsets,num_rules,namedentities))
	for x in executors_list:
		analysis.printComputedRules(x.result(),fca_context,output)
		
	
	
def parseConceptFile(concept_file):
	with open(concept_file,'r') as f:
		content=f.read().replace('\n','#')
	named_regexp=re.compile('#Named:(?P<named>True|False)')
	named=bool(strtobool(re.search(named_regexp,content).group('named')))
	split_content=content.split('#')
	concept_regexp=re.compile('Extent:\s*(?P<extent>(\{(.*)\}|\{\}))\s*Intent:\s*(?P<intent>(\{(.*)\}|\{\}))')
	trans_concepts=set()
	if named==True:
		for l in split_content:
			m=re.search(concept_regexp,l)
			if m!=None:
				key=tuple(m.group('extent').replace('{','').replace('}','').split(','))
				if key==('',):
					key=()
				value=tuple(m.group('intent').replace('{','').replace('}','').split(','))
				if value==('',):
					value=()
				trans_concepts.add((key,value))
	else:
		for l in split_content:
			m=re.search(concept_regexp,l)
			if m!=None:
				key=tuple([int(i) if i!='' else '' for i in m.group('extent').replace('{','').replace('}','').split(',')])
				if key==('',):
					key=()
				value=tuple([int(i) if i!='' else '' for i in m.group('intent').replace('{','').replace('}','').split(',')])
				if value==('',):
					value=()
				trans_concepts.add((key,value))
	return {'transconcepts':trans_concepts,'named':named}
	
def parseFimiConceptFile(concept_file):
	#parse a concept file in FIMI format
	itemsets=set()
	with open(concept_file,'r') as f:
		fimi=f.read().splitlines()
	for l in fimi:
		if l=='':
			itemsets.add(tuple())
		else:
			itemsets.add(tuple(int(i) for i in l.split()))
	return itemsets

def parseCfcaOutput(cfca_output):
	#parse the output of the original C code for FCbO or PCbO
	itemsets=set()
	with open(cfca_output,'r') as f:
		fimi=f.read().splitlines()
	for l in fimi:
		if 'Named' in l:
			named_regexp=re.compile('Named:\s*(?P<named>True|False)')
			named=bool(strtobool(re.search(named_regexp,l).group('named')))
			#print('parsed naming',named)
		elif l=='':
			itemsets.add(tuple())
		else:
			if named==False:
				itemsets.add(tuple(int(i) for i in l.split('#')))
			else:
				itemsets.add(tuple(i for i in l.split('#')))
	return {'itemsets':itemsets,'named':named}

#@profile	
def FCAexecution(inputfile,specfile,fcbo_path,output_file,output_file_analysis,concept_file,disable_naming=False,min_support=0.5,min_conf=0.95,max_conf=0.05,num_rules=100,ncpus=1,csv=False,workflow='both',fca_algo='C'): 
	#runs the generation of concepts with FCA and/or the concept analysis 
	#if workflow is set to 'fca', only the concept generation is performed. If workflow is set to 'analysis', implication rule generation/concept analyis is run on the concepts stored in concept_file.
	#if workflow is set to 'both', concepts are generated with FCA before being analyzed.
	type_json=('context' if csv==False else 'csv') #specifies whether the json specification file passed as argument contains information about CSV file inputs or about queries to run on the database
	parallel=(True if ncpus>1 else False)
	if inputfile=='' and specfile=='':#at least a specification file or an input (context file) must be supplied)
		print('Either a specification file (in json format) or an input CXT or FIMI file is required',file=sys.stderr)
		sys.exit(1)# if not an error is thrown	
	if workflow!='analysis':
		print('Workflow',workflow)
		print('Starting FCA. Generating concepts...\n')	
		proceed_flag=(True if workflow=='both' else False)
		if disable_naming:
			naming=disable_naming
		else:
			naming=False
		res=runFCA(specfile,inputfile,min_support,fca_algo,fcbo_path,proceed_flag,outputfile=output_file,disable_naming=naming,csv_flag=csv,parallel_flag=parallel,cpus=ncpus)	
		print('FCA finished. Concepts generated')
		if workflow=='both':
			print('Starting analysis\n')
			fca_context=res.context
			if res.concepts!=set():
				trans_concepts=res.concepts
			else:
				trans_concepts=res.itemsets
			named=res.naming
			runAnalysisParallel(fca_context,trans_concepts,min_conf,max_conf,num_rules,namedentities=named,c_flag=(fca_algo=='C'),output=output_file_analysis)
			print('end analysis\n')
	else: #run the analysis of the concepts if the 'analysis' flag found
		print('workflow',workflow)
		if concept_file=='':
			print('A concept file is required for the analysis',file=sys.stderr)
			sys.exit(1)
		if inputfile!='':
			fca_context=fcbo.Context(inputfile)
		else:
			fca_context=fcbo.Context(specfile,query_port)
		if fca_algo=='python':
			parsed_concepts=parseConceptFile(concept_file)
			trans_concepts=parsed_concepts['transconcepts']
			named=parsed_concepts['named']
		else:
			parsed_concepts=parseCfcaOutput(concept_file)
			trans_concepts=parsed_concepts['itemsets']
			named=parsed_concepts['named']
		runAnalysisParallel(fca_context,trans_concepts,min_conf,max_conf,num_rules,namedentities=named,c_flag=(fca_algo=='C'),output=output_file_analysis)
		

def constructParamsList(name_tuple,**items):
	#function that constructs a named tuple based on a string defining the name of the named tuple and a list of properties with their values
	vars()[name_tuple]=collections.namedtuple(name_tuple, items.keys())
	return itertools.starmap(vars()[name_tuple], itertools.product(*items.values()))

def constructInputfilesList(directory,inputfile,specfile,conceptfile,csv_flag=[False]):
	return constructParamsList(name_tuple='inputfiles',dirs=directory,input_file=inputfile,spec_file=specfile,concept_file=conceptfile,csv=csv_flag)

def formParameterCombination(param):
	fca_params=collections.namedtuple('fca_params',['directory','inputfile','specfile','csv','conceptfile','outputfile','outputfile_analysis','disable_naming','min_support','min_conf','max_conf','num_rules','ncpus','workflow','fca_algo','fcbo_path'])
	inputFile=param.inputFiles.input_file
	conceptFile=param.inputFiles.concept_file
	specFile=param.inputFiles.spec_file
	input_directory=param.inputFiles.dirs
	csvFlag=param.inputFiles.csv
	minsupp=param.minsupports
	supp=str(minsupp).replace('.','_')
	minconf=param.minconfs
	conf=str(minconf).replace('.','_')
	if inputFile=='' and specFile==(''):
		print('Either a specification file (in json format) or an input CXT or FIMI file is required',file=sys.stderr)
		sys.exit(1)# if not an error is thrown	
	if specFile!='':
		respath=os.path.join(os.path.dirname(os.path.dirname(specFile)),'results_mp')
		outputFile=os.path.join(respath,os.path.split(os.path.dirname(input_directory))[1]+'_'+os.path.splitext(os.path.basename(specFile))[0]+'_concepts_support'+supp+'_conf'+conf+'.txt')
		outputanalysis=os.path.join(respath,os.path.split(os.path.dirname(input_directory))[1]+'_'+os.path.splitext(os.path.basename(specFile))[0]+'_analysis_support'+supp+'_conf'+conf+'.txt')
	else:
		respath=os.path.join(os.path.dirname(os.path.dirname(inputFile)),'results_mp')
		outputFile=os.path.join(respath,os.path.split(os.path.dirname(input_directory))[1]+'_'+os.path.splitext(os.path.basename(inputFile))[0]+'_concepts_support'+supp+'_conf'+conf+'.txt')
		outputanalysis=os.path.join(respath,os.path.split(os.path.dirname(input_directory))[1]+'_'+os.path.splitext(os.path.basename(inputFile))[0]+'_analysis_support'+supp+'_conf'+conf+'.txt')
	formatted_param=fca_params(directory=input_directory,inputfile=inputFile,specfile=specFile,csv=csvFlag,conceptfile=conceptFile,outputfile=outputFile,outputfile_analysis=outputanalysis,disable_naming=param.disableNaming,min_support=param.minsupports,min_conf=param.minconfs,max_conf=param.maxconfs,num_rules=param.numrules,ncpus=param.procs,workflow=param.work_flow,fca_algo=param.fcaAlgo,fcbo_path=param.fcboPath)
	return formatted_param
	
def formatParamsList(params):
	#returns a list of FCA input parameter combinations (to be passed to the FCAexecution function)
	params=list(params)
	param_list=[formParameterCombination(p) for p in params]
	return param_list
			
		
def FCAOneRun(formatted_params):
	#executes FCA for one combination of parameters
	res_directory=os.path.dirname(formatted_params.outputfile)
	if os.path.exists(res_directory)==False:
		os.mkdir(res_directory)
	os.chdir(formatted_params.directory)
	FCAexecution(formatted_params.inputfile,formatted_params.specfile,formatted_params.fcbo_path,formatted_params.outputfile,formatted_params.outputfile_analysis,formatted_params.conceptfile,disable_naming=formatted_params.disable_naming,min_support=formatted_params.min_support,min_conf=formatted_params.min_conf,max_conf=formatted_params.max_conf,num_rules=formatted_params.num_rules,ncpus=formatted_params.ncpus,csv=formatted_params.csv,workflow=formatted_params.workflow,fca_algo=formatted_params.fca_algo)

#@profile	
def runParallel(nb_processes,input_files,fcbo_path,disable_naming=False,min_support=[0.5],min_conf=[0.95],max_conf=[0.05],num_rules=[100],ncpus=[1],workflow='both',fca_algo='C'):
	#runs FCA in parallel
	params=constructParamsList(name_tuple='Params',inputFiles=input_files,minsupports=min_support,minconfs=min_conf,maxconfs=max_conf,numrules=num_rules,procs=ncpus,work_flow=[workflow],fcaAlgo=[fca_algo],disableNaming=[disable_naming],fcboPath=[fcbo_path])
	param_list=formatParamsList(params)
	pool=mp.ProcessingPool(nb_processes)
	results=pool.amap(FCAOneRun,[p for p in param_list])

	
	
if __name__=='__main__':
	Directory='./explore/csv' #root directory that holds the input files (csv)
	specfile=[os.path.join(Directory,'csvspec.json')] #path to the json specification file that describes the input CSV files
	fcbo_path='./fcbo-ins/fcbo' #path to FCbO or PCbO executable (in this case FCbO)
	#min_supp=[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9]
	#min_conf=[0.99,0.95,0.9,0.85,0.8,0.75,0.7,0.65,0.6,0.5]
	minsupp=[0.9] #list of minimum support thresholds
	minconf=[0.95]#list of minimum confidence thresholds
	#directories=glob.glob(Directory+'/*/')
	#directories=[Directory+'/theia/',Directory+'/cadets/'] #directories that hold the input files (csv)
	directories=[Directory+'/theia/']
	input_files=list(constructInputfilesList(directories,[''],specfile,[''],csv_flag=[True]))
	n_processes=3
	runParallel(n_processes,input_files,fcbo_path,disable_naming=False,min_support=minsupp,min_conf=minconf,max_conf=[0.05],num_rules=[100],ncpus=[1],workflow='both',fca_algo='python')

	


	
			
			
				
