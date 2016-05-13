
from kafka import SimpleProducer, KafkaConsumer, KafkaClient
import os

kafkaServer='localhost:9092'

kafka = KafkaClient(kafkaServer)
producer = SimpleProducer(kafka)

consumer = KafkaConsumer('ad', bootstrap_servers=[kafkaServer])

def produce(msg):
    res = producer.send_messages('ac', msg)
    print(res) 

def consume():
    for msg in consumer:
        print(msg)
        if msg.value == b'1':
            produce(b'0')
            print("Starting Anomaly Detection")
            os.system('./start.sh')
            print("Finished Anomaly Detection")
            produce(b'1')
            break

print('Waiting for signal from segmenter...')
consume()
