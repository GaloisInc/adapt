
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

    a. `v2` will be introduced within the next `K` events (and TA1 intended this behavior, which seems a Bad Thing).
    b. `v2` will be introduced a very very long time in the future, say after a million more nodes, so a streaming component should likely discard the edge, or maybe stub `v2`.
    c. `v2` was lost, and will never be seen again, in which case the edge should likely be discarded.

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

    trantor:~/Documents/Adapt-classify/phase3$  make clean all
    bash -c 'rm -f /tmp/{trace_loaded,segmented,classified}.txt'
    pgrep supervisord > /dev/null
    ~/adapt/trace/trace_rsync.sh
    + cd /tilde/jhanley/adapt/trace
    + rsync -av --exclude '*ttl.txt' --exclude '*.dot' --exclude '*.dot.gz' --exclude '*.json.CDM.json' --exclude '*.log' --exclude '*.log.gz' --exclude '*.provn' --exclude '*.provn.gz' --exclude '*.provn.ttl' --exclude '*.raw' --exclude '*.raw.pp_json' --exclude '*.tar.gz' --exclude '*.tgz' --exclude '*.ttl' --exclude '*.zip' adapt@seaside.galois.com:trace/ .
    receiving incremental file list
    2016-06-22/
    2016-06-22/5d_youtube_ie_output.bin
    current/
    current/5d_youtube_ie_output.bin -> ../2016-06-22/5d_youtube_ie_output.bin
    sent 274 bytes  received 639,858 bytes  256,052.80 bytes/sec
    total size is 567,762,861  speedup is 886.95
    killall ingestd; sleep 1  # Background writers can interfere with drop.
    /tilde/jhanley/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
    g.E().drop().iterate()   [Message(status_code=204, data=None, message='', metadata={})]
    graph.tx().commit()      [Message(status_code=200, data=[None], message='', metadata={})]
    g.V().drop().iterate()   [Message(status_code=204, data=None, message='', metadata={})]
    graph.tx().commit()      [Message(status_code=200, data=[None], message='', metadata={})]
    Trint -p /tilde/jhanley/adapt/trace/current/5d_youtube_ie_output.bin | tee /tmp/trace_loaded.txt
    Sent 11106 statements to kafka[TName {_tName = KString {_kString = "ta2"}}].
    time /tilde/jhanley/adapt/tools/await_completion.py se
    2016-06-28 14:17:28,048 INFO Waiting for se kafka message...
    2016-06-28 14:17:33,941 INFO recvd msg: ConsumerRecord(topic='se', partition=0, offset=1, key=None, value=b'\x01')
    0.24user 0.06system 0:06.92elapsed 4%CPU (0avgtext+0avgdata 13956maxresident)k
    3480inputs+0outputs (16major+3925minor)pagefaults 0swaps
    time ~/adapt/tools/label_histogram.py
         2  EDGE_SUBJECT_HASLOCALPRINCIPAL
         4  Agent
         6  EDGE_EVENT_AFFECTS_NETFLOW
        12  EDGE_OBJECT_PREV_VERSION
        22  Entity-NetFlow
        41  EDGE_EVENT_AFFECTS_FILE
        83  EDGE_FILE_AFFECTS_EVENT
       238  EDGE_EVENT_ISGENERATEDBY_SUBJECT
       303  Entity-File
       586  Subject
      1297  total
    0.27user 0.10system 0:05.59elapsed 6%CPU (0avgtext+0avgdata 17812maxresident)k
    0inputs+0outputs (0major+8360minor)pagefaults 0swaps
    ./query.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" | sort -nk2 | awk '$2 >= 10'
    /tilde/jhanley/adapt/segment/segmenter/simple_segmenter.py --k 3 --drop | time tee /tmp/segmented.txt
    2016-06-28 14:18:05,370 WARNING Dropping any existing segments.
    2016-06-28 14:19:13,946 INFO adding 67830000
    2016-06-28 14:19:18,465 INFO adding 68010000
    2016-06-28 14:20:22,709 INFO adding 70570000
    2016-06-28 14:21:48,195 INFO adding 135250000
    2016-06-28 14:22:15,507 INFO adding 137810000
    2016-06-28 14:23:06,232 INFO adding 142930000
    2016-06-28 14:23:07,542 INFO Inserted 9558 edges.
    15.65user 1.06system 5:19.87elapsed 5%CPU (0avgtext+0avgdata 21444maxresident)k
    13224inputs+0outputs (141major+10734minor)pagefaults 0swaps
    ...
    $  touch /tmp/trace_loaded.txt /tmp/segmented.txt && make
    pgrep supervisord > /dev/null
    nosetests3 --with-doctest --doctest-tests test_precondition.py classify/*.py
    .
    ----------------------------------------------------------------------
    Ran 1 test in 8.703s
    
    OK
    ./fg_classifier.py
    nosetests3 --with-doctest --doctest-tests test_postcondition.py
    .
    ----------------------------------------------------------------------
    Ran 1 test in 2.832s
    
    OK
    touch /tmp/classified.txt
    $
