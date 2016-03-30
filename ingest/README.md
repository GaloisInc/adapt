# ADAPT Ingest

The ingester library, and associated command line tool 'Trint', parses Prov-N
data in the form specified by <http://spade.csl.sri.com/rdf/audit-tc.rdfs#>.
This Prov-N is then translated to our CDM-inspired internal data format.  The
tool is able to produce AST print-outs of the CDM, upload CDM to gremlin-server
via websockets, compute statistics about the provenance, and syntax check the
input file to produce localized error messages.

# Local Installation

First install [stack](https://github.com/commercialhaskell/stack/releases) build
tool then run `make` from the top level.

# Usage

Trint command line is in the form "Trint [Flags] Parameters" where

```
Parameters:
  FILES    The Prov-N files to be scanned.

Flags:
  -l                 --lint                    Check the given file for syntactic and type issues.
  -q                 --quiet                   Quiet linter warnings
  -v                 --verbose                 Verbose debugging messages
  -a                 --ast                     Produce a pretty-printed internal CDM AST
  -s                 --stats                   Print statistics.
  -u[Database host]  --upload[=Database host]  Uploads the data by inserting it into a Titan database using gremlin.
  -h                 --help                    Prints this help message.
```

The 'upload' command has been tested with a gremlin server configured with the
default websockets channelizer.
