#! /usr/bin/env python3

import requests, readline, atexit, os, sys, json

if __name__ == '__main__':
    url = "http://localhost:8080"
    if len(sys.argv) >= 2:
    	url = sys.argv[1]
    print("Welcome to the query shell.\n"
          "Queries are sent to '{}/query/generic'.".format(url))
    histfile = "cmdline_query.history"
    if os.access(histfile, os.R_OK):
        readline.read_history_file(histfile)
    atexit.register(readline.write_history_file, histfile)
    while True:
        try:
            query = input("==> ")
            try:
                req = requests.post(url + "/query/generic",
                                    data={"query": query}).json()
                if isinstance(req,list):
                    count = len(list)
                    for item, line in enumerate(req):
                        print("Result {} of {}".format(item+1, count))
                        print(json.dumps(json.loads(line), indent=4))
                else:
                    print(json.dumps(json.loads(req), indent=4))
            except Exception as e:
                print("There was an error processing your query:\n"
                      "{}".format(e))
        except KeyboardInterrupt:
            print("\ninterrupt: input discarded")
        except EOFError:
            print("\nexit")
            sys.exit(0)
