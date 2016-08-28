import operator


class NodeRanker(object):
    def __init__(self, skip_children=True, max_elements=10):
        self.skip_children = skip_children
        self.max_elements = max_elements
        self.clear()

    def clear(self):
        self.rank = []
        self.scores = {}

    def new_node(self, node, parents, score):
        if score > 0.0:
            self.scores[node] = score

            if self.skip_children:
                for p in parents:
                    if self.scores.get(p, 0.0) >= score:
                        return

            self.rank.append((node, score))


    def get_rank(self):
        self.rank.sort(key=operator.itemgetter(1), reverse=True)
        self.rank = self.rank[0:self.max_elements]
        return self.rank
