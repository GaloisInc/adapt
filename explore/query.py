#! /usr/bin/env python3

import requests, readline, sys, json

def getQuery(query):
    try:
        resp = requests.post("http://localhost:8080/query/generic", data={"query": query},timeout=120)
        print('Response: '+ str(resp.status_code))
        return (resp.json())
    except Exception as e:
        print("There was an error processing your query:")
        print(e)

def the(result):
    return result[0]

def getCountQuery(query):
    counts = getQuery(query)
    return [int(count) for count in counts]

def getGroupCountQuery(q):
    result = getQuery(q)
    return [{k : int(dict[k]) for k in dict.keys()} for dict in result]


def printResp(resp):  
    if isinstance(resp,list):
        for line in resp:
            print(line)
    else:
        print(resp)
        

if __name__ == '__main__':
    print("Graph querying 0.1.")
    print("g.V().count()")
    printResp(the(getCountQuery("g.V().count()")))

