#! /usr/bin/env python

import random
from time import sleep
import gremlinrestclient

from kafka import KeyedProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv
import time

def main():
    ta3Host = '127.0.0.1'
    toDX = b'dx'
    toUI = b'ui'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = KeyedProducer(kafka)
    consumer = KafkaConsumer(toDX, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)

    def sendMsg(m): producer.send_messages(toUI, b'fromDX', m)

    def recvMsg():
        try:
            x = consumer.next()
            return x;
        except ConsumerTimeout:
            return None;

    oper(sendMsg, recvMsg)

def oper(sendMsg, recvMsg):
    print("Wait for new data...")
    client = gremlinrestclient.GremlinRestClient()
    while True:
        v = recvMsg();
        if not (v is None):
            print("Diagnose APTs in segment #" + v.value + "...")
            sleep(random.randint(0,30))
            if(random.randint(0,3) > 1):
                print("APT-like behavior found. Notify user...")
                sendMsg(v.value)
            else:
                print("No suspect behavior")
            print("Wait for new data...")


if __name__ == '__main__':
    main()
