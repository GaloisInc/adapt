#! /usr/bin/env python3

from kafka import SimpleProducer, KafkaConsumer, KafkaClient
import os

kafkaServer='localhost:9092'

kafka = KafkaClient(kafkaServer)
producer = SimpleProducer(kafka)

consumer = KafkaConsumer('ad', bootstrap_servers=[kafkaServer])

def produce(msg, topic):
    res = producer.send_messages(topic, msg)
    print(res)

def consume():
    for msg in consumer:
        print(msg)
        if msg.value == b'\x01':
            produce(b'\x00', 'ac')
            produce(b'Starting Anomaly Detection', 'ad-log')
            os.system('./start.sh')
            produce(b'Finished Anomaly Detection', 'ad-log')
            produce(b'\x01', 'ac')
            break

produce(b'Waiting for signal from segmenter...', 'ad-log')
consume()
