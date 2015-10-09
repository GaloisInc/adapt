#! /usr/bin/env python

import random

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
        print("PR Rand: " + str(i))
        if i > 95:
            uiProducer.send_messages(toUI, b'fromPR', m)
        producer.send_messages(toDX, m)
    def recvMsg():
        try:
             x = consumer.next()
             return x;
        except ConsumerTimeout:
             return None

    oper(sendMsg,recvMsg)

def oper(sendMsg,recvMsg):
    dict = {}
    while True:
        v = recvMsg()
        if not (v is None):
            print("PR: " + v.value)
            try:
                i = dict.pop(v.value)
                sendMsg(v.value)
            except KeyError:
                dict[v.value] = True

if __name__ == '__main__':
    main()
