import kafka
import logging

class KafkaHandler(logging.Handler):

    def __init__(self, url='localhost:9092', topic='dx-log'):
        logging.Handler.__init__(self)
        self.topic = topic
        while self.producer is None:
            try:
                self.producer = kafka.KafkaProducer(bootstrap_servers=[url], retries=10)
            except kafka.errors.NoBrokersAvailable:
                time.sleep(2)

    def emit(self, record):
        if self.producer:
            entry = self.format(record)
            self.producer.send(self.topic, entry.encode())
            self.producer.flush()
