
class BackwardAnalysis(object):

    def __init__(self, graph):
        self.graph = graph

    def __iter__(self):
        return self

    def __next__(self):
        raise StopIteration
