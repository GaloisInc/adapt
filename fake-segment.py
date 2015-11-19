#! /usr/bin/env python

import random
from time import sleep
import gremlinrestclient

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv

def main():
    ta3Host = '127.0.0.1'
    toSeg = b'segmenter'
    toFE = b'features'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
    consumer = KafkaConsumer(toSeg, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)


    def sendMsg(m): producer.send_messages(toFE, m)
    def recvMsg():
        try:
             x = consumer.next()
             return x;
        except ConsumerTimeout:
             return None

    oper(sendMsg,recvMsg)

def oper(sendMsg,recvMsg):
    print("Wait for new data...")
    start = 0
    client = gremlinrestclient.GremlinRestClient()
    while True:
        v = recvMsg()
        if not (v is None):
            print("Processing vertices " + v.value)
            sleep(random.randint(0,20))
            client.execute("graph.addVertex(label, p1, 'id', p2)", bindings={"p1": "segment", "p2": str(start)})
            print("Segmented! Notify feature extractor...")
            sendMsg(str(start))
            start = start + 1;
            print("Wait for new data...")

if __name__ == '__main__':
    main()
