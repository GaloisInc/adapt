import datetime.timedelta as t
import datetime.datetime.timedelta as t
import datetime
datetime.timedelta(seconds=35.284)
t1=datetime.timedelta(seconds=35.284)
t1=datetime.timedelta(seconds=35.430)
t1=datetime.timedelta(seconds=35.284)
t2=datetime.timedelta(seconds=35.430)
t3=datetime.timedelta(seconds=35.782)
mean([t1,t2,t3])
import numpy
numpy.mean([t1,t2,t3])
numpy.mean([t1.total_seconds(),t2.total_seconds(),t3.total_seconds()])
numpy.std([t1.total_seconds(),t2.total_seconds(),t3.total_seconds()])
t1=datetime.timedelta(minutes=4,seconds=4.202)
t2=datetime.timedelta(minutes=4,seconds=54.316)
t3=datetime.timedelta(minutes=4,seconds=59.431)
numpy.std([t1.total_seconds(),t2.total_seconds(),t3.total_seconds()])
numpy.mean([t1,t2,t3])
str(numpy.mean([t1,t2,t3]))
t1=datetime.timedelta(seconds=11.211)
t2=datetime.timedelta(seconds=9.120)
t3=datetime.timedelta(seconds=9.177)
numpy.mean([t1,t2,t3])
numpy.std([t1.total_seconds(),t2.total_seconds(),t3.total_seconds()])
exit()
t=('pid','Integer','SINGLE')
len(t)
exit()
1000 001 >> 2
1000 0011 >> 2
10000011 >> 2
5 >> 6
4 >> 6
6200 >> 6
14 >> 6
20 >> 6
100 >> 6
2^7
import math
2.pow(7)
2.power(7)
30 >> 6
7000 >> 6
2000 >> 6
y=2000
~5
~1000
13/64+1
import numpy
cxt=open('~/Documents/shared/In-Close2-8/tealady.cxt','r')
import os
cxt=open('home/gberrada/Documents/shared/In-Close2-8/tealady.cxt','r')
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/tealady.cxt','r')
tea=cxt.readlines()
cxr.close()
cxt.close()
tea
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt','r')
lattice=cxt.readlines()
cxt.close()
lattice
lattice.pop()
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt','r')
lattice=cxt.readlines()
cxt.close()
lattice.pop(0)
lattice
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt','r')
lattice=cxt.readlines()[0:-1]
cxt.close()
lattice
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt','r')
lattice=cxt.readlines()[0:-2]
cxt.close()
lattice
cxt=open('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt','r')
lattice=cxt.read().splitlines()
cxt.close()
lattice
int(lattice[2])
num_att=int(lattice[3])
5+14
lattice[19]
lattice[5]
num_objects=int(lattice[2])
objects=lattice[5:5+num_objects]
objects
lattice[5+num_objects:5+num_objects+num_att]
num_att=int(lattice[3])
lattice[5+num_objects:5+num_objects+num_att]
lattice[5+num_objects+num_att:-1]
lattice[5+num_objects+num_att:len(lattice)]
from distutils.util import strtobool
l=lattice[5+num_objects+num_att:len(lattice)]
strtobool(l[0])
def cxtToBool(c):
  if (c=='x'):
     return True
  else:
     return False
map(cxtToBool,l[0])
list(map(cxtToBool,l[0]))
def cxtToBool(c):
  if (c=='x') or (c=='X'):
     return True
  else:
     return True
def cxtToBool(c):
  if (c=='x') or (c=='X'):
     return True
  else:
     return False
list(map(cxtToBool,l[0]))
strtobool('100100110')
numpy.array([map(cxtToBool,e) for e in l],dtype='bool')
numpy.ndarray([map(cxtToBool,e) for e in l],dtype='bool')
numpy.array([list(map(cxtToBool,e)) for e in l],dtype='bool')
numpy.array([[cxtToBool(i) for i in e] for e in l],dtype='bool')
n=numpy.array([[cxtToBool(i) for i in e] for e in l],dtype='bool')
type(n)
n.nbytes
18*14
n.ndim
l
filter(lambda x: x.lower()=='x',l[0])
list(filter(lambda x: x.lower()=='x',l[0]))
l
'b'.index('abc')
'abc'.index('b')
enumerate(l)
list(enumerate(l))
list(enumerate(l[0]))
[e[0] for e in enumerate(l[0]) if e[1].lower()=='x']
[e[0] for e in enumerate(l[1]) if e[1].lower()=='x']
[[e[0] for e in enumerate(l[i]) if e[1].lower()=='x'] for i in range(len(l))]
[(e[0],i) for e in enumerate(l[i]) if e[1].lower()=='x' for i in range(len(l))]
[(e[0],i) for i in range(len(l)) for e in enumerate(l[i]) if e[1].lower()=='x']
ind=[(e[0],i) for i in range(len(l)) for e in enumerate(l[i]) if e[1].lower()=='x']
lattice[5+num_objects+num_att:]
len(lattice[5+num_objects+num_att:])
len(lattice[5+num_objects+num_att:][0])
num_objects
num_att
n.nbytes
n
objects
def cxtToBool(c):
	if (c.lower()=='x') :
		return True
	else:
		return False
0==True
0==False
1==True
def cxtToInt(c):
	if (c.lower()=='x') :
		return 1
	else:
		return 0
context=numpy.array([[cxtToInt(i) for i in e] for e in l])
context.nbytes
context
~'10001'
~10001
n
n[0:4]
n
import /media/gberrada/EEF3-F458/inclose2.py
import "/media/gberrada/EEF3-F458/inclose2.py"
import os
import numpy
import bitarray
max_concepts=25000000
max_cols=7000
max_rows=1000000
max_for_intents=40000000
max_for_extents=1000000000
highc=1
minIn=0
minEx=0
startCol=0
numcons=0
#def hamSort():
	
def cxtToBool(c):
	if (c.lower()=='x') :
		return True
	else:
		return False
		
def cxtToInt(c):
	if (c.lower()=='x') :
		return 1
	else:
		return 0
class Context():
	def __init__(self,cxtfile):
		return self.cxtToContext(self.cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		self.objects=cxt[5:5+num_objects]
		self.attributes=cxt[5+num_objects:5+num_objects+num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c=Context()
c=Context('file:///home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt ')
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt ')
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
class Context():
	def __init__(self,cxtfile):
		return self.cxtToContext(self,cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		self.objects=cxt[5:5+num_objects]
		self.attributes=cxt[5+num_objects:5+num_objects+num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
class Context():
	def __init__(self,cxtfile):
		return cxtToContext(self,cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		self.objects=cxt[5:5+num_objects]
		self.attributes=cxt[5+num_objects:5+num_objects+num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
class Context():
	def __init__(self,cxtfile):
		return self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		self.objects=cxt[5:5+num_objects]
		self.attributes=cxt[5+num_objects:5+num_objects+num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		num_objects=int(cxt[2])
		num_attributes=int(cxt[3])
		self.objects=cxt[5:5+num_objects]
		self.attributes=cxt[5+num_objects:5+num_objects+num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
c
c.objects
c.attributes
c.context
c.context.nbytes
c_mush=Context('/home/gberrada/Documents/shared/In-Close2-8/mushroomep.cxt')
c_mush.context.nbytes
c_mush.context.objects
c_mush.objects
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+num_objects+num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c_mush=Context('/home/gberrada/Documents/shared/In-Close2-8/mushroomep.cxt')
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
c_mush=Context('/home/gberrada/Documents/shared/In-Close2-8/mushroomep.cxt')
c_mush.num_objects
c_mush.num_attributes
c_mush.num_attributes*c_mush.num_objects
c_mush.context.nbytes
c_mush.objects
c_mush.attributes
c_water=Context('/home/gberrada/Documents/shared/In-Close2-8/liveinwater.cxt')
c_water.objects
c_water.attributes
c_water.attributes.index('can move')
c_water.attributes.index('move')
listAtt=['can move','42','has limbs','MONOCOTYLEDON','nonsense']
indAtt=[c_water.attributes.index(e) for e in listAtt if e.lower() in c_water.attributes]
indAtt=[c_water.attributes.index(e.lower()) for e in listAtt if e.lower() in c_water.attributes]
indAtt
indAtt=[c_water.attributes.index(e.lower()) for e in listAtt if e.lower() in c_water.attributes].sort()
indAtt
indAtt=[c_water.attributes.index(e.lower()) for e in listAtt if e.lower() in c_water.attributes]
indAtt.sort()
indAtt
indAtt[0]
c_water.context
b=c_water.context
b[indAtt]
b'[indAtt]
b[:][indAtt]
b[0:-1][indAtt]
b[0:-1][5:7]
b[0:-1][5:8]
b
b[:,5:8]
b[:,5:8].shape()
b[:,5:8].shape
b[:,5:8].shape[1]
b[:,indAtt]
b[:,indAtt].where(all(x==True))
numpy.where([True,True,True],b[:,indAtt])
slice=b[:,indAtt]
slice[0]
all(slice[0])
indObjs=[i for i in range(slice.shape[0]) if all(slice[i])]
indObjs
slice2=b[:,[6,7]]
indObjs2=[i for i in range(slice2.shape[0]) if all(slice2[i])]
indObjs2
c-water.objects[indObjs]
c_water.objects[indObjs]
b
o=[0,4]
c-water.objects
c_water.objects
listObj
listObj=['bream','frog']
indObj=[c_water.objects.index(e.lower()) for e in listObj if e.lower() in c_water.objects]
indObj
sliceContext=self.context(indObj,:)
sliceContext=c_water.context(indObj,:)
sliceContext=c_water.context(indObj)
sliceContext=c_water.context[indObj,:]
sliceContext
b
[i for i in range(sliceContext.shape[1]) if all(sliceContext[i])]
sliceContext.shape
sliceContext.shape[0]
sliceContext.shape[1]
sliceContext[0]
[i for i in range(sliceContext.shape[1]) if all(sliceContext[:,i])]
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
		
	def getObjectsFromAttributes(self,listAtt):
		indAtt=[self.attributes.index(e.lower()) for e in listAtt if e.lower() in self.attributes]
		sliceContext=self.context[:,indAtt]
		indObjs=[i for i in range(sliceContext.shape[0]) if all(sliceContext[i])]
		return [self.objects[e] for e in indObjs]
		
	def getAttributesFromObjects(self,listObj):
		indObj=[self.objects.index(e.lower()) for e in listObj if e.lower() in self.objects]
		sliceContext=self.context[indObj,:]
		indAtts=[i for i in range(sliceContext.shape[1]) if all(sliceContext[:,i])]
		return [self.attributes[e] for e in indAtts]
c_lattice=Context('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt')
c_lattice.context
c_lattice.objects
c_lattice.attributes
c_lattice.getObjectsFromAttributes(['dually semimodular','GRADED','chaos'])
c_lattice.getAttributesFromObjects(['IV','1'])
[i for i in range(sliceContext.shape[1]) if all(sliceContext[:,i]==True)]
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
		
	def getObjectsFromAttributes(self,listAtt):
		indAtt=[self.attributes.index(e.lower()) for e in listAtt if e.lower() in self.attributes]
		sliceContext=self.context[:,indAtt]
		indObjs=[i for i in range(sliceContext.shape[0]) if all(sliceContext[i])]
		return [self.objects[e] for e in indObjs]
		
	def getAttributesFromObjects(self,listObj):
		indObj=[self.objects.index(e.lower()) for e in listObj if e.lower() in self.objects]
		sliceContext=self.context[indObj,:]
		indAtts=[i for i in range(sliceContext.shape[1]) if (all(sliceContext[:,i])==True)]
		return [self.attributes[e] for e in indAtts]
c_lattice=Context('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt')
c_lattice.getAttributesFromObjects(['IV','1'])
c_lattice.context
c=c_lattice.context
c_lattice.objects
c_lattice.objects['IV','1']
c_lattice.objects[3:5]
c_lattice.context[3:5]
c_s=c_lattice.context[3:5]
[i for i in range(c_s.shape[1]) if (any(c_s[:,i])==True and all(c_s[:,i]))]
[i for i in range(c_s.shape[1]) if (any(c_s[:,i])==True)]
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
		
	def getObjectsFromAttributes(self,listAtt):
		indAtt=[self.attributes.index(e.lower()) for e in listAtt if e.lower() in self.attributes]
		sliceContext=self.context[:,indAtt]
		indObjs=[i for i in range(sliceContext.shape[0]) if all(sliceContext[i])]
		return [self.objects[e] for e in indObjs]
		
	def getAttributesFromObjects(self,listObj):
		indObj=[self.objects.index(e.lower()) for e in listObj if e.lower() in self.objects]
		sliceContext=self.context[indObj,:]
		indAtts=[i for i in range(sliceContext.shape[1]) if (any(sliceContext[:,i])==True and all(sliceContext[:,i]))]
		return [self.attributes[e] for e in indAtts]
c_lattice=Context('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt')
c_lattice.getAttributesFromObjects('IV','1')
c_lattice.getAttributesFromObjects(['IV','1'])
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
		
	def getObjectsFromAttributes(self,listAtt):
		indAtt=[self.attributes.index(e.lower()) for e in listAtt if e.lower() in self.attributes]
		sliceContext=self.context[:,indAtt]
		indObjs=[i for i in range(sliceContext.shape[0]) if all(sliceContext[i])]
		return [self.objects[e] for e in indObjs]
		
	def getAttributesFromObjects(self,listObj):
		indObj=[self.objects.index(e.lower()) for e in listObj if e.lower() in self.objects]
		sliceContext=self.context[indObj,:]
		indAtts=[i for i in range(sliceContext.shape[1]) if (any(sliceContext[:,i])==True and all(sliceContext[:,i]))]
		return [self.attributes[e] for e in indAtts]
c_lattice.getAttributesFromObjects(['IV','1'])
c_lattice=Context('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt')
c_lattice.getAttributesFromObjects(['IV','1'])
c_lattice.objects
c_lattice.objects.index('IV')
c_lattice.objects.index('1')
c_lattice.context[3]
c_lattice.context[4]
c_lattice.context[[3,4]]
c_s=c_lattice.context[[3,4]]
c_s[:,3]
c_s[:,0]
all(c_s[:,0]==True)
all(c_s[:,3]==True)
all(c_s[:,8]==True)
[i for i in range(sliceContext.shape[1]) if all(sliceContext[:,i]==True)]
[i for i in range(c_s.shape[1]) if all(c_s[:,i]==True)]
class Context():
	def __init__(self,cxtfile):
		self.cxtToContext(cxtfile)
		
	def cxtToContext(self,cxtfile):
		with open(cxtfile,'r') as f:
			cxt=f.read().splitlines()
		self.num_objects=int(cxt[2])
		self.num_attributes=int(cxt[3])
		self.objects=cxt[5:5+self.num_objects]
		self.attributes=cxt[5+self.num_objects:5+self.num_objects+self.num_attributes]
		context=cxt[5+self.num_objects+self.num_attributes:]
		self.context=numpy.array([[cxtToBool(i) for i in e] for e in context],dtype='bool')
		return self
		
	def getObjectsFromAttributes(self,listAtt):
		indAtt=[self.attributes.index(e.lower()) for e in listAtt if e.lower() in self.attributes]
		sliceContext=self.context[:,indAtt]
		indObjs=[i for i in range(sliceContext.shape[0]) if all(sliceContext[i])]
		return [self.objects[e] for e in indObjs]
		
	def getAttributesFromObjects(self,listObj):
		indObj=[self.objects.index(e.lower()) for e in listObj if e.lower() in self.objects]
		sliceContext=self.context[indObj,:]
		indAtts=[i for i in range(sliceContext.shape[1]) if (all(sliceContext[:,i]==True))]
		return [self.attributes[e] for e in indAtts]
c_lattice=Context('/home/gberrada/Documents/shared/In-Close2-8/lattice.cxt')
c_lattice.getAttributesFromObjects(['IV','1'])
c_lattice.getAttributesFromObjects(['6','7'])
indObj=[c_lattice.objects.index(e.lower()) for e in listObj if e.lower() in c_lattice.objects]
indobj
indObj
listObj=['IV','1']
indObj=[c_lattice.objects.index(e.lower()) for e in listObj if e.lower() in c_lattice.objects]
indObj
'1'.lower()
'1'.lower() in c_lattice.objects
exit
import pandas
import dask.dataframe as dd
def mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join):
	table1=pandas.read_csv(csv1,low_memory=False)
	table2=pandas.read_csv(csv2,low_memory=False)
	merged_table=pandas.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
	return merged_table
def loadCSVParams(specfile):
	with open(specfile,'r') as f:
		spec=json.load(f)['csv params']
	return spec
def mergeCSVdask(csv1,csv2,csv1_key,csv2_key,type_join):
	table1=dd.read_csv(csv1,low_memory=False)
	table2=dd.read_csv(csv2,low_memory=False)
	merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
	return merged_table
import os
os.chdir('csv/theia')
os.getcwd()
spec=loadCSVParams('csvspec.json')
import json
spec=loadCSVParams('../csvspec.json')
spec
csv1=spec['csv1']
csv2=spec['csv2']
csv1_key=spec['csv1_key']
csv2_key=spec['csv2_key']
type_join=spec['join']
t1=mergeCSV(csv1,csv2,csv1_key,csv2_key,type_join)
t2=mergeCSVdask(csv1,csv2,csv1_key,csv2_key,type_join)
t1==t2
t1
t2
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
d1=constructDictFromCSVSlice(t1,spec['objects'],spec['attributes'])
d2=constructDictFromCSVSlice(t2,spec['objects'],spec['attributes'])
d1==d2
exit
exit()
import itertools,os,glob
Directory='/home/gberrada/csv'
	specfile=[os.path.join(Directory,'csvspec.json')]
	pythonscript_path='/home/gberrada/ultra_fca_script2.py' 
	fcbo_path='/home/gberrada/Downloads/fcbo-ins/fcbo'
	min_supp=[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9]
	min_conf=[0.99,0.95,0.9,0.85,0.8,0.75,0.7,0.65,0.6,0.5]
	directories=glob.glob(Directory+'/*/')
	params=sorted(list(itertools.product(directories,min_supp,min_conf,specfile)),key=itemgetter(1),reverse=True)
specfile=[os.path.join(Directory,'csvspec.json')]
fcbo_path='/home/gberrada/Downloads/fcbo-ins/fcbo'
pythonscript_path='/home/gberrada/ultra_fca_script2.py' 
min_supp=[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9]
directories=glob.glob(Directory+'/*/')
min_conf=[0.99,0.95,0.9,0.85,0.8,0.75,0.7,0.65,0.6,0.5]
params=sorted(list(itertools.product(directories,min_supp,min_conf,specfile)),key=itemgetter(1),reverse=True)
from operator import itemgetter
params=sorted(list(itertools.product(directories,min_supp,min_conf,specfile)),key=itemgetter(1),reverse=True)
print('params',params)
len(directories)
len(min_conf)
len(min_supp)
4*10*9
len(params)
exit()
cxtfile='Documents/datasets/nursery/nursery.cxt'
import fcbo_core as fcbo
fca_context=fcbo.Context('Documents/datasets/nursery/nursery.cxt','context')
def parseCxtfilev2(cxtfile):
	with open(cxtfile,'r') as f:
		cxt=f.read()
	r=re.compile('B\s{2}(?P<num_obj>\d*)\s(?P<num_att>\d*)\s{2}(?P<vals>(.*\s*)*)',re.M)
	m=re.match(r,cxt)
	num_objects,num_attributes,vals=int(m.group('num_obj')),int(m.group('num_att')),m.group('vals')
	vals_split=vals.split('\n',num_objects)
	vals_split2=vals_split[-1].split('\n',num_attributes)
	objects,attributes,context=vals_split[:-1],vals_split2[:-1],vals_split2[-1].translate({ord("X"):'1',ord("."):'0'}).split()
	return objects,attributes,context
objects,attributes,context=parseCxtfilev2(cxtfile)
import re
objects,attributes,context=parseCxtfilev2(cxtfile)
print(timeit.timeit("import os",number=10,globals=globals()))
import timeit
print(timeit.timeit("import os",number=10,globals=globals()))
print(timeit.timeit("import pathlib",number=10,globals=globals()))
pathlib.Path(cxtfile).suffix
import os,pathlib
pathlib.Path(cxtfile).suffix
pathlib.Path(cxtfile).suffix[1:]
def writeCxtFile(objects,attributes,context,cxtfile):
	objs=["''\n" if obj=='' else str(obj)+'\n' for obj in objects]
	fullcontext=['\n'.join(context).translate({ord("1"):'X',ord("0"):'.'})]
	atts=['\n'.join([str(att) for att in attributes])]
	preamble=['\n'.join(['B\n',str(len(objects)),str(len(attributes)),'\n'])]
	filecontent=''.join(preamble+objs+atts+fullcontext)
	with open(cxtfile,'w') as f:
		f.write(filecontent)
def writeFimiFile(context,fimifile):
	filecontent='\n'.join([' '.join([str(i) for i in v[1]]) for v in context])
	with open(fimifile,'w') as f:
		f.write(filecontent)
def writeFimiFileV2(context,fimifile):
	filecontent='\n'.join([' '.join([str(i) for i in v]) for v in context])
	with open(fimifile,'w') as f:
		f.write(filecontent)
def loadSpec(specfile,csv_flag=False):
	key=('csv params' if csv_flag==True else 'context specification')
	with open(specfile,'r') as f:
		spec=json.load(f)[key]
	return spec
	
import dask.dataframe as dd
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
directory='csv/theia'
csv_specfile='csv/csvspec.json'
os.chdir(directory)
os.getcwd()
csv_specfile='~/csv/csvspec.json'
spec=loadSpec(csv_specfile,csv_flag=True)
csv_specfile='/home/gberrade/csv/csvspec.json'
csv_specfile='/home/gberrada/csv/csvspec.json'
spec=loadSpec(csv_specfile,csv_flag=True)
import json
spec=loadSpec(csv_specfile,csv_flag=True)
spec
merged_table=mergeCSV(spec['csv1'],spec['csv2'],spec['csv1_key'],spec['csv2_key'],'inner')
dic=constructDictFromCSVSlice(merged_table,spec['objects'],spec['attributes'])
dic
def convertDict2File(dictionary,filename,fimi_flag=False):
	attributes=list({e for val in dictionary.values() for e in val})
	if fimi_flag:
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(num_attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
def convertDict2Filev2(dictionary,filename):
	extension=os.path.splitext(filename)[1][1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(num_attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
def convertDict2Filev3(dictionary,filename):
	extension=pathlib.Path(filename).suffix[1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(num_attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
def convertDict2File(dictionary,filename,fimi_flag=False):
	attributes=list({e for val in dictionary.values() for e in val})
	if fimi_flag:
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
def convertDict2Filev2(dictionary,filename):
	extension=os.path.splitext(filename)[1][1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
def convertDict2Filev3(dictionary,filename):
	extension=pathlib.Path(filename).suffix[1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,cxtfile)
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
dic
def convertDict2File(dictionary,filename,fimi_flag=False):
	attributes=list({e for val in dictionary.values() for e in val})
	if fimi_flag:
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,filename)
def convertDict2Filev2(dictionary,filename):
	extension=os.path.splitext(filename)[1][1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,filename)
def convertDict2Filev3(dictionary,filename):
	extension=pathlib.Path(filename).suffix[1:]
	attributes=list({e for val in dictionary.values() for e in val})
	if extension=='fimi':
		precontext=[(k,sorted([attributes.index(e) for e in dictionary[k]])) for k in dictionary.keys()]
		writeFimiFile(precontext,filename)
	else:
		precontext=[(k,[attributes.index(e) for e in v]) for k,v in dic.items()]
		objects,fullcontext=zip(*[(c[0],''.join(['X' if i in c[1] else '.' for i in range(len(attributes))])) for c in precontext])
		writeCxtFile(objects,attributes,fullcontext,filename)
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=10,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=100,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=100,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.fimi',fimi_flag=True)",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.fimi')",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.fimi')",number=100,globals=globals()))
print(timeit.timeit("convertDict2File(dic,'testconversion.cxt',fimi_flag=False)",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev2(dic,'testconversion2.cxt')",number=100,globals=globals()))
print(timeit.timeit("convertDict2Filev3(dic,'testconversion3.cxt')",number=100,globals=globals()))
os.stat(cxtfile).st_size
os.getcwd(~)
os.chdir('~')
os.chdir('/home/gberrada')
os.getcwd()
os.stat(cxtfile).st_size
with open(cxtfile) as f:
  cxttest=f.read()
print(timeit.timeit("cxttest.count('\n')",number=100,globals=globals()))
print(timeit.timeit("cxttest.count(r'\n')",number=100,globals=globals()))
print(timeit.timeit("cxttest.count('\\n')",number=100,globals=globals()))
max(cxttest,key=int)
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
cxtfile
fimifile=os.splitext(cxtfile)[0]+'.fimi'
fimifile=os.path.splitext(cxtfile)[0]+'.fimi'
fimifile
convertCxtFileToFimi(cxtfile,fimifile)
with open(fimifile) as f:
  fimitest=f.read()
max(fimitest,key=int)
[list(map(int,re.split('\s',f))) for f in fimitest]
fimitest
[list(re.split('\s',f)) for f in fimitest]
fimitest
[list(re.split('\s*',f)) for f in fimitest]
list(re.split('\s',fimitest))
list(map(int,re.split('\s',fimitest)))
def parseFimifile(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read().splitlines()
	num_objects=len(fimi)
	fimi=[list(map(int,re.split('\s',f))) for f in fimi]
	att_max=max(map(max,fimi))
	att_min=min(map(min,fimi))
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
def parseFimifilev2(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	fimi=list(map(int,re.split('\s',fimi)))
	fimi.sort()
	att_max,att_min=fimi[-1],fimi[0]
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
def parseFimifilev3(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	fimi=list(map(int,re.split('\s',fimi)))
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
with open(fimifile,'r') as f:
		fimi=f.read().splitlines()
num_objects=len(fimi)
fimi2=[list(map(int,re.split('\s',f))) for f in fimi]
att_max=max(map(max,fimi2))
att_min=min(map(min,fimi2))
fimitest
r_line=re.compile('\s*(\d\s)*\s*')
re.findall(r_line,fimitest)
r_line=re.compile('\s*(\d*\s)*\s*')
re.findall(r_line,fimitest)
re.match(r_line,fimitest)
re.finditer(r_line,fimitest)
for e in re.finditer(r_line,fimitest):
   print(e)
r_line=re.compile('\s{0,1}(\d*\s)*\s{0,1}')
for e in re.finditer(r_line,fimitest):
   print(e)
fimites[0:10]
fimitest[0:10]
fimitest[0:42]
r_line=re.compile('\s{0,1}(\d+\s)+\s{0,1}')
for e in re.finditer(r_line,fimitest):
re.findall(r_line,fimitest)
fimitest
r_line=re.compile(r'\s{0,1}(\d+\s)+\s{0,1}')
re.findall(r_line,fimitest)
r_line=re.compile(r'\s{0,1}(\d+\s)+\s{0,1}',re.M)
re.findall(r_line,fimitest)
for e in re.finditer(r_line,fimitest):
   print(e)
lineregexp=re.compile('\s{0,1}(\d+\s{0,1})+\s{0,1}',re.M)
re.findall(r_line,fimitest)
re.findall(lineregexp,fimitest)
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('\s?(\d+\s?)+\s?',re.M)
re.match(lineregexp,fimitest)
re.match(lineregexp,fimitest)
lineregexp=re.compile('\s??(\d+?\s??)+\s??',re.M)
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('(\s??\d+?\s??)+\s??',re.M)
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('(\d+?\s??)+\s??',re.M)
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('\n??(\d+?\s??)+\n??',re.M)
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('\n??(\d+?\s??)+\n??')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('(\d+?\s??)+\n??')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('\n??(\d+?\s??)+')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('(\d+?\s??)+')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('(\d+?)+')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('^\s*(\d+?\s??)+\s*$')
for e in re.finditer(lineregexp,fimitest):
   print(e)
lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
for e in re.finditer(lineregexp,fimitest):
   print(e)
len(re.finditer(lineregexp,fimitest))
len(list(re.finditer(lineregexp,fimitest)))
for e in re.finditer(lineregexp,fimitest):
   print(e.group(0))
for e in re.finditer(lineregexp,fimitest):
   print(e.group(0).split())
for e in re.finditer(lineregexp,fimitest):
   print(map(int,e.group(0).split()))
   print(list(map(int,e.group(0).split())))
lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimitest)]
lineparse
' '.join(lineparse)
list(itertools.chain.from_iterable(lineparse))
import itertools
list(itertools.chain.from_iterable(lineparse))
max(itertools.chain.from_iterable(lineparse))
attributes
num_attributes
globals()
globals().keys()
num_attributes=len(attributes)
num_attributes
contexttest=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
contexttest
def parseFimifile(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read().splitlines()
	num_objects=len(fimi)
	fimi=[list(map(int,re.split('\s',f))) for f in fimi]
	att_max=max(map(max,fimi))
	att_min=min(map(min,fimi))
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
def parseFimifilev2(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	fimi.sort()
	att_max,att_min=fimi[-1],fimi[0]
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
def parseFimifilev3(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=itertools.chain.from_iterable(lineparse)
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
fimitest
fimitest2=itertools.chain.from_iterable(lineparse)
att_max,att_min=max(fimi),min(fimi)
fimifile
parseFimifilev3(fimifile)
def parseFimifilev3(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=itertools.chain.from_iterable(lineparse)
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
parseFimifilev3(fimifile)
att_max,att_min=max(fimitest2),min(fimitest2)
def parseFimifilev3(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
lineparse
def parseFimifilev3(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	att_max,att_min=max(fimi),min(fimi)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
def parseFimifilev4(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi,count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=100,globals=globals()))
def parseFimifilev4(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	num_objects=fimi.count('\n')
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	att_max,att_min=max(fimi),min(fimi)
	num_attributes=att_max-att_min+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifile(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=100,globals=globals()))
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
def parseCxtfilev2(cxtfile):
	with open(cxtfile,'r') as f:
		cxt=f.read()
	r=re.compile('B\s{2}(?P<num_obj>\d*)\s(?P<num_att>\d*)\s{2}(?P<vals>(.*\s*)*)',re.M)
	m=re.match(r,cxt)
	num_objects,num_attributes,vals=int(m.group('num_obj')),int(m.group('num_att')),m.group('vals')
	vals_split=vals.split('\n',num_objects)
	vals_split2=vals_split[-1].split('\n',num_attributes)
	objects,attributes,context=vals_split[:-1],vals_split2[:-1],vals_split2[-1].translate({ord("X"):'1',ord("."):'0'}).split()
	return objects,attributes,context
cxtfile
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
51.2831859041471/1.9703079420141876
fimitest2
fimitest
fimi
lineparse
import numpy
numpy.min(lineparse)
fimitest
fimi2
globals().keys()
fimi3=list(itertools.chain.from_iterable(lineparse))
numpy.min(fimi3)
numpy.max(fimi3)
numpy.max(lineparse)
def parseFimifilev5(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	fimi=list(itertools.chain.from_iterable(lineparse))
	att_max,att_min=numpy.max(fimi),numpy.min(fimi)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
def parseFimifilev6(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineregexp=re.compile('^\s*(\d+?\s??)+\s*$',re.M)
	lineparse=[list(map(int,e.group(0).split())) for e in re.finditer(lineregexp,fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev5(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev6(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
fimi
fimitest
fimitest.split()
fimitest.split('\n')
[int(s) for s in re.split('[\s,]+',fimitest)]
[int(s) for s in re.split('\n',fimitest)]
[[int(i) for i in s.split()] for s in re.split('\n',fimitest)]
def parseFimifilev7(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=max(lineparse),min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev5(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev6(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
def parseFimifilev7(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in lineparse]
	return num_objects,num_attributes,context
def parseFimifilev8(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=[[int(i) for i in s.split()] for s in f.read().splitlines()]
	att_max,att_min=numpy.max(fimi),numpy.min(fimi)
	num_attributes,num_objects=att_max-att_min+1,len(fimi)
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifile(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev2(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev3(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev4(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev5(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev6(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev8(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
def parseFimifilev9(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=[[int(i) for i in s.split()] for s in f.read().splitlines()]
	num_objects,num_attributes=len(fimi),numpy.max(fimi)-numpy.min(fimi)+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifilev8(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev9(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
fimi
fimitest
fimitest2
fimi2
[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for j in rfimi2]
def parseFimifilev10(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=[[int(i) for i in re.split('\s',s)] for s in re.split('\n',f.read())]
	num_objects,num_attributes=len(fimi),numpy.max(fimi)-numpy.min(fimi)+1
	context=[''.join(['1' if i in e else '0' for i in range(num_attributes)]) for e in fimi]
	return num_objects,num_attributes,context
parseFimifilev10(fimifile)
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev8(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev9(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev10(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
import cProfile
import pstats
cprofile.run("parseFimifilev9(fimifile)",'profile.out')
cProfile.run("parseFimifilev9(fimifile)",'profile.out')
results=pstats.Stats('profile.out')
results.sort_stats('ncalls')
result.print_stats()
results.print_stats()
results.print_callees()
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev8(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev9(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev10(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseCxtfilev2(cxtfile)",number=500,globals=globals()))
cProfile.run("parseFimifilev7(fimifile)",'profile.out')
results=pstats.Stats('profile.out')
results.sort_stats('ncalls')
results.print_stats()
results.print_callees()
def parseFimifilev11(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[''.join(('1' if i in e else '0' for i in range(num_attributes))) for e in lineparse]
	return num_objects,num_attributes,context
parseFimifilev11(fimifile)
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev11(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev11(fimifile)",number=500,globals=globals()))
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=((int(i) for i in s.split()) for s in re.split('\n',fimi))
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=[''.join(('1' if i in e else '0' for i in att_range)) for e in lineparse]
	return num_objects,num_attributes,context
parseFimifilev12(fimifile)
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[(int(i) for i in s.split()) for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=[''.join(('1' if i in e else '0' for i in att_range)) for e in lineparse]
	return num_objects,num_attributes,context
parseFimifilev12(fimifile)
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=[''.join(['1' if i in e else '0' for i in att_range]) for e in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev11(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev12(fimifile)",number=500,globals=globals()))
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=list(''.join(list(('1' if i in e else '0' for i in att_range)) for e in lineparse)
	return num_objects,num_attributes,context
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=list((''.join(list(('1' if i in e else '0' for i in att_range)) for e in lineparse))
	return num_objects,num_attributes,context
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	att_range=range(num_attributes)
	context=list(''.join(list(('1' if i in e else '0' for i in att_range)) for e in lineparse))
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev11(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev12(fimifile)",number=500,globals=globals()))
def assignTrue(liste,index):
	liste[index]='1'
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	#att_range=range(num_attributes)
	context=[['0']*num_attributes for range(num_objects)]
	context=[map(assignTrue,context[i],lineparse[i]) for i in range(num_objects)]
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[['0']*num_attributes for i in range(num_objects)]
	context=[map(assignTrue,context[i],lineparse[i]) for i in range(num_objects)]
	return num_objects,num_attributes,context
parseFimifilev12(fimifile)
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[['0']*num_attributes for i in range(num_objects)]
	context=[''.join(map(assignTrue,context[i],lineparse[i])) for i in range(num_objects)]
	return num_objects,num_attributes,context
parseFimifilev12(fimifile)
['0']*6
u=[2,4]
v=['0']*6
lineparse
att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[['0']*num_attributes for i in range(num_objects)]
num_attributes,num_objects=att_max-att_min+1,len(lineparse)
context=[['0']*num_attributes for i in range(num_objects)]
context
for c in context:
   map(assignTrue,c,lineparse[context.index(c)])
for c in context:
   list(map(assignTrue,c,lineparse[context.index(c)]))
for c in context:
   print(c)
for i in range(num_objects):
   print(context[i])
   print(lineparse[i])
context=[['0']*num_attributes]*num_objects
context
def parseFimifilev12(fimifile): #build context object from input fimi file
	with open(fimifile,'r') as f:
		fimi=f.read()
	lineparse=[[int(i) for i in s.split()] for s in re.split('\n',fimi)]
	att_max,att_min=numpy.max(lineparse),numpy.min(lineparse)
	num_attributes,num_objects=att_max-att_min+1,len(lineparse)
	context=[['0']*num_attributes]*num_objects
	context=[''.join(['1' if i in line else e for i, e in enumerate(line)]) for line in lineparse]
	return num_objects,num_attributes,context
print(timeit.timeit("parseFimifilev7(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev11(fimifile)",number=500,globals=globals()))
print(timeit.timeit("parseFimifilev12(fimifile)",number=500,globals=globals()))
[]+['rubbish']
testl=[]
testl+=['rubbish']
testl
num_objects
num_attributes
context
import fcbo as fcbo2
cxtfile
contexttest=fcbo2.Context(cxtfile,'context')
contexttest
contexttest.generateAllIntentsWithSupp(0.5)
print(contexttest.generateAllIntentsWithSupp(0.5),file=open('log_conceptgeneration.txt',w))
print(contexttest.generateAllIntentsWithSupp(0.5),file=open('log_conceptgeneration.txt','w'))
import importlib
import importlib as imp
imp.reload(fcbo2)
print(contexttest.generateAllIntentsWithSupp(0.5),file=open('log_conceptgeneration.txt','a+'))
print(contexttest.generateAllIntentsWithSupp(0.5,'log_conceptgeneration.txt'),file=open('log_conceptgeneration.txt','a+'))
contexttest=fcbo2.Context(cxtfile,'context')
print(contexttest.generateAllIntentsWithSupp(0.5,'log_conceptgeneration.txt'),file=open('log_conceptgeneration.txt','a+'))
contexttest.generateAllIntentsWithSupp(0.5,'log_conceptgeneration.txt')
contexttest.generateAllIntentsWithSupp(0.5,'log_conceptgeneration2.txt')
cxtfile2='/home/gberrada/Downloads/In-Close2/liveinwater.cxt'
contexttest.generateAllIntentsWithSupp(0,'log_conceptgenerationwater.txt')
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp(0,'log_conceptgenerationwater.txt')
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp(0,'log_conceptgenerationwater.txt')
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp(0,'log_conceptgenerationwater.txt')
concepts
concepts={((0, 1, 1, 1, 0, 0, 0, 0), (1, 0, 0, 0, 0, 0, 1, 1, 0)), ((0, 0, 0, 0, 1, 1, 0, 0), (1, 1, 0, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 0, 0, 1, 1, 1), (1, 0, 1, 1, 0, 0, 0, 0, 0)), ((0, 1, 1, 0, 0, 0, 0, 0), (1, 1, 0, 0, 0, 0, 1, 1, 0)), ((0, 0, 1, 1, 0, 0, 0, 0), (1, 0, 1, 0, 0, 0, 1, 1, 0)), ((0, 0, 1, 0, 0, 1, 0, 0), (1, 1, 1, 0, 0, 0, 0, 0, 0)), ((0, 0, 0, 0, 1, 1, 0, 1), (1, 0, 0, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 1, 0, 0, 0, 0), (1, 0, 1, 0, 0, 0, 1, 1, 1)), ((1, 1, 1, 1, 1, 1, 1, 1), (0, 0, 0, 0, 0, 0, 0, 0, 0)), ((1, 1, 1, 0, 1, 1, 0, 0), (1, 1, 0, 0, 0, 0, 0, 0, 0)), ((0, 0, 0, 0, 0, 0, 0, 0), (1, 1, 1, 1, 1, 1, 1, 1, 1)), ((0, 0, 0, 0, 1, 1, 1, 1), (1, 0, 0, 1, 0, 0, 0, 0, 0)), ((1, 1, 1, 1, 1, 1, 1, 1), (1, 0, 0, 0, 0, 0, 0, 0, 0)), ((1, 1, 1, 0, 0, 0, 0, 0), (1, 1, 0, 0, 0, 0, 1, 0, 0)), ((0, 0, 0, 0, 0, 1, 0, 1), (1, 0, 1, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 0, 0, 1, 0, 0), (1, 1, 1, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 0, 0, 0, 1, 0), (1, 0, 1, 1, 1, 0, 0, 0, 0)), ((0, 0, 1, 1, 0, 1, 1, 1), (1, 0, 1, 0, 0, 0, 0, 0, 0)), ((0, 0, 1, 0, 0, 0, 0, 0), (1, 1, 1, 0, 0, 0, 1, 1, 0)), ((1, 1, 1, 1, 0, 0, 0, 0), (1, 0, 0, 0, 0, 0, 1, 0, 0))}
type(concepts)
dict(zip(*concepts))
unfiltered_concepts=[(i,concepts[i][1].count(1)) for i in range(len(concepts))]
concepts=list(concepts)
unfiltered_concepts=[(i,concepts[i][1].count(1)) for i in range(len(concepts))]
unfiltered_concepts
unfiltered_concepts=[(i,concepts[i][0],concepts[i][1].count(1)) for i in range(len(concepts))]
unfiltered_concepts
len(unfiltered_list)
len(unfiltered_concepts)
unfiltered_concepts.sort(key=operator.itemgetter(1))
import operator
unfiltered_concepts.sort(key=operator.itemgetter(1))
unfiltered_concepts
unfiltered_concepts=[(i,concepts[i][0],concepts[i][1].count(1)) for i in range(len(concepts))].sort(key=operator.itemgetter(1))
unfiltered_concepts
unfiltered_concepts=sorted([(i,concepts[i][0],concepts[i][1].count(1)) for i in range(len(concepts))],key=operator.itemgetter(1))
unfiltered_concepts
import itertools
list(max(v,key=operator.itemgetter(1)) for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1)))
list(max(v,key=operator.itemgetter(2)) for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1)))
list(max(v,key=operator.itemgetter(2))[0] for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1)))
list(max(v,key=operator.itemgetter(2))[0] for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1))).sort()
sorted(list(max(v,key=operator.itemgetter(2))[0] for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1))))
len(sorted(list(max(v,key=operator.itemgetter(2))[0] for k,v in itertools.groupby(unfiltered_concepts,operator.itemgetter(1)))))
fcbo
fcbo2
imp.reload(fcbo2)
contexttest.num_attributes
contexttest.num_objects
imp.reload(fcbo2)
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=500,globals=globals()))
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=500,globals=globals()))
contexttest=fcbo2.Context(cxtfile2,'context')
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
contexttest.generateAllIntentsWithSupp2(0)
cxtfile
contexttest2=fcbo2.Context(cxtfile,'context')
print(timeit.timeit("contexttest2.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest2.generateAllIntentsWithSupp(0)",number=10,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
concepts
(0, 0, 0, 0, 0, 0, 0, 0) in (c[0] for c in concepts)
(0, 0, 0, 0, 0, 0, 0, 1) in (c[0] for c in concepts)
(c[0] for c in concepts)
[c[0] for c in concepts]
concepts2=set(concepts)
list(filter(lambda x:x[0]==(0, 0, 0, 0, 0, 0, 0, 0),concepts2))
filter(lambda x:x[0]==(0, 0, 0, 0, 0, 0, 0, 0),concepts2)
list(filter(lambda x:x[0]==(0, 0, 0, 0, 0, 0, 0, 0),concepts2))[0]
list(filter(lambda x:x[0]==(0, 0, 0, 0, 0, 0, 0, 0),concepts2))[0][1].count(1)
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
for c in {((1, 1, 1, 1, 1, 1, 1, 1), (0, 0, 0, 0, 0, 0, 0, 0, 0))}
for c in {((1, 1, 1, 1, 1, 1, 1, 1), (0, 0, 0, 0, 0, 0, 0, 0, 0))}:
   print(c)
for c in {((1, 1, 1, 1, 1, 1, 1, 1), (0, 0, 0, 0, 0, 0, 0, 0, 0))}:
   print(c[0])
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
concept
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
print(timeit.timeit("contexttest.generateAllIntentsWithSupp(0)",number=1500,globals=globals()))
print(timeit.timeit("contexttest.generateAllIntentsWithSupp2(0)",number=1500,globals=globals()))
concept={((1, 1, 1, 1, 0, 0, 0, 0), (1, 0, 0, 0, 0, 0, 1, 0, 0)), ((1, 1, 1, 0, 1, 1, 0, 0), (1, 1, 0, 0, 0, 0, 0, 0, 0)), ((0, 0, 0, 0, 0, 0, 0, 0), (1, 1, 1, 1, 1, 1, 1, 1, 1)), ((0, 0, 0, 0, 1, 1, 1, 1), (1, 0, 0, 1, 0, 0, 0, 0, 0)), ((0, 1, 1, 1, 0, 0, 0, 0), (1, 0, 0, 0, 0, 0, 1, 1, 0)), ((1, 1, 1, 1, 1, 1, 1, 1), (1, 0, 0, 0, 0, 0, 0, 0, 0)), ((1, 1, 1, 0, 0, 0, 0, 0), (1, 1, 0, 0, 0, 0, 1, 0, 0)), ((0, 0, 0, 0, 1, 1, 0, 0), (1, 1, 0, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 0, 0, 1, 1, 1), (1, 0, 1, 1, 0, 0, 0, 0, 0)), ((0, 0, 0, 0, 1, 1, 0, 1), (1, 0, 0, 1, 0, 1, 0, 0, 0)), ((0, 0, 0, 0, 0, 1, 0, 0), (1, 1, 1, 1, 0, 1, 0, 0, 0)), ((0, 1, 1, 0, 0, 0, 0, 0), (1, 1, 0, 0, 0, 0, 1, 1, 0)), ((0, 0, 0, 0, 0, 0, 1, 0), (1, 0, 1, 1, 1, 0, 0, 0, 0)), ((0, 0, 1, 1, 0, 0, 0, 0), (1, 0, 1, 0, 0, 0, 1, 1, 0)), ((0, 0, 1, 1, 0, 1, 1, 1), (1, 0, 1, 0, 0, 0, 0, 0, 0)), ((0, 0, 1, 0, 0, 0, 0, 0), (1, 1, 1, 0, 0, 0, 1, 1, 0)), ((0, 0, 0, 0, 0, 1, 0, 1), (1, 0, 1, 1, 0, 1, 0, 0, 0)), ((0, 0, 1, 0, 0, 1, 0, 0), (1, 1, 1, 0, 0, 0, 0, 0, 0)), ((0, 0, 0, 1, 0, 0, 0, 0), (1, 0, 1, 0, 0, 0, 1, 1, 1))}
concept.intersection({((1, 1, 1, 1, 1, 1, 1, 1), (0, 0, 0, 0, 0, 0, 0, 0, 0))})
list(filter(lambda x:x[0]==(0, 0, 0, 0, 0, 0, 0, 0),concepts2))[0][1].count(1)
list(filter(lambda x:x[0]==(1,1,1,1,1,1,1,1,1),concept))
list(filter(lambda x:x[0]==(1, 1, 1, 1, 0, 0, 0, 0),concept))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
len(contexttest.generateAllIntentsWithSupp2(0))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
len(contexttest.generateAllIntentsWithSupp2(0))
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
imp.reload(fcbo2)
contexttest.generateAllIntentsWithSupp2(0)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
imp.reload(fcbo2)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
len(contexttest.generateAllIntentsWithSupp2(0))
imp.reload(fcbo2)
contexttest.generateAllIntentsWithSupp2(0)
contexttest=fcbo2.Context(cxtfile2,'context')
contexttest.generateAllIntentsWithSupp2(0)
c=contexttest.generateAllIntentsWithSupp2(0)
[c if c[0]==(1,1,1,1,1,1,1,1,1)]
[e for e in c if e[0]==(1,1,1,1,1,1,1,1,1)]
c
[e for e in c if e[0]==(1, 1, 1, 1, 1, 1, 1, 1)]
import dask.dataframe as dd
csv1='/home/gberrada/csv/cadetsPandex/Subjects.csv'
csv2='/home/gberrada/csv/cadetsPandex/Events.csv'
table1=dd.read_csv(csv1,low_memory=False)
table1bis=dd.read_csv(csv1)
print(timeit.timeit("table1=dd.read_csv(csv1,low_memory=False)",number=1500,globals=globals()))
print(timeit.timeit("table1=dd.read_csv(csv1)",number=1500,globals=globals()))
print(timeit.timeit("table1=dd.read_csv(csv1,low_memory=False)",number=1500,globals=globals()))
print(timeit.timeit("table1bis=dd.read_csv(csv1)",number=1500,globals=globals()))
print(timeit.timeit("table2=dd.read_csv(csv2,low_memory=False)",number=1500,globals=globals()))
print(timeit.timeit("table2bis=dd.read_csv(csv2)",number=1500,globals=globals()))
print(timeit.timeit("table1=dd.read_csv(csv1,low_memory=False)",number=1500,globals=globals()))
print(timeit.timeit("table1bis=dd.read_csv(csv1)",number=1500,globals=globals()))
print(timeit.timeit("table2=dd.read_csv(csv2,low_memory=False)",number=1500,globals=globals()))
print(timeit.timeit("table2bis=dd.read_csv(csv2)",number=1500,globals=globals()))
table2
table2=dd.read_csv(csv2,low_memory=False)
merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
csv1_key='uuid'
csv1_key='subjectUuid'
csv1_key='uuid'
csv2_key='subjectUuid'
merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
type_join='inner'
merged_table=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join)
merged_table2=dd.merge(table1,table2,left_on=csv1_key,right_on=csv2_key,how=type_join,copy=False)
dask --version
dask.version
dd.version
import dask
dask.version
dask.__version__
gt=dd.read_csv("csv_results/CADETS_Ground_Truth_Review/CADETS_Ground_Truth_Review/labeled_ground_truth/bovia_webshell.csv",low_memory=False)
gt=dd.read_csv("csv_results/CADETS_Ground_Truth_Review/labeled_ground_truth/bovia_webshell.csv",low_memory=False)
gt['d_predicate']
gt['d_predicates']
gt['d_predicates'][0]
gt.head()
gt
t=['','42','5','7']
tuple([int(i) if i!='' else '' for i in ])
tuple([int(i) if i!='' else '' for i in t])
tuple(int(i) if i!='' else '' for i in t)
t=range(42)
t=[str(e) for e in t]
t.append('')
t=range(4200)
t=[str(e) for e in t]
t.append('')
t
print(timeit.timeit("tuple(int(i) if i!='' else '' for i in t)",number=1500,globals=globals()))
print(timeit.timeit("tuple([int(i) if i!='' else '' for i in t])",number=1500,globals=globals()))
print(timeit.timeit("tuple(int(i) if i!='' else '' for i in t)",number=1500,globals=globals()))
print(timeit.timeit("tuple([int(i) if i!='' else '' for i in t])",number=1500,globals=globals()))
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
parseConceptFile('csv_results/top10/cadets_concept_support0_9_minconf0_95.txt')
import strtobool
from distutils.util import strtobool
parseConceptFile('csv_results/top10/cadets_concept_support0_9_minconf0_95.txt')
gt
def parseConceptFile(concept_file):
	with open(concept_file,'r') as f:
		content=f.read().translate({ord('\n'):'#'})
	named_regexp=re.compile('#Named:(?P<named>True|False)')
	named=bool(strtobool(re.search(named_regexp,content).group('named')))
	split_content=content.split('#')
	print(split_content)
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
parseConceptFile('csv_results/top10/cadets_concept_support0_9_minconf0_95.txt')
def parseConceptFile(concept_file):
	with open(concept_file,'r') as f:
		content=f.read().translate({ord('\n'):'#'})
	named_regexp=re.compile('#{0,1}Named:(?P<named>True|False)')
	named=bool(strtobool(re.search(named_regexp,content).group('named')))
	split_content=content.split('#')
	print(split_content)
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
parseConceptFile('csv_results/top10/cadets_concept_support0_9_minconf0_95.txt')
parseConceptFile('conceptstest.txt')
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
parseCfcaOutput('conceptstest.txt')
parseConceptFile('conceptstest2.txt')
s
s=3
if s!=1:
   break
def f(x):
  if x!=1:
    print(x)
  else:
    return
f(s)
s=1
f(s)
def f(x):
  if x!=1:
    print(x)
  else:
    sys.exit()
import sys
f(s)
exit()
import json
json.load("""["{id=80e27f78-1abe-3063-b821-b0e78b4b1100, filepath=/usr/local/etc/pam.d/sudo}","{id=1038c0d8-c3c9-5,filepath=localhost/home}"]""")
l=['blabla','568','fatigue']
'\n'.join(l)
list(frozenset({'EVENT_OPEN'}))
exit()
d=[{'id':'1','type':'rubbish','ip':'1.1.1.1'},{'id':'2','type':'good','ip':'1.1.2.1'},{'id':'3','type':'rubbish','ip':'2.1.1.1'},{'id':'4','type':'good','ip':'1.5.1.1'}]
att_name=['id','ip','type']
attributes=list({e[att_name] for e in query_content})
attributes=list({e[att_name] for e in d})
attributes=list({e[a] for a in att_name for e in d})
attributes
attributes=list({str(a)+'#'+str(e[a]) for a in att_name for e in d})
attributes
exit()
import re
r=re.compile('\w+#(?P<name>\w+)')
s='type#machin'
s2='filepath#a/b/c'
re.match(r,s)
re.match(r,s).group('name')
re.match(r,s2).group('name')
r=re.compile('\w+#(?P<name>.+)')
re.match(r,s2).group('name')
r=re.compile('\.+#(?P<name>.+)')
re.match(r,s2).group('name')
r=re.compile('(\w|\W)+#(?P<name>.+)')
re.match(r,s2).group('name')
re.match(r,s).group('name')
s3='filepath_45788#a/b/c'
re.match(r,s3).group('name')
s3='filepath,#a/b/c'
re.match(r,s3).group('name')
exit
exit()
f=frozenset({'read','write'})
list(f)
g=frozenset({'execute','mmap','read'})
g.union(f)
exit()
sets=[('a','c'),('b','d')]
l,r=zip(*sets)
l
r
exit()
{'a':1, 'b':2, 'c':3}
p={'a':1, 'b':2, 'c':3}
type(p)
p={'a':[1,2,3], 'b':[2,4,4], 'c':[3,5]}
p
p={'a':[frozenset({'1'}),frozenset({'2'}),frozenset({'3'})], 'b':[frozenset({'2'}),frozenset({'4'}),frozenset({'4'})], 'c':[frozenset({'3'}),frozenset({'5'})]}
p
import glob
import os
os.path.exists('adaptNew/adapt/fca/contextSpecFiles/neo4j_ProcessEvent.csv')
os.path.exists('adaptNew/adapt/fca/contextSpecFiles/neo4j_ProcessEvent.json')
os.path.exists('adaptNew/adapt/fca/contextSpecFiles/neo4jspec_ProcessEvent.json')
import sys
os.chdir('adaptNew/adapt/fca')
os.getcwd()
import fcbo
import fca
import concept_analysis as analysis
fca_context=fcbo.Context('csvContexts/cadets_bovia_ProcessEvent_1.csv','','context')
result=fca.fca(fca_context,0.95,True,sys.stdout,proceed_flag=True,quiet_flag=True)
result
result.concepts
type(result.concepts)
for e in result.concepts:
  print(e)
itemsets={frozenset(c[1]) for c in result.concepts}
itemsets
supports=dict((c,analysis.getItemsetScaledSupport(context,c,namedentities=True)) for c in concepts)
supports=dict((c,analysis.getItemsetScaledSupport(context,c,namedentities=True)) for c in itemsets)
supports=dict((c,analysis.getItemsetScaledSupport(fca_context,c,namedentities=True)) for c in itemsets)
supports
rules = analysis.getRulesWithConfidence(itemsets,supports,0.8)
rules
rules = analysis.getRulesWithConfidence(itemsets,supports,0.98)
rules
rules = analysis.getRulesWithConfidence(itemsets,supports,0.997)
rules
rules = analysis.getRulesWithConfidenceLift(itemsets,supports,0.997)
import importlib as imp
imp.reload('analysis')
imp.reload(analysis)
rules = analysis.getRulesWithConfidenceLift(itemsets,supports,0.997)
rules
reducedRules = analysis.filterSubsumedImp(rules)
rules
reducedRules
imp.reload(analysis)
rules = analysis.findImpRules(itemsets,supports,0.0995)
rules
imp.reload(analysis)
rules = analysis.findImpRules(itemsets,supports,0.0995)
rules
rules = analysis.getRulesWithConfidenceLift(itemsets,supports,0.997)
rules
reducedRules = analysis.filterSubsumedImp(rules)
reducedRules
def findImpRules(concepts,supports,confidence):
	rules = analysis.getRulesWithConfidenceLift(concepts,supports,confidence)
    reducedRules = analysis.filterSubsumedImp(rules)
def findImpRules(concepts,supports,confidence):
	rules = analysis.getRulesWithConfidenceLift(concepts,supports,confidence)
	reducedRules = analysis.filterSubsumedImp(rules)
	return reducedRules
findImpRules(itemsets,supports,0.997)
def violations(s,rules):
	setS=(frozenset({s}) if type(s)==str else frozenset(s))
	ent = 0-sum([numpy.log2(1-rules[e][0]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
	entLift = 0-sum([numpy.log2(1-rules[e][1]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
	return (ent,entLift)
def allViolations(context,rules,namedentities=False):
	if namedentities==False:
		list_objects=range(context.num_objects)
	else:
		list_objects=context.objects
	return {k:violations(context.getAttributeFromObject(k,namedentities),rules) for k in list_objects}
allViolations(fca_context,reducedRules,namedentities=True)
import numpy
allViolations(fca_context,reducedRules,namedentities=True)
vios=allViolations(fca_context,reducedRules,namedentities=True)
violations=(sorted(vios.items(),key=snd,reverse=True)[0:num_rules] if type(num_rules)==int else sorted(vios.items(),key=snd,reverse=True))
violations=(sorted(vios.items(),key=snd,reverse=True)[0:20] if type(num_rules)==int else sorted(vios.items(),key=snd,reverse=True))
violations=(sorted(vios.items(),key=snd,reverse=True)[0:20] if type(20)==int else sorted(vios.items(),key=snd,reverse=True))
def snd(x): return x[1]
violations=(sorted(vios.items(),key=snd,reverse=True)[0:20] if type(20)==int else sorted(vios.items(),key=snd,reverse=True))
violations
vios.items()
vios.items()[0]
list(vios.items())[0]
snd(list(vios.items())[0])
def comp(x): return (x[1][0],x[1][1])
num_rules=20
violations=(sorted(vios.items(),key=comp,reverse=True)[0:num_rules] if type(num_rules)==int else sorted(vios.items(),key=comp,reverse=True))
violations
setAtts=frozenset(context.getAttributeFromObject(violations[0],True))
setAtts=frozenset(fca_context.getAttributeFromObject(violations[0],True))
setAtts=frozenset(fca_context.getAttributeFromObject(violations[0][0],True))
setAtts
violated_rules=dict((e,rules[e]) for e in rules.keys() if e[0] <= setAtts and not(e[1] <= setAtts))
violated_rules
max(stats.iterkeys(), key=(lamda key: stats[key][0]))
max(stats.iterkeys(), key=(lambda key: stats[key][0]))
max(violated_rules.iterkeys(), key=(lambda key: violated_rules[key][0]))
max(violated_rules, key=(lambda key: violated_rules[key][0]))
max(violated_rules.iteritems(), key=(lambda key: violated_rules[key][0]))
max(violated_rules.items(), key=(lambda key: violated_rules[key][0]))
max(violated_rules.items(), key=(lambda x: x[1][0]))
max(violated_rules.items(), key=(lambda x: x[1][1]))
violated_rules
','.join(["Objects","TypeRule","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence","TopViolatedRulesForEachObjectConfidence","TopScoreConfidence","AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift","TopScoreLift"])
[','.join(["Objects","TypeRule","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence","TopViolatedRulesForEachObjectConfidence","TopScoreConfidence","AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift","TopScoreLift"])]
violated_rules.keys()
violated_rules.keys()[0]
list(violated_rules.keys())[0]
list(violated_rules.keys())[0][0]
str(list(violated_rules.keys())[0][0])
list(list(violated_rules.keys())[0][0])
','.join(list(list(violated_rules.keys())[0][0]))
','.join(list(list(violated_rules.keys())[0][0]))+'=>'+','.join(list(list(violated_rules.keys())[0][1]))
';'.join([','.join(list(list(v[0]))+'=>'+','.join(list(list(v[1])) for v in violated_rules.keys()])
[','.join(list(list(v[0]))+'=>'+','.join(list(list(v[1])) for v in violated_rules.keys()]
[','.join(list(list(v[0]))+'=>' for v in violated_rules.keys()]
[','.join(list(list(v[0]))) for v in violated_rules.keys()]
';'.join([','.join(list(list(v[0])))+'=>'+','.join(list(list(v[1]))) for v in violated_rules.keys()])
violated_rules.keys()
'#'.join([';'.join(list(list(v[0])))+'=>'+';'.join(list(list(v[1]))) for v in violated_rules.keys()])
m=max(violated_rules.items(), key=(lambda x: x[1][0]))
m
m[0]
';'.join(list(list(m[0][0])))+'=>'+';'.join(list(list(m[0][1])))
m[1]
m[1][0]
imp.reload(analysis)
analysis.produceScoreCSVPerRuleType(violations,fca_context,rules,'implication',namedentities=True)
imp.reload(analysis)
analysis.produceScoreCSVPerRuleType(violations,fca_context,rules,'implication',namedentities=True)
header=[','.join(["Objects","TypeRule","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence","TopViolatedRulesForEachObjectConfidence","TopScoreConfidence","AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift","TopScoreLift"])+'\n']
csv_body=analysis.produceScoreCSVPerRuleType(violations,fca_context,rules,'implication',namedentities=True)
''.join(header+csv_body)
csv_body=[analysis.produceScoreCSVPerRuleType(violations,fca_context,rules,'implication',namedentities=True)]
''.join(header+csv_body)
output=sys.stdout
with analysis.openFile(output) as f:
   f.write(''.join(header+csv_body))
output='test_scoreCSV.csv'
with analysis.openFile(output) as f:
   f.write(''.join(header+csv_body))
analysis.getConceptSupport(fca_context,frozenset({'EVENT_SIGNAL'}),namedentities=True)
fca_context
fca_context=fcbo.Context('csvContexts/cadets_bovia_ProcessEvent_1.csv','','context')
exit()
import os
os.chdir('adaptNew/adapt/fca')
import fcbo,fca
import concept_analysis as analysis
fca_context=fcbo.Context('csvContexts/cadets_bovia_ProcessEvent_1.csv','','context')
analysis.getConceptSupport(fca_context,frozenset({'EVENT_SIGNAL'}),namedentities=True)
analysis.getConceptSupport(fca_context,frozenset({'EVENT_OTHER'}),namedentities=True)
analysis.getItemsetScaledSupport(fca_context,frozenset({'EVENT_OTHER'}),namedentities=True)
l=[frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_EXIT'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_WRITE'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_OPEN', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_RENAME', 'EVENT_OPEN', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MMAP', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_SIGNAL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_SIGNAL'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_FORK'}), frozenset({'EVENT_LOGIN', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_SENDTO', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_MODIFY_PROCESS', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_RENAME', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_EXIT', 'EVENT_CLOSE', 'EVENT_CONNECT', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_OTHER', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_WRITE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SIGNAL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_RENAME', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_CONNECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FCNTL'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CLOSE', 'EVENT_SIGNAL', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SIGNAL', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_WRITE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_FCNTL'}), frozenset({'EVENT_OPEN', 'EVENT_CREATE_OBJECT', 'EVENT_OTHER', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MMAP', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset(), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_WRITE', 'EVENT_EXIT'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_WRITE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_OTHER', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_CONNECT', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_MMAP'}), frozenset({'EVENT_MMAP', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_READ'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_OTHER', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_MMAP', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CLOSE', 'EVENT_SIGNAL'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_SENDTO', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_OTHER', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_SIGNAL'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_MODIFY_PROCESS', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_SENDTO', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_WRITE', 'EVENT_CLOSE'}), frozenset({'EVENT_WRITE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXIT'}), frozenset({'EVENT_SENDTO'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FCNTL'}), frozenset({'EVENT_OPEN'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_LSEEK', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CREATE_OBJECT', 'EVENT_OTHER', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_WRITE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_SIGNAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_CLOSE'}), frozenset({'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_SENDTO'}), frozenset({'EVENT_SENDTO', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_CHANGE_PRINCIPAL'}), frozenset({'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_WRITE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_MMAP', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_SIGNAL', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_SENDTO', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_READ', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CHANGE_PRINCIPAL', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_OTHER', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_SENDTO', 'EVENT_CLOSE', 'EVENT_LSEEK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_FCNTL', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN'}), frozenset({'EVENT_SENDTO', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_SIGNAL', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_READ', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_CONNECT', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_FORK'}), frozenset({'EVENT_OPEN', 'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_CONNECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_EXIT', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXIT', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FCNTL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT'}), frozenset({'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_EXECUTE', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_CLOSE', 'EVENT_SIGNAL', 'EVENT_FORK'}), frozenset({'EVENT_EXECUTE', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_CREATE_OBJECT'}), frozenset({'EVENT_EXECUTE', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_OPEN', 'EVENT_CLOSE'}), frozenset({'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_MODIFY_PROCESS', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_OPEN', 'EVENT_MODIFY_PROCESS', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP'}), frozenset({'EVENT_READ', 'EVENT_WRITE', 'EVENT_CLOSE', 'EVENT_EXIT'}), frozenset({'EVENT_WRITE', 'EVENT_EXIT'}), frozenset({'EVENT_EXECUTE', 'EVENT_CREATE_OBJECT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_FORK'}), frozenset({'EVENT_SENDTO', 'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_MODIFY_PROCESS', 'EVENT_CHANGE_PRINCIPAL', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE'}), frozenset({'EVENT_OPEN', 'EVENT_MMAP', 'EVENT_CLOSE', 'EVENT_EXIT', 'EVENT_FORK'}), frozenset({'EVENT_CREATE_OBJECT', 'EVENT_EXIT', 'EVENT_LSEEK', 'EVENT_OPEN', 'EVENT_CONNECT', 'EVENT_READ', 'EVENT_CLOSE', 'EVENT_MMAP', 'EVENT_FORK'})]
frozenset({'EVENT_CREATE_OBJECT'}) in l
s
s="""" implication """"
s=""" " implication " """
s
s.strip()
s.translate(None, string.whitespace)
import string
s.translate(None, string.whitespace)
s.translate(None: string.whitespace)
s.translate({None:string.whitespace})
l
b
b=['implication', '0.8', '*', 'implication.json']
b[1:-1]
c
c=[['implication', '0.8', '*', 'implication.json'], ['anti-implication', '0.5', '*', 'antiImplication.json']]
def test(dict_test,path_test):
d
paths=dict((v[0],v[-1]) for v in c)
paths
dict_rule_properties=(dict((v[0],v[1:-1]) for v in c))
dict_rule_properties
','.join(list(iter(paths)))
','.join(list(iter(dict_rule_properties)))
','.join(list(dict_rule_properties.items()))
','.join(list(dict_rule_properties.viewitems()))
print(list(dict_rule_properties.items())))
print(list(dict_rule_properties.items()))
def test(d,p):
   for k in d.keys():
     print(k,','.join(list(d[k])),p[k])
map(test,dict_rule_properties,paths)
list(map(test,dict_rule_properties,paths))
map(test,dict_rule_properties,paths)
def test(d,p):
   k=list(d.keys())[0]
   print(k,','.join(list(d[k])),p[k])
k='implication'
test(dict_rule_properties[k],paths[k])
dict_rule_properties[k]
def test(d,p):
   k=list(d.keys())[0]
   print(k,','.join(d[k]),p[k])
test(dict_rule_properties[k],paths[k])
def test(d,p):
   k=list(d.keys())[0]
   print(k,d[k])
   print(k,','.join(d[k]),p)
test(dict_rule_properties[k],paths[k])
dict_rule_properties[k]
paths[k]
def test(k,d,p):
   print(k,','.join(d[k]),p[k])
test(k,dict_rule_properties,paths)
map(test,paths.keys(),dict_rule_properties,paths)
list(map(test,paths.keys(),dict_rule_properties,paths))
list(map(test,list(paths.keys()),dict_rule_properties,paths))
map(test,list(paths.keys()),dict_rule_properties,paths)
from joblib import Parallel,delayed
Parallel(n_jobs=-1)(delayed(test)(k,dict_rule_properties,paths) for k in paths.keys())
paths
dict_rule_properties
import itertools
itertools.tee(dict_rule_properties,n=len(dict_rule_properties))
itertools.tee(dict_rule_properties,len(dict_rule_properties))
list(itertools.tee(dict_rule_properties,len(dict_rule_properties)))
list(itertools.islice(dict_rule_properties,len(dict_rule_properties)))
dict_rule_properties
l
n
m
m="""[  ($2 == "-n") || ($2 == "--context_names") || ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-p") || ($2 == "--port") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-n" ) || ($2 == "--context_names" ) || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]"""
m=m.replace(' || ',',').replace('"',"'")
m
m=m.replace(')',"""\"""").replace('(',"""\"""")
m
m.tolist()
m.tolist
import ast
ast.literal_eval(m)
m=ast.literal_eval(m)
len(m)
len(set(m))
m=[e.strip() for e in m]
m
len(set(m))
len(m)

import re
r
r=re.compile('-{1}\w+\|-{2}[a-zA-Z_]+\){1}')
with open('adaptNew/adapt/fca/test_specconstruction.sh','r') as f:
   code=f.read()
with open('~/adaptNew/adapt/fca/test_specconstruction.sh','r') as f:
   code=f.read()
os

with open('test_specconstruction.sh','r') as f:
   code=f.read()
code
re.match(r,code)
re.search(r,code)
re.findall(r,code)
parse_opts=re.findall(r,code)
type(parse_opts)
len(pars_opts)*2
len(parse_opts)*2
m
[p.replace(')','').split('|') for p in parse_opts]
itertools
list(itertools.chain.from_iterable([p.replace(')','').split('|') for p in parse_opts]))
parse_opts=list(itertools.chain.from_iterable([p.replace(')','').split('|') for p in parse_opts]))
parse_opts
r2=re.compile("\$2\s+\={2}\s+\'-{1,2}[a-zA-Z_]+\'")
re.search(r2,m[0])
r2=re.compile("\$2\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'")
re.search(r2,m[0]).group('opt')
m2=[re.search(r2,e).group('opt') for e in m]
m2
set(parse_opts)-set(m2)
len(set(m2))
len(set(parse_opts))
set([x for x in m2 if m2.count(x) > 1])
code
code_list=code.split('\n')
code_list
r3=re.compile("\[{2}\s+(\$2\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'\s+\|{2})+\s+\]{2}")
re.search(r3,code)
r3=re.compile("\[{2}\s+((\|{2}\s*)*\$2\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'\s+\|{2})+\s+\]{2}")
re.search(r3,code)
r3=re.compile("\[{2}\s+((\|{2}\s*)*\$2\s+\={2}\s+\'(?P<opt>-{1,2}[a-zA-Z_]+)\'(\s*\|{2})*)+\s+\]{2}")
re.search(r3,code)
r2
code_line[16]
code_list[16]
r3=re.compile("""(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\")""")
re.search(r3,code_list[16])
r3=re.compile("""(\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\))""")
re.search(r3,code_list[16])
r3=re.compile("""(\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)\s*\|{2})""")
re.search(r3,code_list[16])
r3=re.compile("""(\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)""")
re.search(r3,code_list[16])
r3=re.compile("""((\|{2}\s*)*\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)""")
re.search(r3,code_list[16])
r3=re.compile("""((\|{2}\s*)*\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)*""")
re.search(r3,code_list[16])
r3=re.compile("""((\|{2}\s*)*\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)""")
re.search(r3,code_list[16])
r3=re.compile("""((\|{2}\s*)*\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)+""")
re.search(r3,code_list[16])
re.match(r3,code_list[16])
re.search(r3,code_list[16])
code_list[16]
r3=re.compile("""((\|{2}\s*)*\(\$2\s+\={2}\s+\"(?P<opt>-{1,2}[a-zA-Z_]+)\"\s*\)(\s*\|{2})*)+""")
re.search(r3,code_list[16])
re.findall(r3,code_list[16])
re.finditer(r3,code_list[16])
list(re.finditer(r3,code_list[16]))
re.search(r3,code_list[16])
code_list[16]
r3
r3=re.compile('((\\s*\|{2}\\s*)*\\(\\$2\\s+\\={2}\\s+"(?P<opt>-{1,2}[a-zA-Z_]+)"\\s*\\)(\\s*\\|{2}\s*)*)+')
re.search(r3,code_list[16])
print(re.search(r3,code_list[16]))
re.search(r3,code_list[16]).group()
v=re.search(r3,code_list[16])
print(v)
import readline
readline.write_history_file('shell_debug.py')
