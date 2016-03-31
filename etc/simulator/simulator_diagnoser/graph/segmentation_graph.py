import sys
import colour
import graphviz
import networkx as nx
import simulator_diagnoser.generation as generation


class SegmentationGraph(object):
    def __init__(self):
        self.clear()

    def clear(self):
        self.__G = nx.DiGraph()

    def add_edge(self, u, v):
        self.__G.add_edge(u, v)

    def set_node_data(self, n, key, value):
        if n in self.__G.node:
            self.__G.node[n][key] = value

    def get_node_data(self, n, key):
        if n in self.__G.node:
            return self.__G.node[n][key]
        return None

    def nodes_iter(self):
        for n in self.__G.nodes_iter():
            yield n

    def edges_iter(self):
        for u, v in self.__G.edges_iter():
            yield u, v

    def successors(self, n):
        return self.__G.successors(n)

    def predecessors(self, n):
        return self.__G.predecessors(n)

    def get_node_apt_labels(self, n):
        data = self.get_node_data(n, 'apt')
        if data:
            return [x[0] for x in data]
        else:
            return []

    def node_str(self, n, labels=None):
        node = self.__G.node[n]
        node_s = "%d\\n" % (n)
        for apt_elem in node['apt']:
            if labels is not None and apt_elem[0] not in labels:
                continue
            node_s += "%s: %.2f\\n" % apt_elem
        return node_s

    def generate_dot(self, dxs=[], path=[], match=None, symptoms=[], label='Segmentation Graph'):
        dot = graphviz.Digraph(graph_attr={'label': label,
                                           'labelloc': 't',
                                           'fontname': 'sans-serif'},
                               node_attr={'margin': '0',
                                          'fontsize': '6',
                                          'fontname': 'sans-serif'})
        translucent = '#00000019' if path or dxs else 'black'
        for node in self.nodes_iter():
            linecolor, color, penwidth = ('black', 'white', '1')
            fontcolor = linecolor
            node_label = self.node_str(node)

            if node in path:
                linecolor = 'black'
                labels = match.get_labels(path.index(node))
                node_label = self.node_str(node, labels)
                if len(labels):
                    color = self.get_color(1, 1)
            elif dxs:
                pos = len([1 for dx in dxs if node in dx])
                color = self.get_color(pos, len(dxs))
            else:
                linecolor = translucent
                fontcolor = translucent

            if node in symptoms:
                linecolor, penwidth = ('blue', '2.5')

            dot.node(str(node),
                     node_label,
                     style='filled',
                     fillcolor=color,
                     color=linecolor,
                     fontcolor=fontcolor,
                     penwidth=penwidth)

        for i, o in self.edges_iter():
            if i in path and o in path or dxs:
                color = 'black'
            else:
                color = translucent

            dot.edge(str(i), str(o), color=color)
        return dot

    def print_json(self, dxs=[], out=sys.stdout):
        out.write("data = {\n")
        out.write(" \"nodes\": [\n")
        for n, node in enumerate(self.__G.nodes_iter()):
            pos = len([1 for dx in dxs if node in dx])
            color = self.get_color(pos, len(dxs))
            if(set(color.split(' ')) == set("#ffffff".split(' '))):
                out.write("      { \"node\": %d, \"value\":\"#cccccc\" }" % node)
            else:
                out.write("      { \"node\": %d, \"value\":\"%s\" }" % (node, color))
            if(n != len(list(self.__G.nodes_iter())) - 1):
                out.write(",\n")
            else:
                out.write("\n")
        out.write(" ],\n")

        out.write(" \"links\": [\n")
        for n, edge in enumerate(self.__G.edges_iter()):
            out.write("  {\"source\":%d, \"target\": %d, \"value\": 0}" % edge)
            if(n != len(list(self.__G.edges_iter())) - 1):
                out.write(",\n")
            else:
                out.write("\n")
        out.write(" ]\n")
        out.write("};\n")

    def generate(self, p, ranks, per_rank, seed=None):
        generation.random_dag(self, p, ranks, per_rank, seed)

    def annotate(self, grammar):
        generation.annotate_graph(self, grammar)

    @staticmethod
    def create_paths(func, n, current, acc, skip=False, prepend=True):
        nodes = func(n)
        if not skip:
            if prepend:
                current = [n] + current
            else:
                current = current + [n]
        if nodes:
            for n in nodes:
                SegmentationGraph.create_paths(func, n, current, acc, prepend=prepend)
        else:
            acc.append(current)
        return acc

    def full_paths(self, n):
        return [x + [n] + y
                for x in self.create_paths(self.predecessors, n, [], [], skip=True)
                for y in self.create_paths(self.successors, n, [], [], skip=True, prepend=False)]

    @staticmethod
    def get_color(current, max_value, range=(0.2, 0.0)):
        hue, saturation, luminance = (0.0, 1.0, 0.5)
        if max_value > 0 and current > 0:
            hue = range[0] + float(current) / float(max_value) * (range[1] - range[0])
        else:
            saturation, luminance = (0.0, 1.0)
        return colour.Color(hue=hue, saturation=saturation, luminance=luminance).hex_l
