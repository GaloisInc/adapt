#!/bin/bash

tar zcf ./adapt.tar.gz --exclude="legacy/" --exclude=".git/" --exclude="fca/" --exclude="target/" --exclude="neo4j.db" --exclude=".idea/" --exclude="adapt.tar.gz" --exclude=".DS_Store" ./
scp ./adapt.tar.gz bbn:~/
rm ./adapt.tar.gz
