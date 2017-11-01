#! /usr/bin/env python3

import requests, readline, atexit, os, sys, json, subprocess, tempfile

def pager(text):
    tf = open(tempfile.mkstemp()[1], "w")
    tf.write(text)
    tf.flush()
    subprocess.call(["less", "-FX", tf.name])
    tf.close()

if __name__ == '__main__':
    url = "http://localhost:8080"
    if len(sys.argv) >= 2:
    	url = sys.argv[1]
    print("Welcome to the query shell.\n"
          "Queries are sent to '{}/query/json'.".format(url))
    histfile = "cmdline_query.history"
    if os.access(histfile, os.R_OK):
        readline.read_history_file(histfile)
    atexit.register(readline.write_history_file, histfile)
    while True:
        try:
            query = input("==> ")
            try:
                req = requests.post(url + "/query/json",
                                    data={"query": query}).json()
                pager(json.dumps(req, indent=4))
            except Exception as e:
                print("There was an error processing your query:\n"
                      "{}".format(e))
        except KeyboardInterrupt:
            print("\ninterrupt: input discarded")
        except EOFError:
            print("\nexit")
            sys.exit(0)
