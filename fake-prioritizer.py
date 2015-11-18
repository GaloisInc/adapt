#! /usr/bin/env python

import random
from time import sleep
import gremlinrestclient

from kafka import SimpleProducer, KeyedProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv

def main():
    ta3Host = '127.0.0.1'
    toP = b'prioritizer'
    toDX = b'dx'
    toUI  = b'ui'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
    uiProducer = KeyedProducer(kafka)
    consumer = KafkaConsumer(toP, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)


    def sendMsg(m): 
        i = random.randint(1,100)
        if i > 70:
            print("Segment #" + m + " is highly suspect. Immediately notify user.")
            uiProducer.send_messages(toUI, b'fromPR', m)
        print("Notify diagnostic engine of segment #" + m + "...")
        producer.send_messages(toDX, m)
    def recvMsg():
        try:
             x = consumer.next()
             return x;
        except ConsumerTimeout:
             return None

    oper(sendMsg,recvMsg)

def oper(sendMsg,recvMsg):
    print("Wait for new data...")
    client = gremlinrestclient.GremlinRestClient()
    dict = {}
    while True:
        v = recvMsg()
        if not (v is None):
            print("Compute priority of segment #" + v.value + "...")
            try:
                i = dict.pop(v.value)
                sleep(random.randint(0,3))
                print("Priority of segment #" + v.value + " computed.")
                sendMsg(v.value)
            except KeyError:
                print("Still need more data to compute full priority for segment #" + v.value)
                dict[v.value] = True
            print("Wait for new data...")

if __name__ == '__main__':
    main()
