#! /usr/bin/env python

from kafka import KafkaClient, KafkaConsumer
from kafka.common import ConsumerTimeout

from os import getenv
from Tkinter import *
import time

def main():
    root = Tk()
    S = Scrollbar(root)
    T = Text(root, heigh=50, width=50)
    S.pack(side=RIGHT, fill=Y)
    T.pack(side=LEFT, fill=Y)
    S.config(command=T.yview)
    T.config(yscrollcommand=S.set)
    root.update()

    ta3Host = '127.0.0.1'

    toUI = b'ui'

    kafkaServer = ta3Host + ':9092'
    kafka    = KafkaClient(kafkaServer)
    consumer = KafkaConsumer(toUI, bootstrap_servers=[kafkaServer], consumer_timeout_ms=20)

    def recvMsg():
        try:
            x = consumer.next()
            return x;
        except ConsumerTimeout:
            return None;

    oper(recvMsg, T, root)

def oper(recvMsg, T, root):
    while True:
        v = recvMsg();
        if not (v is None):
            print("UI: " + v.value + " from " + v.key)
            if v.key == 'fromPR':
                T.insert(END, "URGENT FROM PR: " + v.value + '\n')
            else:
                T.insert(END, v.value+"\n")
        root.update()

if __name__ == '__main__':
    main()
