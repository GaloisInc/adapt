import random


def func_probability(func, n):
    result = len(func(n))
    if result:
        return random.triangular(mode=0) / float(result+1)
    else:
        return random.triangular(mode=1)


def anotation_probability(graph, n, label):
    if label == 'penetration':
        return func_probability(graph.successors, n)
    elif label == 'exfiltration':
        return func_probability(graph.predecessors, n)
    return random.random()


def annotate_graph(graph, grammar):
    labels = grammar.get_labels()
    for n in graph.nodes_iter():
        apt_elems = []

        for label in labels:
            prob = anotation_probability(graph, n, label)
            if prob > 0.3:
                apt_elems.append((label, prob))

        graph.set_node_data(n, 'apt', apt_elems)
