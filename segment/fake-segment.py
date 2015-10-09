#! /usr/bin/env python

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
    start = 0
    while True:
        v = recvMsg()
        if not (v is None):
            print("SG: " + v.value)
            sendMsg(str(start))
            start = start + 1;

if __name__ == '__main__':
    main()
