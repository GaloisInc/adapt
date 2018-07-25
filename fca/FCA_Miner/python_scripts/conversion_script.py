import input_processing as ip
import argparse

parser=argparse.ArgumentParser(description='script to convert query specfication file to input CSV')
parser.add_argument('--port','-p',help='Query port. Default: 8080', type=int, default=8080)
parser.add_argument('--specdirectory','-d',help='Directory where query specification file are found. Default: ./fca/contextSpecFiles', nargs='+', default='./fca/contextSpecFiles')
parser.add_argument('--contextname','-n',help='Name of the context. Default: ProcessEvent',default='ProcessEvent')
parser.add_argument('--pathCSVContext','-cp',help='Path to the new CSV context. Default: specdirectory/contextname.csv',default='')


	
if __name__ == '__main__':
	#retrieving the arguments from the command line
	args=parser.parse_args()
	port_val=args.port
	contextdirectory=args.specdirectory
	contextname=args.contextname
	csv_path=args.pathCSVContext
	#print(contextdirectory)
	#print(contextname)
	ip.convert2InputCSV(contextname,contextdirectory,csvpath=csv_path,port=port_val)

