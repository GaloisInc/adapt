#! /usr/bin/env python

from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from cassandra.cluster import Cluster

from os import getenv
import logging

def main():
    ta3Host = getenv('TC_SERVICES_HOST')
    toTA1   = b'TC2to1'
    fromTA1 = b'TC1to2'

    logging.basicConfig(
         format='%(asctime)s.%(msecs)s:%(name)s:%(thread)d:%(levelname)s:%(process)d:%(message)s',
         level=logging.DEBUG
    )

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)
    consumer = KafkaConsumer(fromTA1, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)

    cassandraCluster = Cluster()
    dbSession = cassandraCluster.connect('blackboard')

    def sendMsg(m): producer.send_messages(toTA1, m)
    def recvMsg():
        try:
             x = consumer.next()
             return x
        except ConsumerTimeout:
             return None

    oper(sendMsg,recvMsg,dbSession)

def oper(sendMsg,recvMsg,dbSession):
    state = False
    while True:
        v = recvMsg()
        if not (v is None):
            print(v)
            storeInBlackBoard(dbSession,v)
        # XXX randomly send a message

def storeInBlackBoard(db,v):
    db.execute('INSERT INTO blackboard.test (msg) VALUES (%s)', [v.value])

if __name__ == '__main__':
    main()
