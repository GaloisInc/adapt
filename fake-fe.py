#! /usr/bin/env python3

import random
from time import sleep

import kafka
from kafka.common import ConsumerTimeout

from os import getenv

def main():
    ta3Host = '127.0.0.1'
    toPX = 'pe'
    toSE = 'se'

    kafkaServer = ta3Host + ':9092'
    producer = kafka.KafkaProducer(bootstrap_servers=[kafkaServer])
    consumer = kafka.KafkaConsumer(toPX, bootstrap_servers=[kafkaServer])
    print(consumer.subscription())
    for msg in consumer:
      if msg.value == b'\x01':
        producer.send(toSE, b'\x01')

if __name__ == '__main__':
    main()
