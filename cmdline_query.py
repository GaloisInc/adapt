#! /usr/bin/env python3

import requests, readline, atexit, os, sys, json, subprocess, tempfile, argparse, webbrowser

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
    parser.add_argument('--query', metavar='QUERY', type=str, nargs='?')
    args = parser.parse_args()

    # Compute post url
    post_url = args.url + "/query/" + args.endpoint
    sys.stderr.write("Queries are sent to '{}'.\n".format(post_url))

    # Check for the single query case
    if args.query:
        try:
            req = requests.post(post_url, data={"query": args.query}).json()
            json.dump(req, sys.stdout, indent=4)
            sys.exit(0)
        except Exception as e:
            sys.stderr.write("There was an error processing your query:\n"
                             "{}\n".format(e))
            sys.exit(1)

    # Load history file
    histfile = "cmdline_query.history"
    if os.access(histfile, os.R_OK):
        readline.read_history_file(histfile)
    atexit.register(readline.write_history_file, histfile)

    # REPL
    while True:
        try:
            query = input("==> ")
            if not query:
                continue
            elif query[-1] == '?':
                webbrowser.open_new_tab(args.url + "/#" + query[0:-1] + ":F00")
            else:
                try:
                    req = requests.post(post_url, data={"query": query}).json()
                    pager(json.dumps(req, indent=4))
                except Exception as e:
                    sys.stderr.write("There was an error processing your query:\n"
                                 "{}\n".format(e))
        except KeyboardInterrupt:
            sys.stderr.write("\ninterrupt: input discarded\n")
        except EOFError:
            sys.stderr.write("\nexit\n")
            sys.exit(0)
