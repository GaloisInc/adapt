#!/bin/sh

echo "Generating Segment Graph Visualization"

python3 showSegmentsSubGraph.py > static/seggraph.dot
dot -Tsvg seggraph.dot > static/seggraph.svg

echo "Done!"
