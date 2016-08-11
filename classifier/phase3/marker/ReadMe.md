
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

example output
--------------

    hangang:~/TransparentComputing/malware/cross-platform/simple-apt/simple$  python cmake.py rebuild | tail -3 && cp -p bin/linux/release/x64/simple /tmp/
    [*] Deleting bin/linux/release/x64
    [*] Building linux/x64/release
    [*] Built 4 targets in 7 seconds
    hangang:~/TransparentComputing/malware/cross-platform/simple-apt/simple$  /tmp/simple 13.1.102.2 4000
    [*] Target: 13.1.102.2:4000
    [*] Initializing message subsystem
    [*] Initializing network
    [*] Initializing session
    [*] Starting session
    [*] Getting OS information...
    [*] Linux #1 SMP Debian 3.16.7-ckt20-1+deb8u4 (2016-02-29) (3.16.0-4-amd64)
    [*] Encoding message
    [*] Encoding new connection info message
    [*] Sending message of 80 bytes (1 chunks)
    [*] Sending data: 95 bytes
    [*] Sent chunk of 95 bytes
    [*] Sent total message of 80 bytes
    [*] Sent 80 bytes
    [*] Creating message loop thread...
    [*] Entering C&C message processing loop...
    [*] Starting message receive loop...
    [*] Exiting message receive loop
    [*] Session is not connected
    [*] Ending session
    [*] Cleaning up network
    hangang:~/TransparentComputing/malware/cross-platform/simple-apt/simple$  ls -l /tmp/*mark*
    -rw-r--r-- 1 jhanley staff 2 Aug 10 09:24 /tmp/tc-marker-001-begin.txt
    -rw-r--r-- 1 jhanley staff 2 Aug 10 09:24 /tmp/tc-marker-001-end.txt
    -rw-r--r-- 1 jhanley staff 2 Aug 10 09:24 /tmp/tc-marker-002-begin.txt
    -rw-r--r-- 1 jhanley staff 2 Aug 10 09:24 /tmp/tc-marker-002-end.txt
