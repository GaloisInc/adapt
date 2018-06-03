import os,re,glob,sys
import input_processing as ip
import subprocess

def file_len(fname):
	p = subprocess.Popen(['wc', '-l', fname], stdout=subprocess.PIPE,stderr=subprocess.PIPE)
	result, err = p.communicate()
	if p.returncode != 0:
		raise IOError(err)
	return int(result.strip().split()[0])
	
def getFileExtension(fname):
	return  os.path.splitext(fname)[1]
	
	
def compareFiles(fname1,fname2):
	with open(fname1) as f1:
		lines1=set([s.rstrip() for s in f1.readlines()])
	with open(fname2) as f2:
		lines2=set([s.rstrip() for s in f2.readlines()])
	return lines1==lines2
	
def test_inputfiles(fname1,fname2):
	if getFileExtension(fname1)!='.csv':
		print('Test 1 FAILED.\nInput file '+fname1+" is not a CSV file.")
		return 1
	else:
		print('Test 1 PASSED.\nInput file '+fname1+" has the right extension.")
		if file_len(fname1)!=file_len(fname2):
			print('Test 2 FAILED.\nFiles '+fname1+' and '+fname2+" don't have the same number of lines.")
			return 1
		else:
			print('Test 2 PASSED.\nFiles '+fname1+' and '+fname2+" have the same number of lines.")
			if compareFiles(fname1,fname2)==False:
				print('Test 3 FAILED.\nFiles '+fname1+' and '+fname2+" don't have the same content.")
				return 1
			else:
				print('Test 3 PASSED.\nFiles '+fname1+' and '+fname2+" have the same content.\n ALL 3 TESTS PASSED.")
				return 0
