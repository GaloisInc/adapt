import input_processing as ip
import argparse

parser=argparse.ArgumentParser(description='script to test basic query ability')
parser.add_argument('--port','-p',help='Query port. Default: 8080', type=int, default=8080)

query1='g.V().count()'
query2='g.E().count()'

if __name__ == '__main__':
	#retrieving the arguments from the command line
	args=parser.parse_args()
	port_val=args.port
	res1=ip.getQuery(query1,port=port_val)
	res2=ip.getQuery(query2,port=port_val)

	print('query1', res1)
	print('query2', res2)


