#! /usr/bin/env python

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
    while True:
        v = recvMsg()
        if not (v is None):
            print("FE: " + v.value)
            sendMsg(v.value)

if __name__ == '__main__':
    main()
