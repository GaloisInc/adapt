#! /usr/bin/env python

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
    while True:
        v = recvMsg();
        if not (v is None):
            print("DX: " + v.value)
            sendMsg(v.value)


if __name__ == '__main__':
    main()
