import operator


class NodeRanker(object):
    def __init__(self, skip_children=True, max_elements=10):
        self.skip_children = skip_children
        self.max_elements = max_elements
        self.clear()

    def clear(self):
        self.rank = []

    def new_node(self, node, parents, score):
        if score > 0.0:
            self.rank.append((node, score))
            self.rank.sort(key=operator.itemgetter(1), reverse=True)
            self.rank = self.rank[0:self.max_elements]

    def get_rank(self):
        return self.rank
