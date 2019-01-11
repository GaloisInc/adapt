import argparse
import csv
import os
import random
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

parser = argparse.ArgumentParser(description='Krimp/OC3')
parser.add_argument('--input', '-i',
					help='input file', required=True)
parser.add_argument('--output', '-o',
					help='output file', required=True)


def merge(input,context,output):
	with open(input) as score_file:
		score_reader = csv.reader(score_file)
		header = next(score_reader)[1:]
		scores = [float(row[1])
					for row in score_reader]

	with open(context) as context_file, open(output,'w') as out_file:
		context_reader = csv.reader(context_file)
		idx = 0
		header = next(context_reader)[1:]
		out_file.write("Object_ID, OC3 Score\n")
		for row in context_reader:
			out_file.write("%s,%f\n" % (row[0],scores[idx]))
			idx = idx+1

if __name__ == '__main__':
	args = parser.parse_args()

	prefix = '/tmp/krimp%s' % random.randint(1,10000000)
	krimpbin = os.path.abspath(os.path.join(os.path.dirname(__file__), 'bin/krimp'))
	mergepy = os.path.abspath(os.path.join(os.path.dirname(__file__), 'merge.py'))
	
	dat_file = prefix+'/data/datasets/data.dat'

	# todo: create a uniquely named directory so that we can run
	# multiple jobs concurrently without interference
	for d in [prefix,prefix+'/data',prefix+'/data/datasets']:
		os.mkdir(d)

	# inlined from convert.py
	with open(args.input) as csv_file, open(dat_file,'w') as out_file:
		reader = csv.reader(csv_file)
		header = next(reader)[1:]
		for row in reader:
			items = [str(i+1) for i,x in enumerate(row[1:]) if int(x) == 1]
			out_file.write("%s\n" % (" ".join(items)))

	with open(prefix+'/convert.conf','w') as convert_file:
		convert_file.write("""taskclass = datatrans
command = convertdb
takeItEasy = 0
dataDir = %s/data/
expDir = %s/xps/
dbName = data
dbInEncoding = fimi
dbInExt = dat
dbOutExt = db
dbOutEncoding = fic
dbOutTranslateFw = true
dbOutOrderInTrans = true
dbOutBinned = false""" % (prefix,prefix))

	with open('%s/compress.conf' % prefix,'w') as convert_file:
		convert_file.write("""algo = coverpartial
command = compress
datadir = %s/data/
expdir = %s/xps/
internalmineto = memory
iscchunktype = bisc
iscifmined = zap
iscname = data-closed-1d
iscstoretype = bisc
maxmemusage = 1024
numthreads = 1
prunestrategy = pop
reportacc = false
reportcnd = 0
reportsup = 100
takeiteasy = 0
taskclass = main""" % (prefix,prefix))

	os.system('%s %s/convert.conf' % (krimpbin,prefix))
	os.system('%s %s/compress.conf' % (krimpbin,prefix))

	#slight hack, should find the generated file and do merge directly
	os.system('cp %s/xps/compress/*/el*.csv %s/scores.csv' % (prefix,prefix))
	merge(prefix+'/scores.csv',args.input,args.output)
	shutil.rmtree(prefix)
