# Simple anomaly detectors

## Quick start

# Dependencies

```
pip install neo4j-driver pandas numpy matplotlib pyfpgrowth
```

# Running

Assuming an instance of the ADAPT ingester is running at
`http://localhost:8080`, the following command

```
make
```

should do the following things:
* Extract several process-centric contexts (ProcessEvent and several
others)
* Save the context CSV files in `contexts/`
* Run the AVF scoring algorithm on the contexts, producing score files
in `results/`

Additionally, doing

```
make avcbatch
```

should run the AVC algorithm on all of the process-centric contexts in
batch mode.  Additional make targets can be added to
run additional algorithms.  The `CONTEXTS` variable in the Makefile
can be edited to change which contexts are extracted.  The actual
context specifications are in the `specifications/` directory and can
be inspected there.

There are additional targets for streaming variants of the algorithms and
other experimental algorithms but these can be ignored for now.

The makefile does not currently run the tests or exercise the
`check.py` script or other scripts besides `extract.py` and `ad.py`.

Doing

```
make clean
```

will remove all score and ranking files from `results/` and `tests`.

Doing

```
make spotless
```

will do `make clean` and also remove all the context files from
`contexts/`.  This might be a good idea if we want to extract contexts
from a different database.

## Extracting from an arbitrary URL using the Makefile

If you want to extract from an ADAPT ingester at an endpoint other than 
`http://localhost:8080`, such as say `http://www.example.com:8081`, you can do
the following:

```
URL=http://www.example.com PORT=8081 make ...
```

Furthermore, to ingest from a standalone `neo4j` database or data for a specific 
provider, one can do:
```
URL=bolt://www.example.com PORT=7687 DATABASE=neo4j PROVIDER="-r cadets-e3" make
```

## Tests

There is a small set of test examples with a "ground truth" file in `test/`.  
From the `edi/` directory, running

```
python3 test.py
```

should run the tests and yield some results in the `test/` directory.  These 
tests use some artificial context CSV files that illustrate different kinds of 
"anomalies".  The tests currently exercise the AVF and AVC algorithms (`ad.py`) 
use the test  ground truth files to evaluate how good the resulting rankings are 
(`check.py`).  Other operations are not currently exercised by this test script.



## Extracting contexts

The script `extract.py` can be used to extract contexts from a running 
ingester/DB instance.  Usage is as follows:

```
extract.py [-d DATABASE] [-h] [-p PORT] [-u URL] [-r PROVIDER] -i INPUT
                 -o OUTPUT [-v]
```

By default, URL/port are `http://localhost:8080` so if an ingester instance is 
running at that location then just doing

```
python3 extract.py -i specifications/ProcessEvent.json -o contexts/ProcessEvent.csv
```

should work.  The argument is a context name which is used to find the context 
specification (`specifications/ProcessEvent.json`) and the result is placed in 
`contexts/ProcessEvent.csv`.

### Extracting from a standalone Neo4j database

If you have data hosted in a vanilla Neo4j standalone database at say 
`bolt://www.example.com:7687` then this is now possible using the 
`--database neo4j` flag.  Note that the database access uses the "bolt" 
protocol so the `--url` argument
should be something like `bolt://www.example.com` 
and the `--port` should be 7687.

### Extracting the data for a single provider, and avoiding port overload

Prior to E4, all of the data in a given database was from a single provider,
but post E4, a database can contain data from several providers, with the 
`provider` fields of some nodes used to distinguish the sources.  The 
`extract.py` and `times.py` scripts now take a `--provider` (or `-r`) parameter
to specify the provider, e.g. `-r cadets-e3`.  If this parameter is set, the
queries generated will have an appropriate `WHERE` clause to select only 
data from that provider, otherwise all data will be selected.  

The queries in `specifications` all have a `WHERE` clause with a "hole" (`%s`) 
which allows the extraction script to fill in an arbitrary predicate constraint.



## Joining contexts

The script `join.py` joins together two or more contexts (say, `left.csv` and 
`right.csv`) and produces a new
context CSV file (with uniquely renamed attributes).  Note that if
both contexts are sparse then the result can be much larger than the
two separate CSV files.

```
usage: join.py [-h] ctx1.csv ... ctxn.csv [--output OUTPUT]
```

## Anomaly detection

The script `ad.py` implements three anomaly detection algorithms, each
having either a batch or streaming mode.  The batch mode reads and
processes all of the input and then traverses it
again to score it.  (In principle the "training set" and "test set"
could be different.)  The streaming mode traverses the input just once
and scores each transaction when it is first seen, using the model
obtained for the previous input consumed only.

The three algorithms are:

* AVF (Attribute Value Frequency).  From a paper by Koufakou et
al. 2007.  Anomaly scores are averaged probabilities of attribute values.
* AVC (Attribute Value Coding/Compression).  A minor variant that
views the anomaly scores as compressed sizes.
* RASP (working name).  An experimental approach whose main advantage
  over AVC is the ability to adapt to seeing new attributes during
  execution (in a streaming setting).  Safe to ignore for now.

The script's options are as follows.

```
usage: ad.py [-h] -i INPUT -o OUTPUT [-s {avf,avc,rasp}]
             [-m {batch,stream}]
```

The default scoring algorithm and mode are AVF/batch.

## KL-means

The file `klmeans.py` contains an experimental (meta-)algorithm based
on an information-theoretic variant of k-means clustering.  (The name "KL-means"
is a weak pun on "k-means" and "Kullback-Leilber (KL) divergence", a
distance-like function on probability distributions in information
theory.)

There are two versions, a batch version and a stream version:

* The batch version takes two parameters, K and N.  The K parameter sets the 
  number of clusters to try to find, and the N parameter sets the number of 
  iterations to run for.  The end result is a score file which also includes
  the final assignment of objects to clusters 0..K-1.  This makes N+1 passes
  over the data so it can take a while, but usually small values of N (<5) work.
* The stream version does not take any additional parameters.  It starts with 
  one cluster and compresses using that as in AVC until there is evidence that
  another cluster is needed.  This is based on a heuristic: if we see objects
  compress very badly (i.e. worse than simply encoding the attributes in binary)
  then we spawn a new model for a new cluster.  Again, the output includes 
  score (= compressed size) and final cluster assignment of each object.

The intention is that the scores can be used (as with AVC and AVF) as anomaly 
scores.  The cluster assignment information is (at this point) ostly for 
debugging purposes, but might have other uses.

The script's options are as follows.

```
usage: klmeans.py [-h] -i INPUT -o OUTPUT [-k K] [-n N]
             [-m {batch,stream}] [-e EPSILON]
```

As new functionality, in batch mode, all `k` values from 1 to `K` are tried
and the scores obtained using the best result are saved.  (The "best" result 
is the one that leads to the lowest overall score for the whole
dataset.)  The `EPSILON` parameter (default 0.01) is the minimum
required improvement in size over the previous iteration (to avoid a
pathological situation where the compressed size keeps decreasing only
by a tiny amount).

In streaming mode, `K` is ignored and the right value of `k` is determined
adaptively based on the data.  Likewise, `EPSILON` is ignored.

## Simple mining-based anomaly detectors

We have incorporated implementations of two simple anomaly detectors from the 
literature, called FPOF (Frequent Pattern Outlier Factor) and OD (Outlier 
Degree).  They don't work very well on our data (or perhaps at all), but 
if you want to try them you can use this script:

```
pattern.py [-h] --input INPUT --output OUTPUT [--score {fpof,od}]
                  [--conf CONF] [--minsupp MINSUPP]
```

Here, the `--score` parameter chooses which algorithm to use, `conf` chooses the
confidence level to use for rules and `--minsuppo` chooses the minimum support
level to use.  Both of these take values between 0 and 1; higher tends to be 
faster.  `conf` is only relevant for `--score od` while `--minsupp` is 
relevant to both algorithms.  Again though, there is no particular reason to 
use these and they don't currently have `make` targets.

## Krimp: smarter mining-based anomaly detection

We have also added a Linux binary based on the Krimp system, which mines the 
frequent (closed) itemsets and then attempts to identify a subset of them that
"compress the data well".  The end result includes scores for the objects 
indicating their "compressed size", which, like in AVC or RASP, we interpret
as anomaly scores.  There are currently no parameters (Krimp does have some, 
but we currently hard-wire them to reasonable values).

```
krimp.py [-h] --input INPUT --output OUTPUT
```

There is also a `make` target to run krimp on all of the contexts, to do 
this do `make krimpbatch`.  (Krimp only runs in batch mode, but its C++ 
implementation is pretty fast.)

## Checking scores against ground truth

The script `check.py` takes a score file (produced by `ad.py` or `klmeans.py`), 
a ground truth CSV file, and some additional options and produces a
ranking file, which is a CSV file listing just those object IDs found
in the ground truth with their ranks (= how anomalous the object
was).  The checker also prints out the "normalized discounted
cumulative gain" (NDCG) and the "area under ROC curve" (AUC) as
proxies for how "good" the ranking is overall.
The options are as follows:

```
check.py [-h] -i INPUT -o OUTPUT -g GROUNDTRUTH
                [-t TY] [-r]

```

The optional argument `-t TY` allows to specify the
"type" of ground truth ID.  The ground truth CSV files consist of
objectID,type pairs, where the type is a string indicating what kind
of obejct the objectID is.  For example, in the ADM ground truth CSV
files the types are things like `AdmSubject::Node`.  It is helpful to
specify this so that the checker can filter out the ground truth
objects that we do not expect to find in a given context.  If such
objects are included then the NDCG and AUC scores are typically very
low due to the other objects being counted as "misses".

The optional `-r` argument allows to specify whether the scores should
be sorted in decreasing order (i.e. high score = more anomalous).  The
default behavior is to sort in increasing order, which is appropriate
for AVF (small score = more anomalous).  For AVC and RASP (and
KL-means), the scores are "compressed sizes" so larger scores are more
anomalous and the `-r` flag should be used.

