import os,re,sys,numpy
import collections
import input_processing as ip
import json
import operator
import itertools
import ast

Concept=collections.namedtuple('Concept',['extent','intent','support'])
Edge=collections.namedtuple('Edge',['start_node','end_node']) #both start node and end node should be of type Concept

def printConcept(concept):
	print('Extent: {'+','.join([str(e) for e in concept.extent])+'}  Intent: {'+','.join([str(e) for e in concept.intent])+'}\n')
	
get_indexes = lambda x, xs: [i for (y, i) in zip(xs, range(len(xs))) if x == y]



def detectTypeFile(filepath,type_json='context'): #type_json specifies which type of specification file we pass as a parameter. It can take two values: 'context' (default) when 
	#the specification file gives query/context details or 'csv' when details are given about csv to be used to generate the context
	ext=os.path.splitext(filepath)[1]
	if ext=='.json':
		return (-1 if type_json=='context' else (-2 if type_json=='csv' else 2))
	elif ext=='.cxt':
		return 0
	elif ext=='.fimi':
		return 1
	else:
		return 2
	
			

	
def isSuccessor(concept1,concept2,list_concepts):
	#tests if concept2 is a successor of the current concept given the list of concepts list_concepts (=lattice)
	#concept1 and concept2 are objects of class Concept and list_concepts is an object of class ConceptsList
	c1=set(concept1.intent)
	c2=set(concept2.intent)
	return c2<c1 and {c for c in list_concepts if c2 < set(c.intent) and set(c.intent) < c1} == set()
	
class ConceptsList(): #currently not used. Will be used in later version of code
	def __init__(self):
		self.concepts=[]
		self.edges=[]
		self.named=False
		
	def buildConceptsList(self,concepts):
		#Assumes 'concepts' is a dictionary where keys are the extents and the value associated 
		#to each key corresponds to the intent associated with the extent represented by the key
		#builds a list of objects of class Concept
		for k,v in concepts:
			c=Concept(extent=k,intent=v,support=len(k)/len(concepts))
			self.concepts.append(c)


	def getConceptIntent(self,extent):#extent is a set of strings
		l_ext=len(extent)
		return {e.intent for e in self.concepts if l_ext==len(e.extent) and len(set(e.extent)&extent)==l_ext}
	
	def buildEdges(self):
	#Creates edges between the concepts (represented as objects of class Concept) contained in a list
		list_concepts=self.concepts
		self.edges=[Edge(start_node=c1,end_node=c2) for c1 in list_concepts for c2 in list_concepts if isSuccessor(c1,c2,list_concepts)]
		
	def printConceptList(self):
		list_concepts=self.concepts
		print('Concepts   Total:'+str(len(list_concepts))+'\n--------------------------\n')
		for e in list_concepts:
			printConcept(e)
		
	def printEdges(self):
		list_edges=self.edges
		print('Edges  Total:'+str(len(list_edges))+'\n--------------------------\n')
		for i in range(len(list_edges)):
			print('Edge '+str(i)+'\n')
			print('Start node\n')
			printConcept(list_edges[i].start_node)
			print('End node\n')
			printConcept(list_edges[i].end_node)
			print('*********************************\n')
			

class Context():
	
	def __init__(self,filepath,type_json='context'):
		#print('json type (Context)',type_json)
		self.typefile=detectTypeFile(filepath,type_json) #type of input file (cxt,fimi or json specification file)
		print('Parsing input')
		self.read_file(filepath,type_json) #generation of context object
		# A context always has the following attributes: 
		#- num_objects (i.e number of objects in the context)
	    #- num_attributes (i.e number of attributes in the context)
	    #- context (i.e the actual context matrix (with each row expressed as a string of 0s and 1s e.g '100010' where 1 means that the object represented by the row has the attribute
	    # corresponding to the index/position of 1 (in the example, the object has attributes at positions 0 and 4)))
	    #If the context is derived from a JSON specification file or a CXT file, it also has the following attributes:
	    #- objects (i.e the list of object names)
	    #-attributes (i.e the list of attribute names)
	    
		self.min_support=0 #minimal support. 0 by default, which leads to the generation of all concepts. A scaled value is expected, i.e a value between 0 and 1: for example if a concept
		# should have at least 2 objects in a context containing a total of 8 objects, min_support is set to 2/8 i.e 0.25 
		self.stats={'total':0,'closures':0, 'fail_canon':0, 'fail_fcbo':0, 'fail_support':0} #FCbO-related statistics
	
	def setMinSupport(self,support): #set minimal support
		self.min_support=support
		
	def parseQuery(self,filepath): #generates context object from json query specification file
		spec=ip.loadSpec(filepath,csv_flag=False)
		query=spec['query']
		obj_name=spec['objects']
		att_name=(spec['attributes'].strip() if ',' not in spec['attributes'] else [s.strip() for s in spec['attributes'].split(',')])
		if 'port' in spec.keys():
			port=spec['port']
		else:
			port=8080
		query_res=ast.literal_eval(ip.getQuery(query,port))
		query_res=[ast.literal_eval(v.replace('{ ','{').replace(', ',',').replace(' }','}').translate({ord('='):'":"',ord(','):'","',ord('{'):'{"',ord('}'):'"}'})) for v in query_res]
		dic = collections.defaultdict(list)
		if type(att_name)==str:
			for e in query_res:
				dic[e[obj_name]].append(e[att_name])
		elif type(att_name)==list:
			for e in query_res:
				dic[e[obj_name]].extend([att+'='+str(e[att]) for att in att_name])
		else:
			raise TypeError('The attributes should either be a string or a list of strings')
		self.attributes=list({e for val in dic.values() for e in val})
		self.num_attributes=len(self.attributes)
		precontext=[(k,[self.attributes.index(e) for e in v]) for k,v in dic.items()]
		self.objects,self.context=zip(*[(c[0],''.join(['1' if i in c[1] else '0' for i in range(self.num_attributes)])) for c in precontext])
		self.num_objects=len(self.objects)
		return self
		
	def parseCxtfile(self,cxtfile): #build context object from input cxt file
		with open(cxtfile,'r') as f:
			cxt=f.read()
		r=re.compile('B\s{2}(?P<num_obj>\d*)\s(?P<num_att>\d*)\s{2}(?P<vals>(.*\s*)*)',re.M)
		m=re.match(r,cxt)
		self.num_objects,self.num_attributes,vals=int(m.group('num_obj')),int(m.group('num_att')),m.group('vals')
		vals_split=vals.split('\n',self.num_objects)
		vals_split2=vals_split[-1].split('\n',self.num_attributes)
		self.objects,self.attributes,self.context=vals_split[:-1],vals_split2[:-1],vals_split2[-1].translate({ord("X"):'1',ord("."):'0'}).split()
		return self
	
	def parseFimifile(self,fimifile): #build context object from input fimi file
		with open(fimifile,'r') as f:
			fimi=[list(map(int,s.split())) for s in f.read().splitlines()]
		self.num_objects,self.num_attributes=len(fimi),numpy.max(fimi)-numpy.min(fimi)+1
		self.context=[''.join(['1' if i in e else '0' for i in range(self.num_attributes)]) for e in fimi]
		return self
		
	def parseCSV(self,specfile):
		csv_dictionary=ip.loadSpec(specfile,csv_flag=True)
		file1=csv_dictionary['csv1']
		file2=csv_dictionary['csv2']
		key1=csv_dictionary['csv1_key']
		key2=csv_dictionary['csv2_key']
		join=csv_dictionary['join']
		object_name=csv_dictionary['objects']
		attribute_name=csv_dictionary['attributes']
		dic=ip.constructDictFromCSVFiles(file1,file2,key1,key2,object_name,attribute_name,join)
		self.attributes=list(set(itertools.chain.from_iterable(dic.values())))
		self.num_attributes=len(self.attributes)
		precontext=[(k,[self.attributes.index(e) for e in v]) for k,v in dic.items()]
		self.objects,self.context=zip(*[(c[0],''.join(['1' if i in c[1] else '0' for i in range(self.num_attributes)])) for c in precontext])
		self.num_objects=len(self.objects)
		return self
		
		
	def read_file(self,filepath,type_json='context'): #generates context object depending on input (cxt file, fimi file or json query specification file)
		print('Detecting input type')
		filetype=detectTypeFile(filepath,type_json)
		print('filetype',filetype)
		if filetype==-2:
			self.parseCSV(filepath)
		elif filetype==-1:
			self.parseQuery(filepath)
		elif filetype==0:
			self.parseCxtfile(filepath)
		elif filetype==1:
			self.parseFimifile(filepath)
		else:
			print('Wrong file format. Expecting .cxt or .fimi or context (query)/csv specification file (json format) and not '+os.path.splitext(filepath)[1])
			
	def writeContext2Cxt(self,cxtfile):
		ip.writeCxtFile(self.objects,self.attributes,self.context,cxtfile)
				
	def writeContext2Fimi(self,fimifile):
		fullcontext=[' '.join(str(i) for i in range(len(e)) if e[i]=='1') for e in self.context]
		ip.writeFimiFileV2(fullcontext,fimifile)
		
	def generateTable(self): #returns the context
		return self.context
	
	def generateRows(self,attribute): #returns the objects that have a certain attribute
		t=self.generateTable()
		return (t.index(element) for element in t if element[attribute]=='1')
		
	def getAttributeFromObject(self,obj,namedentities=False): #get the attributes corresponding to an object. namedentities specifies whether the attributes and object are
		#named or identified by their position in the context matrix
		table=self.context
		if namedentities==False:
			attributes=get_indexes('1',table[obj])
		else:
			ind=self.objects.index(obj)
			attributes=operator.itemgetter(*get_indexes('1',table[ind]))(self.attributes)
		attributes=(attributes if type(attributes)==tuple else tuple({attributes}))
		return attributes
		
	#def getAttributeFromObject2
			
	def computeClosure(self,extent,intent,new_attribute): #computes closures
		if self.typefile>1:
			print('Cannot compute closure. File format not recognized and not supported.\n')
		else:
			table=self.context
			rows=(table.index(element) for element in table if element[new_attribute]=='1')
			C=self.num_objects*[0]
			D=self.num_attributes*[1]
			intersect_extent_rows=filter(lambda x:extent[x]==1,rows)
			for e in intersect_extent_rows:
				C[e]=1
				for j in range(self.num_attributes):
					if table[e][j]=='0':
						D[j]=0
			self.stats['closures']+=1
			return C,D
			
	def convertContext2Sets(self,namedentities=False): #concert a context to sets
		c=self.context
		new_context=[]
		for e in c:
			if namedentities==False:
				new_line={i for i in range(len(e)) if e[i]==1}
			else:
				new_line={self.attributes[i] for i in range(len(e)) if e[i]==1}
			new_context.append(new_line)
		return new_context
			
			
	def computeClosureWithSupp(self,extent,intent,new_attribute): #computes closures. Takes into account minimal support conditions
		if self.typefile>1:
			print('Cannot compute closure. File format not recognized and not supported.\n')
		else:
			table=self.context
			rows=(table.index(element) for element in table if element[new_attribute]=='1')
			C=[0 for i in range(self.num_objects)]
			D=[1 for j in range(self.num_attributes)]
			intersect_extent_rows=list(filter(lambda x:extent[x]==1,rows))
			supp=len(intersect_extent_rows)/self.num_objects
			for e in intersect_extent_rows:
				C[e]=1
				for j in range(self.num_attributes):
					if int(table[e][j])==0:
						D[j]=0
			self.stats['closures']+=1
			return C,D,supp
			
	def generateFrom(self,extent,intent,new_attribute): #main function of FCbO. Generates the concepts. Currently is a direct port of the original recursive algorithm
		#For scalability reasons, an iterative version of this function will be included in a future version of the code
		concepts={(tuple(extent),tuple(intent)),}
		if all(intent)!=1 and new_attribute<=self.num_attributes:
			for j in range(new_attribute,self.num_attributes):
				if intent[j]==0:
					C,D=self.computeClosure(extent,intent,j)
					skip=False
					for k in range(j-1):
						if D[k]!=intent[k]:
							skip=True
							self.stats['fail_canon']+=1
							break
					if skip==False:
						concept=self.generateFrom(C,D,j+1)
						concepts.update(concept)
		return concepts
		
	def generateFromWithSupp(self,extent,intent,new_attribute): #main function of FCbO. Generates the concepts whose support is above a certain threshold. Currently is a direct port of the original recursive algorithm
		#For scalability reasons, an iterative version of this function will be included in a future version of the code
		concepts={(tuple(extent),tuple(intent)),}
		if all(intent)!=1 and new_attribute<=self.num_attributes:
			for j in range(new_attribute,self.num_attributes):
				if intent[j]==0:
					C,D,supp=self.computeClosureWithSupp(extent,intent,j)
					skip=False
					for k in range(j-1):
						if D[k]!=intent[k]:
							skip=True
							self.stats['fail_canon']+=1
							break
						if supp<self.min_support:
							skip=True
							self.stats['fail_support']+=1
							break
					if skip==False:
						concept=self.generateFromWithSupp(C,D,j+1)
						concepts.update(concept)
		return concepts
		
	def generateFromWithSupp2(self,extent,intent,new_attribute): #main function of FCbO. Generates the concepts whose support is above a certain threshold. Currently is a direct port of the original recursive algorithm
		#For scalability reasons, an iterative version of this function will be included in a future version of the code
		concepts={(tuple(extent),tuple(intent)),}
		if all(intent)!=1 and new_attribute<=self.num_attributes:
			for j in range(new_attribute,self.num_attributes):
				if intent[j]==0:
					C,D,supp=self.computeClosureWithSupp(extent,intent,j)
					print('C',C)
					print('D',D)
					print('supp',supp)
					skip=False
					if supp<self.min_support:
						skip=True
						self.stats['fail_support']+=1
						break
					for k in range(j-1):
						if D[k]!=intent[k]:
							skip=True
							self.stats['fail_canon']+=1
							break
					if skip==False:
						concept=self.generateFromWithSupp(C,D,j+1)
						existing_concepts=dict((c[0],c[1].count(1)) for c in concepts)
						print(existing_concepts)
						concept={c for c in concept if (c[0] not in existing_concepts.keys() or c[1].count(1)>existing_concepts[c[0]])}
						concepts.update(concept)
		return concepts
		
		
		
	def generateAllIntents(self): #generates all concepts
		start_extent=self.num_objects*[1]
		start_intent=self.num_attributes*[0]
		new_attribute=0
		concepts=self.generateFrom(start_extent,start_intent,new_attribute)
		unfiltered_concepts=list(concepts)
		dic=collections.defaultdict(list)
		for i in range(len(unfiltered_concepts)):
			dic[unfiltered_concepts[i][0]].append((i,unfiltered_concepts[i][1].count(1)))
		concept_filter=[max(v,key=operator.itemgetter(1))[0] for v in dic.values()]
		filtered_concepts=set([unfiltered_concepts[c] for c in concept_filter])
		self.stats['total']=len(filtered_concepts)
		return filtered_concepts
		
	def generateAllIntentsWithSupp(self,support): #generates all concepts whose support is above the 'support' parameter
		self.setMinSupport(support)
		start_extent=self.num_objects*[1]
		start_intent=self.num_attributes*[0]
		new_attribute=0
		concepts=self.generateFromWithSupp(start_extent,start_intent,new_attribute)
		unfiltered_concepts=list(concepts)
		dic=collections.defaultdict(list)
		for i in range(len(unfiltered_concepts)):
			dic[unfiltered_concepts[i][0]].append((i,unfiltered_concepts[i][1].count(1)))
		concept_filter=[max(v,key=operator.itemgetter(1))[0] for v in dic.values()]
		filtered_concepts=set([unfiltered_concepts[c] for c in concept_filter])
		self.stats['total']=len(filtered_concepts)
		return filtered_concepts

		
	#def generateAllIntentsWithSupp2(self,support): #generates all concepts whose support is above the 'support' parameter
		#self.setMinSupport(support)
		#start_extent=self.num_objects*[1]
		#start_intent=self.num_attributes*[0]
		#new_attribute=0
		#concepts=list(self.generateFromWithSupp2(start_extent,start_intent,new_attribute))
		##unfiltered_concepts=[(i,concepts[i][0],concepts[i][1].count(1)) for i in range(len(concepts))]
		##concept_filter=list(max(v,key=operator.itemgetter(2))[0] for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1)))
		##filtered_concepts={concepts[c] for c in concept_filter}
		#self.stats['total']=len(concepts)
		#return concepts
				
	def writeConcepts(self,concepts,outputfile=sys.stdout,namedentities=False):
		if outputfile!=sys.stdout:
			output=open(outputfile,'w+')
		else:
			output=sys.stdout
		header=['Concepts   Total:'+str(len(concepts))+'\nNamed:'+str(namedentities)+'\n--------------------------\n']
		body=['\n'.join(['Extent: {'+','.join([str(e) for e in concept[0]])+'}  Intent: {'+','.join([str(e) for e in concept[1]])+'}' for concept in concepts])]
		fullcontent=''.join(header+body)
		print(fullcontent,file=output)
		if outputfile!=sys.stdout:
			output.close()	
				
							
	def printConcepts(self,concepts,outputfile=sys.stdout,namedentities=False): #prints concepts to screen or to file
		named=namedentities
		transformed_concepts={(ip.tupToIndices(tup[0]),ip.tupToIndices(tup[1])) for tup in concepts}
		if named==True:
			transformed_concepts={(ip.indicesToNames(tup[0],self.objects),ip.indicesToNames(tup[1],self.attributes))for tup in transformed_concepts}
		self.writeConcepts(transformed_concepts,outputfile,namedentities=named)
		
			
					
	def returnConcepts(self,concepts,namedentities=False): #returns concepts as a set of tuples (first element of each tuple=concept extent, second element of each tuple=concept intent)
		transformed_concepts={(ip.tupToIndices(tup[0]),ip.tupToIndices(tup[1])) for tup in concepts}
		if namedentities==True:
			transformed_concepts={(ip.indicesToNames(tup[0],self.objects),ip.indicesToNames(tup[1],self.attributes))for tup in transformed_concepts}
		return transformed_concepts
		
	def generateConceptsList(self,concepts,namedentities=False):
		transformed_concepts=self.returnConcepts(concepts,namedentities)
		list_concepts=ConceptsList()
		list_concepts.buildConceptsList(transformed_concepts)
		list_concepts.buildEdges()
		list_concepts.named=namedentities
		return list_concepts
		
##functions written mainly to make output format of FCbO compatible with input format required by analysis functions written by James. Will mostly change in future version.
	def returnItemsets(self,concepts,namedentities=False):
		transformed_concepts={ip.tupToIndices(tup[1]) for tup in concepts}
		if namedentities==True:
			transformed_concepts={frozenset(ip.indicesToNames(tup,self.attributes)) for tup in transformed_concepts}
		else:
			transformed_concepts={frozenset(c) for c in transformed_concepts}
		return transformed_concepts
	
	def getSupports(self,concepts,namedentities=False):
		transformed_concepts=self.returnConcepts(concepts,namedentities)
		supports={frozenset(v):len(k) for k,v in transformed_concepts}
		return supports		
		
	def getScaledSupports(self,concepts,namedentities=False):
		transformed_concepts=self.returnConcepts(concepts,namedentities)
		supports={frozenset(v):len(k)/self.num_objects for k,v in transformed_concepts}
		return supports
		
	def getSupportsFromItemsets(self,itemsets,namedentities=False):
		supports=collections.defaultdict(int)
		for item in itemsets:
			for row in self.context:
				if namedentities==True:
					indices=[self.attributes.index(e) for e in item]
				else:
					indices=item
				if indices!=[]:
					vals=operator.itemgetter(*indices)(row)
					if all(vals[i]=='1' for i in range(len(vals))):
						support[indices]+=1
				else:
					if all(row[i]=='0' for i in range(len(row))):
						support[indices]+=1
		return supports
		
	def getScaledSupportsFromItemsets(self,itemsets,namedentities=False):
		supports=self.getSupportsFromItemsets(itemsets,namedentities)
		supports=dict((k,v/self.num_objects) for k,v in supports.items())
		return supports
	    
		

		
def getSupportsFromTransConcepts(transformed_concepts):
	supports={frozenset(v):len(k) for k,v in transformed_concepts}
	return supports
		
def getScaledSupportsFromTransConcepts(transformed_concepts,num_objects):
	supports={frozenset(v):len(k)/num_objects for k,v in transformed_concepts}
	return supports
			
		
		

		


		
		
		
	
		
		
	
		
					
					

	
	
				
				
		
		
						
			
				
			
			
	
	
	
	
		




