# Adapt

ADAPT software for Transparent Computing This integration repo holds references
to individual component repos.

# Pulling Commits

Update adapt files from a subtree repo with a command like:

    git subtree pull --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master --squash

or push adapt files to a subtree repo with a command like:

    git subtree push --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master

# Directory Descriptions

- ad: Anomaly Detection
- classifier: Classifier
- config: System configuration files for primitive services including the
  database and communication queues.
- dx: Diagnostics
- example: Basic ProvN examples which will eventually be replaced by CDM.
- ingest: The ingester, which takes data from its source (TA-1) and insert it
  into the database after some basic checks and transformations.
- install: Scripts to install all available tools in a single virtual machine.
- kb: Knowledge base (adversarial models etc)
- px: Pattern Extractor
- Segment: Graph segmentation
- trace: Script for working with the data store on seaside.galois.com
- ui: A fake user interface that was once useful (trash now?)
