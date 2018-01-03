#! /usr/bin/env python3

import requests, readline, atexit, os, sys, json, subprocess, tempfile, argparse

def pager(text):
    tf = open(tempfile.mkstemp()[1], "w")
    tf.write(text)
    tf.flush()
    subprocess.call(["less", "-FX", tf.name])
    tf.close()

if __name__ == '__main__':

    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Command line query')
    parser.add_argument('url', metavar='URL', type=str, nargs='?',
                        default='http://localhost:8080')
    parser.add_argument('--endpoint', metavar='ENDPOINT', type=str, nargs='?',
                        default='json')
    args = parser.parse_args()

    # Compute post url
    post_url = args.url + "/query/" + args.endpoint
    print("Welcome to the query shell.\n"
          "Queries are sent to '{}'.".format(post_url))

    # Load history file
    histfile = "cmdline_query.history"
    if os.access(histfile, os.R_OK):
        readline.read_history_file(histfile)
    atexit.register(readline.write_history_file, histfile)

    # REPL
    while True:
        try:
            query = input("==> ")
            try:
                req = requests.post(post_url, data={"query": query}).json()
                pager(json.dumps(req, indent=4))
            except Exception as e:
                print("There was an error processing your query:\n"
                      "{}".format(e))
        except KeyboardInterrupt:
            print("\ninterrupt: input discarded")
        except EOFError:
            print("\nexit")
            sys.exit(0)
