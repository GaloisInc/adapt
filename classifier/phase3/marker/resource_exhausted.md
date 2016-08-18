
too many open files
===================

Twenty open files is OK; 1024 is not.

Normal `lsof`-reported resource consumption for In:

    ...
    ingestd 15137 jhanley   11r     FIFO    0,10      0t0  1595375 pipe
    ingestd 15137 jhanley   12w     FIFO    0,10      0t0  1595375 pipe
    ingestd 15137 jhanley   13u  a_inode    0,11        0    10239 [eventfd]
    ingestd 15137 jhanley   14u     IPv4 1623768      0t0      TCP localhost:49194->localhost:9092 (ESTABLISHED)
    ingestd 15137 jhanley   16u     IPv4 1623765      0t0      TCP localhost:46370->localhost:8182 (ESTABLISHED)
    ingestd 15137 jhanley   17u     IPv4 1622853      0t0      TCP localhost:49196->localhost:9092 (ESTABLISHED)
    ingestd 15137 jhanley   18u     IPv4 1621212      0t0      TCP galaxy02.parc.xerox.com:46466->galaxy02.parc.xerox.com:9092 (ESTABLISHED)
    ingestd 15137 jhanley   19u     IPv4 1622855      0t0      TCP galaxy02.parc.xerox.com:46468->galaxy02.parc.xerox.com:9092 (ESTABLISHED)


Abnormal consumption:

    ...
    ingestd 15614 jhanley   11r     FIFO    0,10      0t0  1800596 pipe
    ingestd 15614 jhanley   12w     FIFO    0,10      0t0  1800596 pipe
    ingestd 15614 jhanley   13u  a_inode    0,11        0    10239 [eventfd]
    ingestd 15614 jhanley   14u     IPv4 1798564      0t0      TCP localhost:47205->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley   15u     IPv4 1800606      0t0      TCP localhost:47193->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley   16u     IPv4 1804417      0t0      TCP localhost:47183->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley   17u     IPv4 1799533      0t0      TCP localhost:47207->localhost:8182 (ESTABLISHED)
    ...
    ingestd 15614 jhanley 1020u     IPv4 1806087      0t0      TCP localhost:53274->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1021u     IPv4 1806090      0t0      TCP localhost:53280->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1022u     IPv4 1813775      0t0      TCP localhost:53286->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1023u     sock     0,8      0t0  1918761 protocol: TCP

Some time later:

    ...
    ingestd 15614 jhanley 1020u     IPv4 1806087      0t0      TCP localhost:53274->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1021u     IPv4 1806090      0t0      TCP localhost:53280->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1022u     IPv4 1813775      0t0      TCP localhost:53286->localhost:8182 (ESTABLISHED)
    ingestd 15614 jhanley 1023u     IPv4 1982574      0t0      TCP localhost:53794->localhost:9092 (ESTABLISHED)

A `killall ingestd` can put things back on track, as long as kafka remains stable and available.
