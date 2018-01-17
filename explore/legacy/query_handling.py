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
        resp = requests.post("http://localhost:"+str(port)+"/query/generic", data={"query": query},timeout=300)
        print('Response: '+ str(resp.status_code))
        return (resp.json())
    except Exception as e:
        print("There was an error processing your query:")
        print(e)
        
        
def printResp(resp):  
	# prints query response
    if isinstance(resp,list):
        for line in resp:
            print(line)
    else:
        print(resp)
        
def loadContextSpec(specfile):
	with open(specfile,'r') as f:
		spec=json.load(f)['context specification']
	return spec
	
def loadCSVParams(specfile):
	with open(specfile,'r') as f:
		spec=json.load(f)['csv params']
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
		att=getattr(row,'eventType')
		if key not in dico.keys():
			dico[key]=[att]
		else:
			if att not in dico[key]:
				dico[key].append(att)
	return dico
	
def convertDict2Cxt(dictionary,cxtfile):
	attributes=list({e for val in dictionary.values() for e in val})
	precontext=dict((k,len(attributes)*['.']) for k in dictionary.keys())
	for k,v in dictionary.items():
		ind=[attributes.index(e) for e in v]
		for i in ind:
			precontext[k][i]='X'
	context_withobj=[(k,''.join(v)) for k,v in precontext.items()]
	objects=[e[0] for e in context_withobj]
	fullcontext=[e[1] for e in context_withobj]
	num_objects=len(objects)
	num_attributes=len(attributes)
	with open(cxtfile,'w') as f:
		f.write('B\n\n')
		f.write(str(num_objects)+'\n')
		f.write(str(num_attributes)+'\n\n')
		for obj in objects:
			if obj=='':
				f.write("''\n")
			else:
				f.write(str(obj)+'\n')
		for att in attributes:
			f.write(str(att)+'\n')
		for line in fullcontext:
			f.write(line+'\n')
			
def convertDict2Fimi(dictionary,fimifile):
	attributes=list({e for val in dictionary.values() for e in val})
	precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
	with open(fimifile,'w') as f:
		for v in precontext:
			f.write(' '.join(str(l) for l in v[1]))
			f.write('\n')
			
def convertCSV2Fimi(csv1,csv2,csv1_key,csv2_key,type_join,object_name,attribute_name,fimifile):
	merged_table=mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join)
	dico=constructDictFromCSVSlice(merged_table,object_name,attribute_name)
	convertDict2Fimi(dico,fimifile)
	
def convertCSV2Cxt(csv1,csv2,csv1_key,csv2_key,type_join,object_name,attribute_name,cxtfile):
	merged_table=mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join)
	dico=constructDictFromCSVSlice(merged_table,object_name,attribute_name)
	convertDict2Fimi(dico,cxtfile)
	
def getFimiFromCSVSpec(specfile):
	spec=loadCSVParams(specfile)
	file1=spec['csv1']
	file2=spec['csv2']
	key1=spec['csv1_key']
	key2=spec['csv2_key']
	join=spec['join']
	object_name=spec['objects']
	attribute_name=spec['attributes']
	fimifile=os.path.splitext(file1)[0]+'_'+os.path.splitext(os.path.basename(file2))[0]+'.fimi'
	convertCSV2Fimi(file1,file2,key1,key2,join,object_name,attribute_name,fimifile)
	return fimifile
		  
        
def getQueryResFromSpecFile(specfile):
	#loads a json specification file (defining a query) and returns the results (in json format) of the query definined in the specification file 
	#as well as the types of objects and attributes
	spec=loadContextSpec(specfile)
	query=spec['query']
	obj_name=spec['objects']
	att_name=spec['attributes']
	port=int(spec['port'])
	query_res=getQuery(query,port)
	return query_res,obj_name,att_name


def convertQueryRes2Dict(json,obj_name,att_name):
	keys=list(set([e[obj_name] for e in json]))
	keys.sort()
	dic=dict((k,[]) for k in keys)
	for e in json:
		dic[e[obj_name]].append(e[att_name])
	return dic
        
def convertQueryRes2Cxt(json,obj_name,att_name,cxtfile):
	#converts query results (json) to cxt file (cxtfile being the path to the cxt file)
	dic=convertQueryRes2Dict(json,obj_name,att_name)
	convertDict2Cxt(dic,cxtfile)
			
def convertQueryRes2Fimi(json,obj_name,att_name,fimifile):
	#converts query results (json) to cxt file (cxtfile being the path to the fimi file)
	dic=convertQueryRes2Dict(json,obj_name,att_name)
	convertDict2Fimi(dic,fimifile)
			
def convertQueryRes(json,obj_name,att_name,filename):
	extension=os.path.splitext(filename)[1]
	if 'cxt' in extension:
		convertQueryRes2Cxt(json,obj_name,att_name,filename)
	elif 'fimi' in extension:
		convertQueryRes2Fimi(json,obj_name,att_name,filename)
	else:
		print('Extension of destination file not supported: '+extension+'instead of .cxt or .fimi')
		
def convertCSVRes(specfile,outputfile):
	extension=os.path.splitext(outputfile)[1]
	spec=loadCSVParams(specfile)
	dico=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'])
	obj_name=spec['objects']
	att_name=spec['attributes']
	if 'cxt' in extension:
		convertDict2Cxt(dico,outputfile)
	elif 'fimi' in extension:
		convertDict2Fimi(dico,outputfile)
	else:
		print('Extension of destination file not supported: '+extension+'instead of .cxt or .fimi')
				
def convertQueryRes2FimiWithEntities(query_output,obj_name,att_name,fimifile,csv_flag=False):
	#converts query results (json/merged_table from csv files) to fimifile (fimifile being the path to the fimi file)
	dic=(convertQueryRes2Dict(query_output,obj_name,att_name) if csv_flag==False else constructDictFromCSVSlice(query_output,obj_name,att_name))
	attributes=list({e for val in dic.values() for e in val})
	precontext=[(k,sorted([attributes.index(e) for e in dic[k]])) for k in dic.keys()]
	objs=[l[0] for l in precontext]
	with open(fimifile,'w') as f:
		for v in precontext:
			f.write(' '.join(str(l) for l in v[1]))
			f.write('\n')
	return {'attributes':attributes,'objects':objs}
			
def getFimiFromQuery(specfile,fimifile,namedentities=True,csv_flag=False):
	if csv_flag==False:
		query_res,obj_name,att_name=getQueryResFromSpecFile(specfile)
	else:
		spec=loadCSVParams(specfile)
		query_res=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],spec['join'])
		obj_name=spec['objects']
		att_name=spec['attributes']
	res=convertQueryRes2FimiWithEntities(query_res,obj_name,att_name,fimifile,csv_flag)
	if namedentities==True:
		return res
	else:
		return 'converted result of query from '+specfile+' to '+fimifile
			
FPGrowth_Params=collections.namedtuple('FPGrowth_Params',['inputFPgrowth','objects','attributes'])

def convertQueryRes2FPgrowthInput(json,obj_name,att_name):
	#converts query results (json) to input format required by pyfpgrowth package (list of lists, in which each row i corresponds to the list 
	# of attributes the object at row i of the context of the matrix has and attributes are identified by their position in the context matrix)
	dic=(convertQueryRes2Dict(query_output,obj_name,att_name) if csv_flag==False else constructDictFromCSVSlice(query_output,obj_name,att_name))
	atts=list({e for val in dic.values() for e in val})
	list_params=[(k,sorted([atts.index(e) for e in dic[k]])) for k in dic.keys()]
	objs=[l[0] for l in list_params]
	inputfp=[l[1] for l in list_params]
	res=FPGrowth_Params(inputFPgrowth=inputfp,objects=objs,attributes=atts)
	return res
	

def convertCxtFileToFimi(cxtfile,fimifile):
	with open(cxtfile,'r') as f:
		cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		offset_obj=5+num_objects
		offset_att=offset_obj+num_attributes
		cxt_context=cxt[offset_att:]
		context=[[i for i in range(len(e)) if e[i].lower()=='x'] for e in cxt_context]
	with open(fimifile,'w') as f:
		for line in context:
			print(' '.join(str(l) for l in line),file=f)	

	

