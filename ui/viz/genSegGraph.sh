#!/bin/sh

echo "Generating Segment Graph Visualization"

python3 showSegmentsSubGraph.py > seggraph.dot
dot -Tsvg seggraph.dot > seggraph.svg

rm seggraph.dot

echo "Done!"
