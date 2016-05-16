
phase 2 classifier
==================

The daemon is managed by supervisord.

    $ head -4 config/supervisord.conf
    [program:classifyd]
    command=classifier/phase2/classifyd.py
    autorestart=yes
    startsecs=20

Running `make` will run unit tests, and will then invoke the client to give the daemon something to do:

    $ make
    nosetests3 --with-doctest --doctest-tests phase2_test.py classify/*.py
    ..
    ----------------------------------------------------------------------
    Ran 2 tests in 0.677s

    OK
    flake8 *.py */[a-z]*.py


    Testing the classifier daemon...
    ./faux_seg.py
    Working to produce segments...
    reporting 0
    2016-05-16 02:14:12,653 INFO recvd msg: ConsumerRecord(topic='ac',
    partition=0, offset=8, key=None, value=b'\x00')
    Finished producing segments.
    reporting 1


Lessons learned
---------------
1. DB performance matters, both for rapid ingest and rapid query.
   A slow DB impacts developer productivity.
   The DB must reliably accept insert requests,
   without throwing random errors about locking and such.
2. Component visibility matters.
   It is hard to `tail -f` more than a few busy logs.
   The dashboard should display
   per-component heartbeat and throughput figures,
   updated multiple times (perhaps 60 times) per minute.
   Easily the most critical component is our (largely unindexed) DB.
3. Kafka works well.
   Delivery latency is often low, though first message after a pause 
   may experience some lag. 
   Adapt streaming components should pass Segment IDs around in phase3.
   Sending base node IDs via kafka is probably more overhead than we desire.
4. Kafka works too well, perhaps.
   It provides impressive separation between components,
   to the extent that they can't tell, and don't care,
   whether / when downstream components are running.
   There's no perceptible difference between an upstream
   component that is missing versus an idle one that is
   currently running but silent.
   Score a win.
