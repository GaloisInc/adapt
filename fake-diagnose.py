from kafka import SimpleProducer, KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from cassandra.cluster import Cluster

from os import getenv
import logging
import time

def main():
    ta3Host = getenv('TC_SERVICES_HOST')
    toTA5   = b'TC2to5'

    logging.basicConfig(
         format='%(asctime)s.%(msecs)s:%(name)s:%(thread)d:%(levelname)s:%(process)d:%(message)s',
         level=logging.DEBUG
    )

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    producer = SimpleProducer(kafka)

    cassandraCluster = Cluster()
    dbSession = cassandraCluster.connect('blackboard')

    def sendMsg(m): producer.send_messages(toTA1, m)

    oper(sendMsg,dbSession)

def oper(sendMsg,db):
    state = False
    while True:
        xs = db.execute('SELECT * FROM blackboard.test')
        for x in xs:
            print x
            sendMsg(x)
            time.sleep(5)

if __name__ == '__main__':
    main()
