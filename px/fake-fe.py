#! /usr/bin/env python

import random
from time import sleep
import gremlinrestclient

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv

def main():
    ta3Host = '127.0.0.1'
    toFE = b'features'
    toAD = b'AD'
    toClass = b'classifier'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
    consumer = KafkaConsumer(toFE, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)


    def sendMsg(m): 
        producer.send_messages(toAD, m)
        producer.send_messages(toClass, m)
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
    while True:
        v = recvMsg()
        if not (v is None):
            print("Searching for features on segment #" + v.value + "...")
            sleep(random.randint(0,20))
            print("Features found and annotated on graph. Notify classifiers and anomoly detector...")
            sendMsg(v.value)
            print("Wait for new data...")

if __name__ == '__main__':
    main()
