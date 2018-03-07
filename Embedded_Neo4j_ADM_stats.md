# Ingesting ADM into Neo4j external

I'm doing this with ADM because ADM has some fun guarantees: nothing is ever referred to
before it is defined. That makes it easy to slice transactions of any size you want. The
transactions are easy to make - just a bunch of `MATCH`s to bring into scope vertices not
already in scope, and a bunch of `CREATE`s to create new nodes and edges.

I had two obvious knobs to turn: transaction size and session size. A session is (I think)
the unit of what gets sent to the DB over the wire. Basically, you can have lots of
transactions per session.

The following chart explores both dimensions. I let ingestion run for 1 minute in each case
before killing it. I then looked at the final average ingestion speed (at the command line)
as well as the number of nodes and edges Neo4j reported having.

Ingested after 1 min:

Total in session:       |   100          500         1000         10000        50000
Total in transaction:   |
                        |
------------------------|----------------------------------------------------------------
                        |
10                      |  212/s        256/s       313/s        1324/s       2404/s
                        |  7.0K nodes   7.0K nodes  6.6K nodes   6.5K nodes   5.0K nodes
                        |  19.4K edges  19.4 edges  18.3K edges  18.3K edges  13.7K edges
                        |
100                     |               211/s       370/s        1868/s       2807/s
                        |               2.3K nodes  6.3K nodes   7.5K nodes   6.7K nodes
                        |               6.0K edges  17.6K edges  20.8K edges  18.4K edges
                        |
1000                    |                           293/s        1537/s       2256/s
                        |                           5.6K nodes   4.3K nodes   4.4K nodes
                        |                           15.3K edges  12.2K edges  12.3K edges
                        |
10000                   |                                        1019/s       1570/s
                        |                                        2.9K nodes   1.9K nodes
                        |                                        7.8K edges   4.9K edges


I then zoomed in on the 100/transaction 10000/session sweet spot. Manual gradient descent
is fun. I also increased the time to let ingest run:

Ingested after 2 minutes


Total in session:       |   5000         10000        15000
Total in transaction:   |
                        |
------------------------|-----------------------------------------
                        |
10                      |   453/s        725/s        882/s
                        |   12588 nodes  11875 nodes  12588 nodes
                        |   35746 edges  33638 edges  35740 edges
                        |
50                      |                             1022/s
                        |                             18329 nodes
                        |                             53055 edges
                        |
100                     |                             1047/s
                        |                             19940 nodes
                        |                             57719 edges
                        |
150                     |                750/s        890/s
                        |                17706 nodes  17907 nodes
                        |                50964 edges  51676 edges


Finally, I decided to see how 15000/100 (the best combination) fared as ingest went on for longer.
After the command line claimed to have ingested 161K ADM, I interrupted ingestion. I did this for
embedded Neo4j too:

                         |  external    embedded
                         |
-------------------------|-----------------------------------------
ADM ingested (cmd line)  |  161K        161K
rate (cmd line)          |  519/s       3594/s
time (total)             |  5.5m        1m
nodes written in DB      |  54K         3012K
edges written in DB      |  158K        5624K

# Observations

External does a ton of buffering somewhere: ingest rates are crazy high for a bit, then tank.
The larger the session/transactions, the more buffering there appears to be. Also, look at the
discrepancy between what was actually written the the DB: either Neo4j has a long pipeline, or
it is just plain slow. The average nodes written per second comes down to 161.
