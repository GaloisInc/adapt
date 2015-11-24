import sys
import colour
import networkx as nx
import simulator_diagnoser.generation as generation


class SegmentationGraph:

    def __init__(self):
        self.clear()

    def clear(self):
        self.G = nx.DiGraph()

    def node_str(self, n):
        node = self.G.node[n]
        node_s = "%d\\n" % (n)
        for apt_elem in node['apt']:
            node_s += "%s: %.2f\\n" % apt_elem
        return node_s

    def print_dot(self, dxs=[], out=sys.stdout):
        out.write("digraph {\n node [margin=0 fontsize=6];\n")
        for node in self.G.nodes_iter():
            pos = len([1 for dx in dxs if node in dx])
            color = self.get_color(pos, len(dxs))
            out.write(" %d [label=\"%s\", style=filled, fillcolor = \"%s\"];\n" %
                      (node, self.node_str(node), color))
        for edge in self.G.edges_iter():
            out.write(" %d -> %d;\n" % edge)
        out.write("}\n")

    def generate(self, p, ranks, per_rank, seed=None):
        generation.random_dag(self, p, ranks, per_rank, seed)
        generation.annotate_graph(self)

    def create_paths(self, func, n, current, acc, skip=False, prepend=True):
        nodes = func(n)
        if not skip:
            if prepend:
                current = [n] + current
            else:
                current = current + [n]
        if nodes:
            for n in nodes:
                self.create_paths(func, n, current, acc, prepend=prepend)
        else:
            acc.append(current)
        return acc

    def full_paths(self, n):
        return [x + [n] + y
                for x in self.create_paths(self.G.predecessors, n, [], [], skip=True)
                for y in self.create_paths(self.G.successors, n, [], [], skip=True, prepend=False)]

    def get_color(self, current, max_value, range=(0.2, 0.0)):
        hue, saturation, luminance = (0.0, 1.0, 0.5)
        if max_value > 0 and current > 0:
            hue = range[0] + float(current) / float(max_value) * (range[1] - range[0])
        else:
            saturation, luminance = (0.0, 1.0)
        return colour.Color(hue=hue, saturation=saturation, luminance=luminance).hex_l
