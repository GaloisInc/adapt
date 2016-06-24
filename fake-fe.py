#! /usr/bin/env python3

import random
from time import sleep

import kafka
from kafka.common import ConsumerTimeout

from os import getenv
import sys

def main():
    ta3Host = '127.0.0.1'
    toPX = 'pe'
    toSE = 'se'
    toLog = 'pe-log'

    kafkaServer = ta3Host + ':9092'
    print("Connecting to Kafka...", file=sys.stderr)
    producer = kafka.KafkaProducer(bootstrap_servers=[kafkaServer])
    print("Connected producer to Kafka", file=sys.stderr)
    consumer = kafka.KafkaConsumer(toPX, bootstrap_servers=[kafkaServer])
    print("Connected consumer to Kafka", file=sys.stderr)
    print(consumer.subscription())
    for msg in consumer:
      print("Got a message: %s" % msg.value, file=sys.stderr)
      if msg.value == b'\x01':
        producer.send(toSE, b'\x01')
        producer.send(toLog, b'Signaled for segmenter to start.')

def safe():
    try:
        main()
    except kafka.errors.NoBrokersAvailable:
        print("Kafka failed, No brokers available", file=sys.stderr)

if __name__ == '__main__':
    print("Feature Extractor has started.",file=sys.stderr)
    while True:
        safe()
