## Prerequisites 

- Python 2.7.x
- virtualenwrapper (http://virtualenvwrapper.readthedocs.org/en/latest/install.html)

## Installation 

    mkvirtualenv segmenter
    pip install -r requirements.txt

## Phase 1 segmenter

The phase 1 segmenter takes provenance input data (in a limited form
of PROV-N format) and a segmentation specification, and produces a collection
of segments (using PROV-N-like syntax).  Each segment is annotated
with information such as the PID and start time, according to the
segmentation specification.

The PROV-N input and output formats are somewhat "throwaway",
implemented only for human consumption since in phase 2 we will move
to reading from and writing to the database directly.  Internally, the
segmenter transforms the prov-n to graph data structures close to what
will be received from Titan.  The segmentation specification refers to
these internal edge names, so that when the base layer schema changes, we
only need to change the segmentation specification, not the Python
code.

Example:

    workon segmenter
    python provn_segmenter.py test/test_james.provn test/test_james_spec.json
	
or, to get the usage:

    python pred_segmenter.py --help

This example shows a simple scenario with activity segmented by day and PID.

Current limitations:

- we currently assume that all events are tagged with times.  Correct
  behavior in the absence of complete time annotations is undefined.
- we currently aren't producing edges between segments in the segment layer.
  There is nothing to stop us doing this, but the current approach we have
  played with seems to generate many spurious edges.  In any case, the edges
  can be defined later by queries using the segment inclusion edges and the raw graph.


## Older code (now superseded)


### Time-based segmenter

Example:

    workon segmenter
    python segmenter.py test/prov_out.ttl 694575727

or, to get the usage:

    workon segmenter
    python segmenter.py -h

After segmentation, you can do, for example:

    python segmented2dot.py test/prov_out.ttl

This will generate .dot files for all the segment.
You can the use zgrviewer (http://zvtm.sourceforge.net/zgrviewer.html)
to visualize them.

### Predicate segmenter

This module implements a simple predicate-based RDF segmenter.
The main program takes as inputs
- an RDF graph G in turtle format and
- a predicate name N,
- a predicate value V, and
- an integer radius R,
and produces an RDF file consisting of 
the subgraph of G containing all nodes at distance
R of nodes s such that (s, N, V) is in G.
    
	Example:
	
    workon segmenter
    python pred_segmenter.py test/bad-ls.provn.ttl  http://spade.csl.sri.com/rdf/audit-tc.rdfs#pid 3233 0 -v
    python pred_segmenter.py test/bad-ls.provn.ttl  http://spade.csl.sri.com/rdf/audit-tc.rdfs#pid 3233 1 -v

or, to get the usage:

    python pred_segmenter.py --help


## dot2viz 

dot2viz takes all dot files in a directory and converts to a given
format (pdf is the default). Use dot2viz -h for help.
