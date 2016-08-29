import logging

IN_PROGRESS = b'\x00'
DONE = b'\x01'

class Messenger(object):

    def __init__(self):
        self.log = logging.getLogger('dx-logger')

    def receive(self):
        self.log.info("Received stubbed message")
        while(True):
            self.send(IN_PROGRESS)
            yield
            self.send(DONE)

    def send(self, msg=DONE):
        self.log.info("Sending message: " + str(msg))
