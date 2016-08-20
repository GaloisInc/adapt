
Ac annotations
--------------

![](figures/node_relationships.png)

These gremlin queries demonstrate Ac annotations to the DB.

Show all classification nodes with their attributes:

    gremlin> g.V().hasLabel('Activity').limit(2).valueMap(true)
    ==>[activity:type:[marker_events_end], activity:suspicionScore:[0.1], id:8626280, label:Activity]
    ==>[activity:type:[marker_events_begin], activity:suspicionScore:[0.1], id:17256512, label:Activity]


Classification counts per base node type:

    gremlin> g.V().hasLabel('Activity').outE('activity:includes').inV().groupCount().by(label())
    ==>[Entity-File:377]

Selected paths from a recent segment to classified base nodes:

    gremlin> g.V(8589400).hasLabel('Segment').
        out().hasLabel('Activity').
        out().hasLabel('Entity-File').limit(3).path()
    ==>[v[8589400], v[8487040], v[475200]]
    ==>[v[8589400], v[8491136], v[118816]]
    ==>[v[8589400], v[8495232], v[221288]]

Such paths may be fed to `marker/path_expander.py`.


execution
---------

To execute the the phase3 classifier on the imagegrab trace, use `make`:

    $ cd ~/adapt/classifier/phase3/out
    $ rm -f imagegrab.bin.txt
    $ make

Sample output, for simple_with_marker_3.avro:

    2016-08-19 19:45:54,286 INFO Fetched 1319 base nodes.
    2016-08-19 19:45:54,286 INFO Inserted 377 activity classifications.

Pipeline components after In are run in the foreground, so they do not appear in the local supervisord.conf.

Several [detectors](https://github.com/GaloisInc/adapt/blob/ac-dev/classifier/phase3/classify/activity_classifier.py#L46-L50)
run during classification of each segment:

- AcrossFirewallDetector
- MarkerDetector
- ScanDetector
- SensitiveFileDetector
- UnusualFileAccessDetector

To instrument Kudu's `simple` APT application, use the supplied script:

    $ cd ~/adapt/classifier/phase3/marker
    $ ./build_instrumented_simple_apt.sh

This yields an instrumented app, `/tmp/simple`, which TA1 can run
to produce a trace containing numbered begin / end markers.


events
------

![](figures/event.png)

The classifier needs several recorded event attributes, including:

- pid
- userID
- url
- startedAtTime (low resolution, many "simultaneous" nodes)
- sequence (essential for Happens-Before)

These are available on only a small subset of base nodes.
To bring them together
Ac [queries](https://github.com/GaloisInc/adapt/blob/2ffa1/tools/gremlin_event/stream.py#L88-L95)
for an ordered sequence of events matching
the Seg -> Subj -> EDGE_foo -> ESA pattern using this:

    g.V(S).hasLabel('Segment').out().hasLabel('Subject').out().out().
    has(label, within('Entity-File', 'Entity-NetFlow', 'Entity-Memory',
                      'Subject', 'Agent'))

Then the `sequence` attribute can be used to recover the original Happens-Before relationship.


TA1 sources
-----------

During the September engagement ADAPT will accept event streams from the following TA1's:

1. SRI (`ta5attack2`, `simple_with_marker_3`)
2. 5D (`imagegrab`)
3. CADETS (`remove_file`)


edge types
----------

An EDGE_foo will be one of
the [CDM13](https://git.tc.bbn.com/bbn/ta3-serialization-schema/blob/8eda8/avro/CDM13.avdl#L274)
EdgeTypes:

1.  EDGE_EVENT_AFFECTS_FILE
2.  EDGE_EVENT_AFFECTS_MEMORY
3.  EDGE_EVENT_AFFECTS_NETFLOW
4.  EDGE_EVENT_AFFECTS_REGISTRYKEY
5.  EDGE_EVENT_AFFECTS_SRCSINK
6.  EDGE_EVENT_AFFECTS_SUBJECT
7.  EDGE_EVENT_CAUSES_EVENT
8.  EDGE_EVENT_HASPARENT_EVENT
9.  EDGE_EVENT_HAS_TAG
10. EDGE_EVENT_ISGENERATEDBY_SUBJECT
11. EDGE_FILE_AFFECTS_EVENT
12. EDGE_FILE_HAS_TAG
13. EDGE_MEMORY_AFFECTS_EVENT
14. EDGE_MEMORY_HAS_TAG
15. EDGE_NETFLOW_AFFECTS_EVENT
16. EDGE_NETFLOW_HAS_TAG
17. EDGE_OBJECT_PREV_VERSION
18. EDGE_REGISTRYKEY_AFFECTS_EVENT
19. EDGE_REGISTRYKEY_HAS_TAG
20. EDGE_SRCSINK_AFFECTS_EVENT
21. EDGE_SRCSINK_HAS_TAG
22. EDGE_SUBJECT_AFFECTS_EVENT
23. EDGE_SUBJECT_HASLOCALPRINCIPAL
24. EDGE_SUBJECT_HASPARENT_SUBJECT
25. EDGE_SUBJECT_HAS_TAG
26. EDGE_SUBJECT_RUNSON
