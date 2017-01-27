#! /usr/bin/env python3

import requests, readline, sys

if __name__ == '__main__':
    print("Welcome to the query shell. Queries are sent to 'http://localhost:8080/query/generic'.")
    while True:
        query = input("==> ")
        try:
            req = requests.post("http://localhost:8080/query/generic", data={"query": query}).json()
            if isinstance(req,list):
                for line in req:
                    print(line)
            else:
                print(req)
        except Exception as e:
            print("There was an error processing your query:")
            print(e)

