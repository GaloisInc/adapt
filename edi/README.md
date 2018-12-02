# Simple anomaly detectors

## Quick start

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

Currently, connecting to an endpoint other than
`http://localhost:8080` is not supported by the makefile.  We could
easily make that configurable, but the makefile is really just
intended as a sanity check to make sure things are working.  It is
intended to create a new script that automates the extraction and
scoring pipeline for production use.

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
extract.py [-h] [-p PORT] [-u URL] -i INPUT
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

## Joining contexts

The script `join.py` joins together two contexts (say, `left.csv` and 
`right.csv`) and produces a new
context CSV file (with uniquely renamed attributes).  Note that if
both contexts are sparse then the result can be much larger than the
two separate CSV files.

```
usage: join.py [-h] [--left LEFT] [--right RIGHT] [--output OUTPUT]
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
             [-m {batch,stream}]
```

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

