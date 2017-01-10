#!/bin/bash

assembledAdapt="./target/scala-2.11/adapt-assembly-0.1.jar"
assembledScepter="./scepter/target/scala-2.11/scepter-assembly-0.1.jar"

#remote="adapt.galois.com~/acceptance_tests"
remote="alfred:/srv/www/adapt.galois.com/public_html/acceptance_tests"

rm ./adapt-tester.jar 2> /dev/null
rm ./adapt.jar 2> /dev/null
sbt clean

# Upload the actual adapt jar
sbt assembly && \
scp $assembledAdapt ${remote}/adapt.jar

# Upload the hash for the adapt jar (queried by adapt-tester)
md5 -q $assembledAdapt > ./adapt.hash && \
scp ./adapt.hash ${remote}/current.hash
rm ./adapt.hash 2> /dev/null

# Upload the adapt-tester jar
sbt scepter/assembly && \
scp $assembledScepter ${remote}/adapt-tester.jar

# Upload the hash for the adapt-tester jar
md5 -q $assembledScepter > ./scepter.hash && \
scp ./scepter.hash ${remote}/current-tester.hash
rm ./scepter.hash 2> /dev/null

# Download/run the latest adapt-tester
wget http://adapt.galois.com/adapt-tester.jar && \
java -jar ./adapt-tester.jar ./Engagement1DataCDM13/pandex/ta1-clearscope-cdm13_pandex.bin
rm ./adapt-tester.jar 2> /dev/null

