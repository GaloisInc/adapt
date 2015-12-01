#! /usr/bin/env python

import random
import gremlinrestclient

from time import sleep

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

def main():
    ta3Host = '127.0.0.1'
    toSeg = b'segmenter'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
#    consumer = KafkaConsumer(fromTA1, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)


    def sendMsg(m): producer.send_messages(toSeg, m)
    def recvMsg():
             return None

    oper(sendMsg,recvMsg)

def oper(sendMsg,recvMsg):
    start = 0
    client = gremlinrestclient.GremlinRestClient()
    print("Wait for new data from TA1...");
    sleep(10);
    while True:
        print("New data recieved.")
        v = recvMsg()
        if not (v is None):
            print("IN: " + v.value)
        count = random.randint(1,100)
        print("Processing inserts " + str(start) + " to " + str(start+count-1) + "...")
        for id in range(start, start+count):
          client.execute("graph.addVertex(label, p1, 'id', p2)", bindings={"p1": "process", "p2": str(id)})
        print("Inserted! Notify segmenter.")
        sendMsg("{"+str(start)+","+str(start+count-1)+"}")
        start = start + count;
        print("Wait for new data from TA1...");
        sleep(10)

if __name__ == '__main__':
    main()
