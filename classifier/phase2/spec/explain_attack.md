
explain a known attack
----------------------

By hypothesis, an attack happened in segment N,
and we know N since Angelos gave it to us:

> "Given the ID of an event or file from a trace as a starting point,
> discover, display, and explain the attack-relevant portions of the
> provenance graph of that starting point. Then, discover display and
> explain other downstream system objects or events affected by
> elements of that graph."


First we'll need a gremlin query that maps the given base node (the "event")
to a segment ID, that's straightforward.

> inputs: base node ID, segmentation type (e.g. byHour, byDay, byUid)

> output: an attack segment ID

> visualization: display details, including the segment's min/max timestamp, set of UIDs, set of file names manipulated, set of classifications.


Next we'll need to extract an attack-related subgraph of limited size,
a set of segment IDs with edges connecting them.
This sounds like a task for Dx, but perhaps a gremlin query
could produce it, as well.

> input: the attack segment ID, and subgraph diameter K

> output: set of segment IDs (which shall include the given attack segment), along with edges.

> visualization: an SVG AT&T graphviz `dot` rendering. Also, in tabular form, display segment details as above.


How does this affect the Ac component? Well, if we already
exhaustively classified each segment of the original graph G as
the observations arrived over kafka, then we're done, titan already
contains the required segment details. OTOH, if G was enormous and
maybe we're lazily classifying or we fell far behind realtime, then
the attack subgraph of diameter K represents a prioritized work-list of segments
we ought to emit classifications for. So feed each segment ID to Ac
via kafka, and we'll emit "done with segment N" once classification
is complete.


Open question: we can keep incrementing K and computing larger
subgraphs, but how would we know when we're done, when we're not
uncovering any additional relevant segments?
