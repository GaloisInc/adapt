import json
import requests

class Database:
	def __init__(self,url='http://localhost',
				 port=8080):
		self.url = url
		self.port = port

	def getQuery(self,query,endpoint='json',timeout=300):
		try:
			resp = requests.post("%s:%d/query/%s"
								% (self.url,self.port,endpoint),
								data={"query": query},
								timeout=timeout)
			print('Response: '+ str(resp.status_code))
			return (resp.json())
		except Exception as e:
			print("There was an error processing your query:")
			print(e)
