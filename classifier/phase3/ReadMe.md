
Phase3 Classifier
=================

The Ac component fits between Se & Dx:

    TA1(kafka) -> In -> Se -> Ac -> Dx

Like In & Se before it, Ac is a streaming component.


technical learnings
-------------------

Several lessons were learned during development.

1. TA1 event producers may emit poorly formed event streams, with
   out-of-sequence or missing base nodes. The In component has to deal with
   requests to add `Edge(v1, v2)`, where one or both vertices have not yet
   been seen. We poorly understand this phenomenon, and how it impacts the
   relationship between HappensBefore and monotonically increasing DB
   id's. (Assume for the sake of argument that `v1` has already been seen.)
   It may be due to:
    - `v2` will be introduced within the next `K` events (and TA1 intended this behavior, which seems a Bad Thing).
    - `v2` will be introduced a very very long time in the future, say after a million more nodes, so a streaming component should likely discard the edge, or maybe stub `v2`.
    - `v2` was lost, and will never be seen again, in which case the edge should likely be discarded.
2. Queries that are non-deterministic should be frowned upon; `order()` & `sort()` can help.
3. Naïve DB commands will yield a gremlin throughput of only 40 inserts per second.
4. It is necessary for Se to efficiently query unseen (unprocessed) nodes, so indices matter. Ingestd could help, by transmitting query arguments via kafka.
5. Similarly Ac queries unseen nodes, and Se could help by sending query args over kafka.
6. Multi-hour segments are probably out of scope for ADAPT - segments
   should have bounded size so streaming components don't over-malloc.
   Multi-hour aggregates can be computed by AD where needed.
7. The classifier could deal with CDM13 enums better, e.g. `source:[12], eventType:[21], subjectType:[4]`.
8. Ingestd apparently tacks on an unhelpful empty properties map to the properties attribute: `==>[sequence:[4235], startedAtTime:[45620160-08-21 04:50:00 UTC], ident:[nh611EQB5OiqpC5LBE0/aw==], source:[12], eventType:[12], subjectType:[4], properties:['[]']]
`
9. FiveDirection URLs should be simple utf8, with no backslash escaping, e.g. an URL
   of `C:\\Users\\Sarah\\AppData` contains three characters too many.
10. Kafka messages should probably bear app-level timestamps, to
    facilitate the edit / debug cycle, as stale messages tend to
    accumulate and clients can't tell how old a DONE message is.
11. Gremlin could be more robust, e.g. python clients occasionally get
   a "serialization error" instead of a usable client connection, and
   the supplied frontend sometimes reports "true" instead of the
   desired result set:

`$ gremlin.sh`

    gremlin> graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();
    ==>graphtraversalsource[standardtitangraph[cassandra:[localhost]], standard]
    gremlin>
    gremlin> g.V().has('ident', 'segment_id_007492263').outE('segment:includes').inV()
    ==>true
    gremlin>
    gremlin> g.V().has('ident', 'segment_id_007492263').outE('segment:includes').inV()
    ==>v[3522752]
    ==>v[3526672]
    ==>v[3526704]
    ==>v[3526752]
    ==>v[3526768]
    ==>v[3526808]
    ==>v[3526832]
    ==>v[3526848]
    ==>v[3526880]
    ==>v[3530768]


execution
---------

To classify nodes, use `make`:

    trantor:~/adapt$  time make clean all
    bash -c 'rm -f /tmp/{trace_loaded,segmented,classified}.txt'
    pgrep supervisord > /dev/null
    killall ingestd; sleep 1  # Background writers can interfere with drop.
    ~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
    g.E().drop().iterate()   [Message(status_code=204, data=None, message='', metadata={})]
    graph.tx().commit()      [Message(status_code=200, data=[None], message='', metadata={})]
    g.V().drop().iterate()   [Message(status_code=204, data=None, message='', metadata={})]
    graph.tx().commit()      [Message(status_code=200, data=[None], message='', metadata={})]
    Trint -p /tilde/jhanley/adapt/trace/current/5d_youtube_ie_output.bin | tee /tmp/trace_loaded.txt
    Sent 11106 statements to kafka[TName {_tName = KString {_kString = "ta2"}}].
    time ~/adapt/tools/await_completion.py se
    2016-06-29 17:25:23,907 INFO Waiting for se kafka message...
    2016-06-29 17:28:43,446 INFO recvd msg: ConsumerRecord(topic='se', partition=0, offset=7, key=None, value=b'\x01')
    1.18user 0.07system 3:20.00elapsed 0%CPU (0avgtext+0avgdata 14060maxresident)k
    8inputs+16outputs (0major+3957minor)pagefaults 0swaps
    time ~/adapt/tools/label_histogram.py
         9  Agent
         9  EDGE_SUBJECT_HASLOCALPRINCIPAL
       170  EDGE_EVENT_AFFECTS_NETFLOW
       170  Entity-NetFlow
       486  EDGE_OBJECT_PREV_VERSION
      1006  EDGE_EVENT_AFFECTS_FILE
      1268  Entity-File
      1514  EDGE_FILE_AFFECTS_EVENT
      2973  EDGE_EVENT_ISGENERATEDBY_SUBJECT
      2982  Subject
     10587  total
    0.30user 0.13system 0:05.23elapsed 8%CPU (0avgtext+0avgdata 18832maxresident)k
    8inputs+8outputs (0major+8612minor)pagefaults 0swaps
    ./query.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" | sort -nk2 | awk '$2 >= 10'
      80   10  AS29990  ASN-APPNEXUS - AppNexus, Inc, US
      90   12  AS8075  MICROSOFT-CORP-MSN-AS-BLOCK - Microsoft Corporation, US
      17   14  173.194.121.34
      74   90  AS15169  GOOGLE - Google Inc., US
    ~/adapt/segment/segmenter/simple_segmenter.py --k 3 --drop | time tee /tmp/segmented.txt
    2016-06-29 17:29:11,662 WARNING Dropping any existing segments.
    2016-06-29 17:29:46,522 INFO adding 250000
    2016-06-29 17:30:52,084 INFO adding 2470000
    2016-06-29 17:30:54,768 INFO adding 2560000
    2016-06-29 17:30:59,776 INFO adding 2810000
    2016-06-29 17:31:55,154 INFO adding 5030000
    2016-06-29 17:31:57,138 INFO adding 5120000
    2016-06-29 17:32:02,835 INFO adding 5370000
    2016-06-29 17:32:46,683 INFO adding 45900000
    2016-06-29 17:33:16,492 INFO adding 84050000
    2016-06-29 17:34:22,132 INFO Inserted 10587 edges.
    0.00user 0.00system 5:20.71elapsed 0%CPU (0avgtext+0avgdata 704maxresident)k
    56inputs+0outputs (1major+231minor)pagefaults 0swaps
    nosetests3 --with-doctest --doctest-tests test_precondition.py classify/*.py
    .
    ----------------------------------------------------------------------
    Ran 1 test in 18.977s

    OK
    ./fg_classifier.py
    nosetests3 --with-doctest --doctest-tests test_postcondition.py
    .
    ----------------------------------------------------------------------
    Ran 1 test in 6.861s

    OK
    touch /tmp/classified.txt
    26.17user 2.75system 12:19.07elapsed 3%CPU (0avgtext+0avgdata 22372maxresident)k
    75096inputs+344outputs (178major+79348minor)pagefaults 0swaps
