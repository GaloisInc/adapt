import logging

IN_PROGRESS = b'0'
DONE = b'1'

class Messenger(object):

    def __init__(self):
        self.log = logging.getLogger('dx-logger')

    def receive(self):
        self.log.info("Received stubbed message")
        self.send(IN_PROGRESS)
        yield
        self.send(DONE)

    def send(self, msg=DONE):
        self.log.info("Sending message: " + str(msg))
