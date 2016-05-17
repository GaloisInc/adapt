
IN_PROGRESS = b'0'
DONE = b'1'

class Messenger(object):

    def __init__(self):
        pass

    def receive(self):
        self.send(IN_PROGRESS)
        yield
        self.send(DONE)

    def send(self, msg=DONE):
        pass
