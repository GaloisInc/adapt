import sys
import colour
import graphviz
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

    def get_node_apt_labels(self, n):
        node = self.G.node[n]
        return [x[0] for x in node['apt']]

    def generate_dot(self, dxs=[], symptoms=[], label="Segmentation Graph"):
        dot = graphviz.Digraph(graph_attr={'label': label,
                                           'labelloc': 't',
                                           'fontname': 'sans-serif'},
                               node_attr={'margin': '0',
                                          'fontsize': '6',
                                          'fontname': 'sans-serif'})
        for node in self.G.nodes_iter():
            is_symptom = node in symptoms
            pos = len([1 for dx in dxs if node in dx])
            color = self.get_color(pos, len(dxs))
            dot.node(str(node),
                     self.node_str(node),
                     style='filled',
                     fillcolor=color,
                     color='blue' if is_symptom else 'black',
                     penwidth='2.5' if is_symptom else '1')
        for i, o in self.G.edges_iter():
            dot.edge(str(i), str(o))
        return dot

    def print_json(self, dxs=[], out=sys.stdout):
        out.write("data = {\n")
        out.write(" \"nodes\": [\n")
        for n, node in enumerate(self.G.nodes_iter()):
            pos = len([1 for dx in dxs if node in dx])
            color = self.get_color(pos, len(dxs))
            if(set(color.split(' ')) == set("#ffffff".split(' '))):
                out.write("      { \"node\": %d, \"value\":\"#cccccc\" }" % node)
            else:
                out.write("      { \"node\": %d, \"value\":\"%s\" }" % (node, color))
            if(n != len(list(self.G.nodes_iter())) - 1):
                out.write(",\n")
            else:
                out.write("\n")
        out.write(" ],\n")

        out.write(" \"links\": [\n")
        for n, edge in enumerate(self.G.edges_iter()):
            out.write("  {\"source\":%d, \"target\": %d, \"value\": 0}" % edge)
            if(n != len(list(self.G.edges_iter())) - 1):
                out.write(",\n")
            else:
                out.write("\n")
        out.write(" ]\n")
        out.write("};\n")

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
