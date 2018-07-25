import tests
import argparse

parser=argparse.ArgumentParser(description='script to test that two input CSV are the same')
parser.add_argument('--file1','-f1',help='file to be compared')
parser.add_argument('--file2','-f2',help='reference file')





if __name__ == '__main__':
	#retrieving the arguments from the command line
	args=parser.parse_args()
	f1=args.file1
	f2=args.file2
	tests.test_inputfiles(fname1,fname2)

		
	
	
	

	
