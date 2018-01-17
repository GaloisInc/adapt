import os,re,sys,numpy
import collections
from operator import itemgetter
import query_handling as q
import json

Concept=collections.namedtuple('Concept',['extent','intent','support'])
Edge=collections.namedtuple('Edge',['start_node','end_node']) #both start node and end node should be of type Concept

def printConcept(concept):
	print('Extent: {'+','.join([str(e) for e in concept.extent])+'}  Intent: {'+','.join([str(e) for e in concept.intent])+'}\n')

def loadContextSpec(specfile):
	with open(specfile,'r') as f:
		spec=json.load(f)['context specification']
	return spec

def detectTypeFile(filepath):
	ext=os.path.splitext(filepath)[1]
	if ext=='.json' and 'spec' in filepath:
		return -1
	elif ext=='.cxt':
		return 0
	elif ext=='.fimi':
		return 1
	else:
		return 2
			
def tupToIndices(tup):
	return tuple(i for i in range(len(tup)) if tup[i]==1)
	
def indicesToNames(indices_tup,names_list):
	return tuple(names_list[e] for e in indices_tup)
	
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
	
	def __init__(self,filepath):
		self.typefile=detectTypeFile(filepath) #type of input file (cxt,fimi or json specification file)
		self.read_file(filepath) #generation of context object
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
		spec=loadContextSpec(filepath)
		query=spec['query']
		obj_name=spec['objects']
		att_name=spec['attributes']
		query_res=q.getQuery(query)
		keys=list(set([e[obj_name] for e in query_res]))
		dic=dict((k,[]) for k in keys)
		for e in query_res:
			dic[e[obj_name]].append(e[att_name])
		self.attributes=list({e for val in dic.values() for e in val})
		precontext=dict((k,len(self.attributes)*['0']) for k in dic.keys())
		for k,v in dic.items():
			ind=[self.attributes.index(e) for e in v]
			for i in ind:
				precontext[k][i]='1'
			context_withobj=[(k,''.join(v)) for k,v in precontext.items()]
			self.objects=[e[0] for e in context_withobj]
			self.context=[e[1] for e in context_withobj]
			self.num_objects=len(self.objects)
			self.num_attributes=len(self.attributes)
		return self
		
	def parseCxtfile(self,cxtfile): #build context object from input cxt file
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
			self.num_objects=int(cxt[2])
			self.num_attributes=int(cxt[3])
			offset_obj=5+self.num_objects
			offset_att=offset_obj+self.num_attributes
			self.objects=cxt[5:offset_obj]
			self.attributes=cxt[offset_obj:offset_att]
			self.context=[re.sub('X','1',re.sub('\.','0',c)) for c in cxt[offset_att:]]
		return self
	
	def parseFimifile(self,fimifile): #build context object from input fimi file
		with open(fimifile,'r') as f:
			fimi=f.read().splitlines()
			self.num_objects=len(fimi)
			fimi=[list(map(int,re.split('\s',f))) for f in fimi]
			att_max=max(map(max,fimi))
			att_min=min(map(min,fimi))
			self.num_attributes=att_max-att_min+1
			self.context=[''.join(['1' if i in e else '0' for i in range(self.num_attributes)]) for e in fimi]
		return self
		
		
	def read_file(self,filepath): #generates context object depending on input (cxt file, fimi file or json query specification file)
		filetype=detectTypeFile(filepath)
		print('filetype',filetype)
		if filetype==-1:
			self.parseQuery(filepath)
		elif filetype==0:
			self.parseCxtfile(filepath)
		elif filetype==1:
			self.parseFimifile(filepath)
		else:
			print('wrong file format. expecting .cxt or .fimi or context (query) specification file (json format) and not '+os.path.splitext(filepath)[1])
		
	def generateTable(self): #returns the context
		return self.context
	
	def generateRows(self,attribute): #returns the objects that have a certain attribute
		t=self.generateTable()
		return (t.index(element) for element in t if element[attribute]=='1')
		
	def getAttributeFromObject(self,obj,namedentities=False): #get the attributes corresponding to an object. namedentities specifies whether the attributes and object are
		#named or identified by their position in the context matrix
		table=self.context
		if namedentities==False:
			attributes={element for element in range(len(table[obj])) if table[obj][element]=='1'}
		else:
			ind=self.objects.index(obj)
			attributes={self.attributes[element] for element in range(len(table[ind])) if table[ind][element]=='1'}
		return attributes
	
	def computeClosure(self,extent,intent,new_attribute): #computes closures
		if self.typefile>1:
			print('cannot compute closure. File format not recognized and not handled.\n')
		else:
			table=self.generateTable()
			rows=self.generateRows(new_attribute)
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
			print('cannot compute closure. File format not recognized and not handled.\n')
		else:
			table=self.generateTable()
			rows=self.generateRows(new_attribute)
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
			#concepts={}
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
		
		
	def generateAllIntents(self): #generates all concepts
		start_extent=self.num_objects*[1]
		start_intent=self.num_attributes*[0]
		new_attribute=0
		concepts=self.generateFrom(start_extent,start_intent,new_attribute)
		unfiltered_concepts=list(concepts)
		dic=dict((c[0],[]) for c in unfiltered_concepts)
		for i in range(len(unfiltered_concepts)):
			dic[unfiltered_concepts[i][0]].append((i,unfiltered_concepts[i][1].count(1)))
		concept_filter=[max(v,key=itemgetter(1))[0] for v in dic.values()]
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
		dic=dict((c[0],[]) for c in unfiltered_concepts)
		for i in range(len(unfiltered_concepts)):
			dic[unfiltered_concepts[i][0]].append((i,unfiltered_concepts[i][1].count(1)))
		concept_filter=[max(v,key=itemgetter(1))[0] for v in dic.values()]
		filtered_concepts=set([unfiltered_concepts[c] for c in concept_filter])
		self.stats['total']=len(filtered_concepts)
		return filtered_concepts
				
		
	def printConcepts(self,concepts,output=sys.stdout,namedentities=False): #prints concepts to screen or to file
		#transform concepts according to file type 
		transformed_concepts={(tupToIndices(tup[0]),tupToIndices(tup[1])) for tup in concepts}
		if namedentities==True:
			transformed_concepts={(indicesToNames(tup[0],self.objects),indicesToNames(tup[1],self.attributes))for tup in transformed_concepts}
		if output==sys.stdout:
			print('Concepts   Total:'+str(len(transformed_concepts))+'\n--------------------------\n')
			for concept in transformed_concepts:
				print('Extent: {'+','.join([str(e) for e in concept[0]])+'}  Intent: {'+','.join([str(e) for e in concept[1]])+'}\n')
		else:
			with open(output,'w') as f:
				f.write('Concepts\n--------------------------\n')
				for concept in transformed_concepts:
					f.write('Extent: {'+','.join([str(e) for e in concept[0]])+'}  Intent: {'+','.join([str(e) for e in concept[1]])+'}\n')
					
					
	def returnConcepts(self,concepts,namedentities=False): #returns concepts as a set of tuples (first element of each tuple=concept extent, second element of each tuple=concept intent)
		transformed_concepts={(tupToIndices(tup[0]),tupToIndices(tup[1])) for tup in concepts}
		if namedentities==True:
			transformed_concepts={(indicesToNames(tup[0],self.objects),indicesToNames(tup[1],self.attributes))for tup in transformed_concepts}
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
		transformed_concepts={tupToIndices(tup[1]) for tup in concepts}
		if namedentities==True:
			transformed_concepts={frozenset(indicesToNames(tup,self.attributes)) for tup in transformed_concepts}
		else:
			transformed_concepts={frozenset(c) for c in transformed_concepts}
		return transformed_concepts
		
	def getSupports(self,concepts,namedentities=False):
		transformed_concepts=self.returnConcepts(concepts,namedentities)
		supports={frozenset(v):len(k) for k,v in transformed_concepts}
		return supports
		
	def getSupportsFromTransConcepts(self,transformed_concepts):
		supports={frozenset(v):len(k) for k,v in transformed_concepts}
		return supports
		
	def getScaledSupports(self,concepts,namedentities=False):
		transformed_concepts=self.returnConcepts(concepts,namedentities)
		supports={frozenset(v):len(k)/len(concepts) for k,v in transformed_concepts}
		return supports
		
	def getScaledSupportsFromTransConcepts(self,transformed_concepts):
		supports={frozenset(v):len(k)/len(transformed_concepts) for k,v in transformed_concepts}
		return supports
			
		
		

		


		
		
		
	
		
		
	
		
					
					

	
	
				
				
		
		
						
			
				
			
			
	
	
	
	
		




