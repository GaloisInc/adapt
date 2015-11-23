# ADAPT Ingest

The ingester library, and associated command line tool 'Trint', parses Prov-N
data in the form specified by <http://spade.csl.sri.com/rdf/audit-tc.rdfs#>,
which should closely match [Ingest/Language.md]()  (modulo time lag in updating
to match agreed changes).

# Installation

First install [ghc](https://www.haskell.org/ghc/) and the
[stack](https://github.com/commercialhaskell/stack/releases) build tool then run
`make` from the top level.

# Usage

Trint command line is in the form "trint [Flags] Parameters" where

```
Parameters:
  FILES    The Prov-N files to be scanned.

Flags:
  -l                 --lint                    Check the given file for syntactic and type issues.
  -q                 --quiet                   Quiet linter warnings
  -v                 --verbose                 Verbose debugging messages
  -a                 --ast                     Produce a pretty-printed internal AST
  -g                 --graph                   Produce a dot file representing a graph of the conceptual model.
  -s                 --stats                   Print statistics.
  -t                 --turtle                  Produce a turtle RDF description of the graph.
  -u[Database host]  --upload[=Database host]  Uploads the data by inserting it into a Titan database using gremlin.
  -h                 --help                    Prints this help message.
```

For example (notice infoleak.provn is large and thus not in the database, see
seaside for example traces):

```
trint -s -l ./example/SRI/infoleak.provn
// No output because the example is syntactically and type correct.
trint -u ./example/SRI/infoleak.provn
// No output (currently, just a long wait time)
```

During execution of upload one can query the gremlin server using the same
restful interface as the trint tool.  For example, using curl to send a vertex
count request and extract the result from the returned json via the `jq` tool.

```
curl -s -X POST -d "{ \"gremlin\" : \"g.V().count()\" }" "http://localhost:8182"
| jq '.result.data'
[
  39693
]
```

The 'upload' command has been tested with a gremlin server configured
with the example 'rest-modern.yaml'.  Gremlin's restful interface is useful for
debugging and easy inspection, but is known to be inefficient - so we'll use
this interface only so long as the web sockets solution remains opaque.
