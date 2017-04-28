#! /usr/bin/env python3

import requests, readline, sys, json

def getQuery(query):
    try:
        resp = requests.post("http://localhost:8080/query/generic", data={"query": query}).json()
        return resp
    except Exception as e:
        print("There was an error processing your query:")
        print(e)

def getSingletonQuery(query):
    return getQuery(query)[0]


def getCountQuery(query):
    count = getSingletonQuery(query)
    return (int(count))

def getGroupCountQuery(query):
    dict = getSingletonQuery(query)
    return {k:int(dict[k]) for k in dict.keys()}


def printResp(resp):  
    if isinstance(resp,list):
        for line in resp:
            print(line)
    else:
        print(resp)
        

if __name__ == '__main__':
    print("Graph querying 0.1.")
    print("g.V().count()")
    printResp(getCountQuery("g.V().count()"))

