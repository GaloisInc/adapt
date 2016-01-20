#! /usr/bin/env python

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout
from time import sleep
import gremlinrestclient
import random


def main():
    ta3Host = '127.0.0.1'
    toClass = b'classifier'
    toP = b'prioritizer'

    kafkaServer = ta3Host + ':9092'
    kafka = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
    consumer = KafkaConsumer(toClass, bootstrap_servers=[kafkaServer],
                             consumer_timeout_ms=20)

    def sendMsg(m):
        producer.send_messages(toP, m)

    def recvMsg():
        try:
            x = consumer.next()
            return x
        except ConsumerTimeout:
            return None

    oper(sendMsg, recvMsg)


def oper(sendMsg, recvMsg):
    print("Wait for new data...")
    client = gremlinrestclient.GremlinRestClient()
    while True:
        v = recvMsg()
        if not (v is None):
            print("Classify activites on segment #" + v.value + "...")
            sleep(random.randint(0, 20))
            print("Search for APT behavior on segment #" + v.value + "...")
            sleep(random.randint(0, 20))
            print("Graph annoated. Notify prioritizer...")
            sendMsg(v.value)
            print("Found %d edges." % client.execute('g.E().count()').data[0])
            print("Wait for new data...")


if __name__ == '__main__':
    main()
