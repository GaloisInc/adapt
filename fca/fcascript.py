import argparse
import fca
import sys


#-------------------------------------------------------------------------------------------------------
#definition of command line arguments
#------------------------------------------------------------------------------------------------------------

parser=argparse.ArgumentParser(description='FCbO concept generation and analysis input arguments')
parser.add_argument('--workflow','-w',help='Specify whether to run only FCA, only the concept analysis or both', required=True, action='store', choices=['context','fca','analysis','both'])
#specifies whether to analyze the concepts after they have been computed. Doesn't make use of the saved concepts files for now.
#In an upcoming version, it will be possible to do the concepts computation and the analysis separately.
parser.add_argument('--fca_algo','-f',help='Specifies whether to run FCA from the python (FCbO) or the C (FCbO or PCbO) code ',action='store',choices=['python','C'],default='python')
parser.add_argument('--fcbo_path','-p',help='Specifies the location of the FCbO/PCbO C code',action='store',default='')
parser.add_argument('--inputfile','-i',help='Full path to FCA input file including extension (cxt or fimi or csv). If not given, --specfile/-s is required.',default='')
parser.add_argument('--specfile','-s',help="Context specification file (json format+contains 'spec' in filename) that generates FCA input context. If not given, --inputfile/-i is required",default='')
parser.add_argument('--queryres','-q',help="JSON file containing results of a query",default='')
#either an context input file (in cxt or fimi format) or a query specification file or both have to be provided. If only a context input file is provided, the context in the file is used as
#input to FCbO. If only a query specification file is provided, a query is sent to the database to generate a temporary context that is used as input to FCbO. If both a context input file
# and a query specification file are provided, a query is sent to the database to generate a context that is saved to the input file. The input file is then used as input to FCbO.
#Either an context input file (in fimi or cxt format) or a query specification file (in json format+contains 'spec' in filename) is required. Otherwise, an error is thrown.
parser.add_argument('--csv',help='Specifies whether the specification file gives details about the csv files to use to construct a context',action='store_true')
parser.add_argument('--parallel','-pl',help='specifies number of processors to use in PCbO and that PCbo should be used instead of FCbO',type=int,default=1)
parser.add_argument('--min_support','-m',help="Minimum support of the concepts to be computed (float required). Default:0",type=float,default=0)
#argument to specify the minimal support of the concepts to be returned by FCbO. A default of 0 is set, which means that all concepts are genrated by default.
parser.add_argument('--port',help="Query port. Default:8080",type=int,default=8080)
parser.add_argument('--disable_naming','-dn',help='Only applies when context is directly generated from json specification file or when context is a CXT file. Turns off naming of object and attribute entities in concepts. Objects and attributes only referred to by their position (index) in the context.',action='store_true')
#This argument cannot be invoked with an input FIMI file. It can be invoked with a json query specification file or an input file in CXT format. If invoked, the objects and attributes
#are identified by their positions in the context matrix (as is the case for FIMI files) and not by name.

parser.add_argument('--outputfile','-o',help='Full path to output file (saving concepts). If not specified, the concepts are printed on the screen.',default=sys.stdout)
#specifies where to save the FCbO concepts. By default, the concepts are just printed out on the screen

vals=['imp','anti','dis']
#parser.add_argument('--analysis_type','-a',help='specifies which type of analysis to perform',action='store',default='all',choices=['all']+list(','.join(s) for s in list(itertools.permutations(vals,1))+list(itertools.permutations(vals,2))+list(itertools.permutations(vals,3))))
parser.add_argument('--analysis_outputfile','-oa',help='Full path to output file (saving analysis). If not specified, the analysis results are printed on the screen.',default=sys.stdout)
#specifies where to save the analysis results. By default, the analysis results are just printed out on the screen

parser.add_argument('--concept_file','-cf',help='Full path to the file that contains the concepts to analyze',default='')

parser.add_argument('--rules_spec','-rs',help='JSON file that specifies which rules should be computed along with their computation parameters',action='store',default='./rulesSpecs/rules_implication_all.json')


#launches concept generation with FCbO then analysis
#this part will need to be factorized in a subsequent version

if __name__ == '__main__':
	#retrieving the arguments from the command line
	args=parser.parse_args()
	workflow=args.workflow
	port_val=args.port
	fcaAlgo=args.fca_algo
	fcboPath=args.fcbo_path
	csv_flag=args.csv
	type_json=('context' if csv_flag==False else 'csv')
	inputfile=args.inputfile
	specfile=args.specfile
	nbcpus=args.parallel
	parallel=(True if nbcpus>1 else False)
	queryres_file=args.queryres
	outputfile=(args.outputfile if args.outputfile!='' else sys.stdout)
	minsupport=args.min_support
	fca_algo=args.fca_algo
	if args.disable_naming:
		naming=args.disable_naming
	else:
		naming=False
	output_file_analysis=args.analysis_outputfile
	rulesSpec=args.rules_spec
	conceptfile=args.concept_file
	fca.fca_execution(inputfile,specfile,workflow,port=port_val,fca_algo=fcaAlgo,fcbo_path=fcboPath,csv=csv_flag,ncpus=nbcpus,min_support=minsupport,disable_naming=naming,queryres=queryres_file,output=outputfile,analysis_output=output_file_analysis,rules_spec=rulesSpec,concept_file=conceptfile)






