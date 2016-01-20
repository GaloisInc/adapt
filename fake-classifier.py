#! /usr/bin/env python

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout
from time import sleep
import gremlinrestclient
import random


def main():
    ta3_host = '127.0.0.1'
    to_class = b'classifier'
    to_p = b'prioritizer'

    kafka_server = ta3_host + ':9092'
    kafka = KafkaClient(kafka_server)
    producer = SimpleProducer(kafka)
    consumer = KafkaConsumer(to_class, bootstrap_servers=[kafka_server],
                             consumer_timeout_ms=20)

    def send_msg(m):
        producer.send_messages(to_p, m)

    def recv_msg():
        try:
            x = consumer.next()
            return x
        except ConsumerTimeout:
            return None

    oper(send_msg, recv_msg)


def oper(send_msg, recv_msg):
    print("Wait for new data...")
    client = gremlinrestclient.GremlinRestClient()
    while True:
        v = recv_msg()
        if not (v is None):
            print("Classify activites on segment #" + v.value + "...")
            sleep(random.randint(0, 20))
            print("Search for APT behavior on segment #" + v.value + "...")
            sleep(random.randint(0, 20))
            print("Graph annoated. Notify prioritizer...")
            send_msg(v.value)
            print("Found %d edges." % client.execute('g.E().count()').data[0])
            print("Wait for new data...")


if __name__ == '__main__':
    main()
