
    adapt/classifier/phase3/marker/

marker injection
================

We will focus on `ta5attack2` using PID-based segmentation,
injecting begin / end markers for grammar terminals into
the sequence of logged events:

1. preamble events
2. marker: begin 1
3. events of interest
4. marker: end 1
5. postamble events

Our sklearn.ensemble.RandomForestClassifier model will
identify segments with region 3 events of interest,
even after markers are removed.

During training we deliberately blind the learner
to attributes easily controlled by an attacker,
such as names of files outside of `/usr/bin`,
substituting a nonce value instead.

For marker number 1, the begin event shows up in
the trace as a write to `/tmp/tc-marker-001-begin.txt`.
This is followed by events of interest,
and then a write to `/tmp/tc-marker-001-end.txt`.
