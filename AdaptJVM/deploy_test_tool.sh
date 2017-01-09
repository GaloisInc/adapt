#!/bin/bash

rm ./adapt-tester.jar 2> /dev/null
rm ./adapt.jar 2> /dev/null
sbt clean
sbt assembly && scp target/scala-2.11/adapt-assembly-0.1.jar adapt.galois.com:~/acceptance_tests/adapt.jar
sbt scepter/assembly && scp scepter/target/scala-2.11/scepter-assembly-0.1.jar adapt.galois.com:~/acceptance_tests/adapt-tester.jar

wget http://adapt.galois.com/adapt-tester.jar && \
java -jar ./adapt-tester.jar ./Engagement1DataCDM13/pandex/ta1-clearscope-cdm13_pandex.bin && \
rm ./adapt-tester.jar
