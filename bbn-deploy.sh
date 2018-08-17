#!/bin/bash

STAMP=$(date)
STAMP=${STAMP// /_}
STAMP=${STAMP//:/-}
STAMP="packaged_on-$STAMP"
touch ./$STAMP
echo "Packaging into: adapt.tar.gz"
tar zcf ./adapt.tar.gz --exclude=".git/" --exclude="iforest.exe" --exclude="ppmTreeEval/" --exclude="legacy/" --exclude="fca/" --exclude="target/" --exclude="neo4j.db" --exclude=".idea/" --exclude="adapt.tar.gz" --exclude=".DS_Store" ./
scp ./adapt.tar.gz bbn:~/
rm ./$STAMP
rm ./adapt.tar.gz
