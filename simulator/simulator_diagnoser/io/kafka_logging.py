import kafka
import logging

class KafkaHandler(logging.Handler):

    def __init__(self, url='localhost:9092', topic='dx-log'):
        logging.Handler.__init__(self)
        self.topic = topic
        self.producer = kafka.KafkaProducer(bootstrap_servers=[url])

    def emit(self, record):
        entry = self.format(record)
        self.producer.send(self.topic, entry.encode())
        self.producer.flush()
