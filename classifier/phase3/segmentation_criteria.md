
segmentation criteria
=====================

> 1. Segment by PID
>
>     - What is the exact criteria?

Recall that Ac is a streaming component.
The TA1 event stream is under control of the adversary, of the APT.
If altering the TA1 event stream can cause a streaming component
to consume an arbitrarily large amount of RAM,
or to consume arbitrarily many CPU cycles per base node,
then the streaming component has a code defect.
We strive to make Ac behave correctly.

In the forensic setting,
the Ac component will not consume arbitrarily large segments.
It will append `.limit(1024)` to queries,
so for segments with much more than a thousand nodes
a portion of the event stream shall be ignored.
Some processes generate more than a thousand events using same PID.
In such cases it is recommended that Se emits multiple segments for a
given PID's activity.
(A constant like 1e4 would also be fine, but 1e9 in contrast would Be Bad.)

In the online setting,
the Se component cannot wait until the end of time before emitting
buffered events from a given immortal PID, since for some processes it
shall *never* be the case that we see a call to `exit()`.
This motivates emitting multiple segments for a given PID's activity,
based on some configurable parameter like elapsed time or,
preferably, number of events, or even both, which would bound
the latency from time-event-happens to time-event-is-reported.

We interpret "Segmentation by PID" as "Bounded Segmentation by PID".
The simplest exact criteria would be sets of base nodes with identical PID
and size of set < K.
A fancier criterion would be sets of base nodes with identical PID,
each set size being no more than K,
and difference between last and first timestamp no more than T.

Events within an event stream are reported serially;
each one has a HappensBefore relationship with its successor.
Multiple concurrent event streams will occasionally synchronize with
one another, but the HappensBefore relationships won't be as straightforward.
For segments S(n) and S(n+1), it would be desirable for each of
the base nodes in S(n) to have a HappensBefore relationship
with each of the base nodes in S(n+1).
