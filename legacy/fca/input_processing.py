#! /usr/bin/env python3

import requests, readline, sys, json
import collections, re
import os
import dask.dataframe as dd

def tupToIndices(tup):
	return tuple(i for i in range(len(tup)) if tup[i]==1)
	
def indicesToNames(indices_tup,names_list):
	return tuple(names_list[e] for e in indices_tup)

def getQuery(query,port=8080):
	#gets query result as json
    try:
        resp = requests.post("http://localhost:"+str(port)+"/query/json", data={"query": query},timeout=300)
        print('Response: '+ str(resp.status_code))
        return (resp.json())
    except Exception as e:
        print("There was an error processing your query:")
        print(e)

def generateQueryResultFile(resfile,query,port=8080):
	with open(resfile,'w') as f:
		f.write(getQuery(query,port))
        
        
def printResp(resp):  
	# prints query response
    if isinstance(resp,list):
        for line in resp:
            print(line)
    else:
        print(resp)
        
def loadSpec(specfile,csv_flag=False):
	key=('csv params' if csv_flag==True else 'context specification')
	with open(specfile,'r') as f:
		spec=json.load(f)[key]
	return spec
	
def mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join):
	#joins two csv tables (read from files) based on some specified keys (csv1_key for file csv1 and csv2_key for file csv2). type_join specifies the type of join to perform
	table1=dd.read_csv(csv1,low_memory=False)
	table2=dd.read_csv(csv2,low_memory=False)
	merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
	return merged_table
	
def constructDictFromCSVSlice(merged_table,object_name,attribute_name):
	table_slice=merged_table[[object_name,attribute_name]]
	dico={}
	for row in table_slice.itertuples():
		key=getattr(row,object_name)
		att=getattr(row,attribute_name)
		if key not in dico.keys():
			dico[key]=[att]
		else:
			if att not in dico[key]:
				dico[key].append(att)
	return dico
	
def constructDictFromCSVFiles_old(csv1,csv2,csv1_key,csv2_key,object_name,attribute_name,type_join):
	merged_table=mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join)
	dico=constructDictFromCSVSlice(merged_table,object_name,attribute_name)
	return dico
	
def constructDictFromCSVFiles(csv1,csv2,csv1_key,csv2_key,object_name,attribute_name,type_join):
	if csv1!=csv2 or type_join.lower()!='none':
		print('2 different csvs')
		table1=dd.read_csv(csv1,header=0,usecols=[csv1_key,object_name],low_memory=False)
		table2=dd.read_csv(csv2,header=0,usecols=[csv2_key,attribute_name],low_memory=False)
		merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
	else:
		print('no joins')
		merged_table=dd.read_csv(csv1,header=0,usecols=[csv1_key,object_name,attribute_name],low_memory=False)
	dico=collections.defaultdict(list)
	for row in merged_table.itertuples():
		key=getattr(row,object_name)
		att=getattr(row,attribute_name)
		dico[key].append(att)
	return dico
	
def writeCxtFile(objects,attributes,context,cxtfile):
	objs=["''\n" if obj=='' else str(obj)+'\n' for obj in objects]
	fullcontext=['\n'.join(context).translate({ord("1"):'X',ord("0"):'.'})]
	atts=['\n'.join([str(att) for att in attributes])]+['\n']
	preamble=['\n'.join(['B\n',str(len(objects)),str(len(attributes)),'\n'])]
	filecontent=''.join(preamble+objs+atts+fullcontext)
	if cxtfile==sys.stdout:
		print(filecontent)
	else:
		with open(cxtfile,'w') as f:
			f.write(filecontent)
			
def writeContextCSVFile(objects,attributes,context,cxtcsvfile):
	#print(type(objects[0]))
	#print(type(attributes[0]))
	#print(type(context[0]))
	filecontent=','.join([' ']+attributes)+'\n'+'\n'.join([objects[i]+','+','.join(list(context[i])) for i in range(len(objects))])
	with open(cxtcsvfile,'w') as f:
		f.write(filecontent)
		
def writeFimiFile(context,fimifile):
	filecontent='\n'.join([' '.join([str(i) for i in v[1]]) for v in context])
	with open(fimifile,'w') as f:
		f.write(filecontent)
		
def writeFimiFileV2(context,fimifile):
	filecontent='\n'.join([' '.join([str(i) for i in v]) for v in context])
	with open(fimifile,'w') as f:
		f.write(filecontent)
		
def convertDict2File(dictionary,filename):
	extension=(os.path.splitext(filename)[1][1:] if filename!=sys.stdout else '')
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	elif extension=='csv':
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dictionary.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['1' if i in c[1] else '0' for i in range(len(attributes))])) for c in precontext])
		writeContextCSVFile(objects,attributes,fullcontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dictionary.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,filename)
			
def convertCSV2File(csv1,csv2,csv1_key,csv2_key,type_join,object_name,attribute_name,filename):
	#merged_table=mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join)
	dico=constructDictFromCSVFiles(csv1,csv2,csv1_key,csv2_key,object_name,attribute_name,type_join)
	convertDict2File(dico,filename)
	
def getFimiFromCSVSpec(specfile):
	spec=loadSpec(specfile,csv_flag=True)
	file1,file2,key1,key2,join,object_name,attribute_name=spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'],spec['objects'],spec['attributes']
	fimifile=''.join([os.path.splitext(file1)[0],'_',os.path.splitext(os.path.basename(file2))[0],'.fimi'])
	convertCSV2File(file1,file2,key1,key2,join,object_name,attribute_name,fimifile)
	return fimifile
		  
        
def getQueryResFromSpecFile(specfile,port=''):
	#loads a json specification file (defining a query) and returns the results (in json format) of the query definined in the specification file 
	#as well as the types of objects and attributes
	spec=loadSpec(specfile,csv_flag=False)
	query=spec['query']
	obj_name=spec['objects']
	att_name=(spec['attributes'].strip() if ',' not in spec['attributes'] else [s.strip() for s in spec['attributes'].split(',')])
	if port=='':
		port_val=(int(spec['port']) if 'port' in spec.keys() else 8080)
	else:
		port_val=port
	query_res=getQuery(query,port_val)
	return query_res,obj_name,att_name
	
	
def getQueryResFromJsons(specfile,resfile):
	#loads a json specification file (defining a query) and returns the results (in json format) of the query definined in the specification file 
	#as well as the types of objects and attributes
	spec=loadSpec(specfile,csv_flag=False)
	query=spec['query']
	obj_name=spec['objects']
	att_name=spec['attributes']
	port=(int(spec['port']) if 'port' in spec.keys() else 8080)
	query_res=json.load(open(resfile,'r'))
	return query_res,obj_name,att_name
	



def convertQueryRes2Dict(json,obj_name,att_name):
	dic = collections.defaultdict(list)
	if type(att_name)==str:
		for e in json:
			dic[e[obj_name]].append(e[att_name])
	elif type(att_name)==list:
		for e in json:
			dic[e[obj_name]].extend([str(e[a]) for a in att_name])
	#for e in json:
		#dic[e[obj_name]].append(e[att_name])
	return dic
        
def convertQueryRes2File(json,obj_name,att_name,filename):
	#converts query results (json) to cxt or fimi file (filename being the path to the file, a .cxt file (resp. .fimi file) is created if the extension of filename is cxt (resp. fimi))
	dic=convertQueryRes2Dict(json,obj_name,att_name)
	convertDict2File(dic,filename)
			
def convertSpec2File(specfile,outputfile,csv_flag=False,port=''):
	if csv_flag==False:
		query_res,obj_name,att_name=getQueryResFromSpecFile(specfile,port)
		convertQueryRes2File(query_res,obj_name,att_name,outputfile)
	else:
		convertCSVRes(specfile,outputfile)
	

			
def convertQueryRes(json,obj_name,att_name,filename=sys.stdout):
	extension=(os.path.splitext(filename)[1][1:] if filename!=sys.stdout else '')
	if extension=='cxt' or extension=='fimi' or extension=='':
		convertQueryRes2File(json,obj_name,att_name,filename)
	else:
		print('Extension of destination file not supported: '+extension+'instead of .cxt or .fimi')
		
def convertCSVRes(specfile,outputfile=sys.stdout):
	extension=(os.path.splitext(outputfile)[1][1:] if filename!=sys.stdout else '')
	spec=loadSpec(specfile,csv_flag=True)
	dico=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'])
	obj_name=spec['objects']
	att_name=spec['attributes']
	if extension=='cxt' or extension=='fimi' or extension=='':
		convertDict2File(dico,outputfile)
	else:
		print('Extension of destination file not supported: '+extension+'instead of .cxt or .fimi')
				
def convertQueryRes2FimiWithEntities(query_output,obj_name,att_name,fimifile,csv_flag=False):
	#converts query results (json/merged_table from csv files) to fimifile (fimifile being the path to the fimi file)
	dic=(convertQueryRes2Dict(query_output,obj_name,att_name) if csv_flag==False else constructDictFromCSVSlice(query_output,obj_name,att_name))
	attributes=list({e for val in dic.values() for e in val})
	precontext=[(k,sorted([attributes.index(e) for e in dic[k]])) for k in dic.keys()]
	objs=[l[0] for l in precontext]
	writeFimiFile(precontext,fimifile)
	return {'attributes':attributes,'objects':objs}
	
def convertSpec2Fimi(query_output,obj_name,att_name,fimifile,csv_flag=False):
	#converts query results (json/merged_table from csv files) to fimifile (fimifile being the path to the fimi file)
	dic=(convertQueryRes2Dict(query_output,obj_name,att_name) if csv_flag==False else constructDictFromCSVSlice(query_output,obj_name,att_name))
	attributes=list({e for val in dic.values() for e in val})
	precontext=[(k,sorted([attributes.index(e) for e in dic[k]])) for k in dic.keys()]
	writeFimiFile(precontext,fimifile)

			
def getFimiFromSpec(specfile,fimifile,csv_flag=False):
	if csv_flag==False:
		query_res,obj_name,att_name=getQueryResFromSpecFile(specfile)
	else:
		spec=loadSpec(specfile,csv_flag=True)
		query_res=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'])
		obj_name=spec['objects']
		att_name=spec['attributes']
	convertSpec2Fimi(query_res,obj_name,att_name,fimifile,csv_flag)
	print(''.join(['converted result of query from ',specfile,' to ',fimifile]))
	
def getFimiFromQuery(specfile,fimifile,namedentities=True,csv_flag=False):
	if csv_flag==False:
		query_res,obj_name,att_name=getQueryResFromSpecFile(specfile)
	else:
		spec=loadSpec(specfile,csv_flag=True)
		query_res=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'])
		obj_name=spec['objects']
		att_name=spec['attributes']
	res=convertQueryRes2FimiWithEntities(query_res,obj_name,att_name,fimifile,csv_flag)
	if namedentities==True:
		return res
	else:
		return ''.join(['converted result of query from ',specfile,' to ',fimifile])
			
FPGrowth_Params=collections.namedtuple('FPGrowth_Params',['inputFPgrowth','objects','attributes'])

def convertQueryRes2FPgrowthInput(json,obj_name,att_name):
	#converts query results (json) to input format required by pyfpgrowth package (list of lists, in which each row i corresponds to the list 
	# of attributes the object at row i of the context of the matrix has and attributes are identified by their position in the context matrix)
	dic=(convertQueryRes2Dict(query_output,obj_name,att_name) if csv_flag==False else constructDictFromCSVSlice(query_output,obj_name,att_name))
	atts=list({e for val in dic.values() for e in val})
	list_params=[(k,sorted([atts.index(e) for e in dic[k]])) for k in dic.keys()]
	objs,inputfp=zip(*list_params)
	res=FPGrowth_Params(inputFPgrowth=inputfp,objects=objs,attributes=atts)
	return res
	

def convertCxtFileToFimi(cxtfile,fimifile):
	with open(cxtfile,'r') as f:
		cxt=f.read()
	r=re.compile('B\s{2}(?P<num_obj>\d*)\s(?P<num_att>\d*)\s{2}(?P<vals>(.*\s*)*)',re.M)
	m=re.match(r,cxt)
	num_objects,num_attributes,vals=int(m.group('num_obj')),int(m.group('num_att')),m.group('vals')
	vals_split=vals.split('\n',num_objects)
	vals_split2=vals_split[-1].split('\n',num_attributes)
	objects,attributes,cxt_context=vals_split[:-1],vals_split2[:-1],vals_split2[-1].split()
	context=[[i for i in range(len(e)) if e[i].lower()=='x'] for e in cxt_context]
	writeFimiFileV2(context,fimifile)
	
	
def convertCxtFileToContextCSV(cxtfile,csvfile):
	with open(cxtfile,'r') as f:
		cxt=f.read()
	r=re.compile('B\s{2}(?P<num_obj>\d*)\s(?P<num_att>\d*)\s{2}(?P<vals>(.*\s*)*)',re.M)
	m=re.match(r,cxt)
	num_objects,num_attributes,vals=int(m.group('num_obj')),int(m.group('num_att')),m.group('vals')
	vals_split=vals.split('\n',num_objects)
	vals_split2=vals_split[-1].split('\n',num_attributes)
	objects,attributes,cxt_context=vals_split[:-1],vals_split2[:-1],vals_split2[-1].split()
	context=[e.translate({ord('.'):'0,',ord('X'):'1,'})[:-1] for e in cxt_context]
	filecontent=','.join([' ']+attributes)+'\n'+'\n'.join([objects[i]+','+context[i] for i in range(num_objects)])
	with open(csvfile,'w') as f:
		f.write(filecontent)
	
	
def num2roman(num):
	num_map = [(10000,'|M|'),(1000, 'M'), (900, 'CM'), (500, 'D'), (400, 'CD'), (100, 'C'), (90, 'XC'),(50, 'L'), (40, 'XL'), (10, 'X'), (9, 'IX'), (5, 'V'), (4, 'IV'), (1, 'I')]
	roman = ''
	while num > 0:
		for i, r in num_map:
			while num >= i:
				roman += r
				num -= i
	if set(roman)=={'M','|'}:
		roman=num2roman(roman.count('|M|'))+'*'+'|M|'
	return roman
	
def convertFimiFileToCxt(fimifile,cxtfile):
	with open(fimifile,'r') as f:
		fimi=[[int(i) for i in s.split()] for s in f.read().splitlines()]
	num_objects,num_attributes=len(fimi),numpy.max(fimi)-numpy.min(fimi)+1
	objects=[str(i) for i in range(num_objects)]
	attributes=[num2roman(i) for i in range(1,num_attributes+1)]
	context=[''.join(['X' if i in e else '.' for i in range(num_attributes)]) for e in fimi]
	writeCxtFile(objects,attributes,context,cxtfile)


	

