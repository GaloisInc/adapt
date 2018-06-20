#! /usr/bin/env python3

import requests, os, sys, json, subprocess, tempfile, argparse, webbrowser

def pager(text):
    tf = open(tempfile.mkstemp()[1], "w")
    tf.write(text)
    tf.flush()
    subprocess.call(["less", "-FX", tf.name])
    tf.close()


def main():

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


def query(post_url):
    try:
        req = requests.post(post_url, data={"query": query}).json()
        (json.dumps(req, indent=4))
    except Exception as e:
        sys.stderr.write("There was an error processing your query:\n"
                     "{}\n".format(e))


def repl():
    # REPL
    while True:
        try:
            query = input("Enter vertex label==> ")
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


def search(graph_element):
    s

if __name__ == '__main__':
    main()
