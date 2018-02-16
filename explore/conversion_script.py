import input_processing as ip
import os,re,sys
import glob
import argparse

parser=argparse.ArgumentParser(description='script to convert query specfication file to input CSV')
parser.add_argument('--port','-p',help='Query port. Default: 8080', type=int, default=8080)
parser.add_argument('--specdirectory','-s',help='Directory where query specification file are found. Default: ./explore/contextSpecFiles', default='./explore/contextSpecFiles')
parser.add_argument('--contextname','-c',help='Name of the context. Default: ProcessEvent',default='ProcessEvent')

def convert2InputCSV(contextName,contextdirectory,port=8080):
	port_val=port
	#get file from context directory where filename contains contextname
	files=glob.glob(os.path.join(contextdirectory,'*.json'))
	r=re.compile('(\w|\W)+_(?P<contextname>\w+)\.json')
	contextFile=list(filter(lambda x: re.match(r,x).group('contextname').lower()==contextName.lower(),files))[0]
	#generate input CSV
	outputfile=os.path.splitext(contextFile)[0]+'.csv'
	ip.convertSpec2File(contextFile,outputfile,csv_flag=False,port=port_val)
	return 'Conversion successful. File '+outputfile+' created.'
	
	
if __name__ == '__main__':
	#retrieving the arguments from the command line
	args=parser.parse_args()
	port_val=args.port
	contextdirectory=args.specdirectory
	contextname=args.contextname
	print(contextdirectory)
	print(contextname)
	convert2InputCSV(contextname,contextdirectory,port=port_val)

