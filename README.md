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
  -l  --lint     Check the given file for syntactic and type issues.
  -q  --quiet    Quiet linter warnings
  -v  --verbose  Verbose debugging messages
  -a  --ast      Produce a pretty-printed internal AST
  -g  --graph    Produce a dot file representing a graph of the conceptual model.
  -s  --stats    Print statistics.
  -t  --turtle   Produce a turtle RDF description of the graph.
```
