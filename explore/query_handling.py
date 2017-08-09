#! /usr/bin/env python3

import requests, readline, sys, json

def getQuery(query):
	#gets query result as json
    try:
        resp = requests.post("http://localhost:8080/query/generic", data={"query": query},timeout=120)
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
        
def convertQueryRes2Cxt(json,obj_name,att_name,cxtfile):
	#converts query results (json) to cxt file (cxtfile being the path to the cxt file)
	keys=list(set([e[obj_name] for e in json]))
	dic=dict((k,[]) for k in keys)
	for e in json:
		dic[e[obj_name]].append(e[att_name])
	attributes=list({e for val in dic.values() for e in val})
	precontext=dict((k,len(attributes)*['.']) for k in dic.keys())
	for k,v in dic.items():
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
			
def convertQueryRes2Fimi(json,obj_name,att_name,fimifile):
	#converts query results (json) to cxt file (cxtfile being the path to the fimi file)
	keys=list(set([e[obj_name] for e in json]))
	dic=dict((k,[]) for k in keys)
	for e in json:
		dic[e[obj_name]].append(e[att_name])
	attributes=list({e for val in dic.values() for e in val})
	precontext=dict((k,[attributes.index(e) for e in dic[k]]) for k in dic.keys())
	with open(fimifile,'w') as f:
		for v in precontext.values():
			f.write(' '.join(str(l) for l in v))
			f.write('\n')
