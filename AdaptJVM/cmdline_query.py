#! /usr/bin/env python3

import requests, readline, sys

if __name__ == '__main__':
    url = "http://0.0.0.0:8080"
    if len(sys.argv) >= 2:
        url = sys.argv[1]
    print("Welcome to the query shell. Queries are sent to '" + url +  "/query/generic'.")
    while True:
        query = input("==> ")
        try:
            req = requests.post(url + "/query/generic", data={"query": query}).json()
            if isinstance(req,list):
                for line in req:
                    print(line)
            else:
                print(req)
        except Exception as e:
            print("There was an error processing your query:")
            print(e)

