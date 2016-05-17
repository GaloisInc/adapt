from .messenger import *
import kafka

class KafkaMessenger(Messenger):

    def __init__(self, url='localhost:9092', topic='dx', downstream_topics=['ui']):
        self.downstream_topics = downstream_topics
        self.consumer = kafka.KafkaConsumer(topic, bootstrap_servers=[url])
        self.producer = kafka.KafkaProducer(bootstrap_servers=[url])

    def receive(self):
        for msg in self.consumer:
            if msg.value == DONE:
                self.send(IN_PROGRESS)
                yield
                self.send(DONE)

    def send(self, msg=DONE):
        for topic in self.downstream_topics:
            s = self.producer.send(topic, msg)
