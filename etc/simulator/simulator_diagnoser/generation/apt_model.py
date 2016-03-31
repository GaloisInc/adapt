import random


def exfiltration_rnd(graph, n):
    pred = len(graph.predecessors(n))
    if pred:
        return random.triangular(mode=0) / float(pred+1)
    else:
        return random.triangular(mode=1)


def penetration_rnd(graph, n):
    succ = len(graph.successors(n))
    if succ:
        return random.triangular(mode=0) / float(succ+1)
    else:
        return random.triangular(mode=1)


def staging_rnd(graph, n):
    return random.random()


APT_labels = {'penetration': penetration_rnd,
              'staging': staging_rnd,
              'exfiltration': exfiltration_rnd}


def annotate_graph(graph):
    for n in graph.nodes_iter():
        apt_elems = []

        for k, v in APT_labels.iteritems():
            prob = v(graph, n)
            if prob > 0.3:
                apt_elems.append((k, prob))

        graph.set_node_data(n, 'apt', apt_elems)
