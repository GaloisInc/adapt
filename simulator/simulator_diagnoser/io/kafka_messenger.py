from .messenger import *
import kafka
import logging

class KafkaMessenger(Messenger):

    def __init__(self, url='localhost:9092', topic='dx', downstream_topics=['ui']):
        self.downstream_topics = downstream_topics
        self.consumer = kafka.KafkaConsumer(topic, bootstrap_servers=[url])
        self.producer = kafka.KafkaProducer(bootstrap_servers=[url])
        self.log = logging.getLogger('dx-logger')
        self.log.info("Listening to topic " + topic)

    def receive(self):
        for msg in self.consumer:
            self.info("Received messaage: " + str(msg))
            if msg.value == DONE:
                self.send(IN_PROGRESS)
                yield
                self.send(DONE)

    def send(self, msg=DONE):
        self.info("Sending message " + str(msg) + " to topics " + str(self.downstream_topics))
        for topic in self.downstream_topics:
            s = self.producer.send(topic, msg)
