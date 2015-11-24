import random


def random_dag(graph, p, ranks, per_rank, seed=None):
    min_ranks, max_ranks = ranks

    if seed is not None:
        random.seed(seed)

    graph.clear()

    r = ranks[0] + int(round(random.random() % (ranks[1] - ranks[0] + 1)))

    nodes = 0
    for i in xrange(r):
        new_nodes = per_rank[0] + int(round(random.random() % (per_rank[1] - per_rank[0] + 1)))

        for j in xrange(nodes):
            for k in xrange(new_nodes):
                if random.random() < p:
                    graph.G.add_edge(j, k + nodes)

        nodes = nodes + new_nodes
    return graph
