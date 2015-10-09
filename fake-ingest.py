#! /usr/bin/env python

import prov_pb2
import random

from time import sleep

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv

def main():
    ta3Host = '127.0.0.1'
    toSeg = b'segmenter'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
#    consumer = KafkaConsumer(fromTA1, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)


    def sendMsg(m): producer.send_messages(toSeg, m)
    def recvMsg():
#        try:
#             x = consumer.next()
#             return x;
#        except ConsumerTimeout:
             return None

    oper(sendMsg,recvMsg)

def oper(sendMsg,recvMsg):
    start = 0
    while True:
        v = recvMsg()
        if not (v is None):
            print("IN: " + v.value)
        count = random.randint(1,100)
        sendMsg("{"+str(start)+","+str(start+count-1)+"}")
        start = start + count;
        sleep(10)

if __name__ == '__main__':
    main()
