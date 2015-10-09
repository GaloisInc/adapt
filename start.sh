#!/bin/sh

python ui/fake-ui.py &
python dx/fake-diagnose.py &
python dx/fake-prioritizer.py &
python classifier/fake-classifier.py &
python ad/fake-ad.py &
python featureExtractor/fake-fe.py &
python segment/fake-segment.py &
python ingest/fake-ingest.py &
