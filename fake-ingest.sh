#! /usr/bin/env bash

# Copyright 2015, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.
#

KAFKA_DIR=${HOME}/Documents/kafka_2.10-0.8.2.0

start_zookeeper_if_needed() {
    netstat -an | egrep '^tcp.*:2181 .*LISTEN' > /dev/null ||
        (cd ${KAFKA_DIR} &&
         bin/zookeeper-server-start.sh config/zookeeper.properties) &
}

start_kafka_if_needed() {
    netstat -an | egrep '^tcp.*:9092 [ \*:]*LISTEN' > /dev/null ||
        (cd ${KAFKA_DIR} &&
         bin/kafka-server-start.sh config/server.properties) &
    RACE_KLUDGE=4
    sleep ${RACE_KLUDGE}
    date

    (cd ${KAFKA_DIR} &&
     bin/kafka-topics.sh --create \
                         --zookeeper localhost:2181 \
			 --topic current_time \
			 --replication-factor 1 \
                         --partitions 1)

    (cd ${KAFKA_DIR} &&
     date | bin/kafka-console-producer.sh \
                --broker-list localhost:9092 \
                --topic current_time)
}


netstat -an | egrep '2181 |9092 '

start_zookeeper_if_needed
set -x
start_kafka_if_needed

export TC_SERVICES_HOST=localhost

set -x
exec ./fake-ingest.py "$@"
