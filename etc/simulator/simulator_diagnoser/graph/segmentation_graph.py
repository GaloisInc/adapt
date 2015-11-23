import sys
import networkx as nx
import simulator_diagnoser.generation as generation
import colorsys

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
    color = "#8BEF91"
    out.write("digraph {\n node [margin=0 fontsize=6];\n")
    for node in self.G.nodes_iter():
      pos = [ind for ind in xrange(len(dxs)) if node in dxs[ind]]
      if len(pos) > 0: #any(node in dx for dx in dxs):
          #color = self.convert_to_rgba(0,6,len(pos),[(0, 0, 255), (0, 255, 0), (255, 0, 0)]) #"#8BEF91"
          color = self.get_rgb_from_hue_spectrum(len(pos) / float(len(dxs)), 0.2, 0.0)
          print color
      else:
          color = "#FFFFFF"
      out.write(" %d [label=\"%s\", style=filled, fillcolor = \"%s\"];\n" % (node, self.node_str(node), color))
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

  def get_rgb_from_hue_spectrum(self, percent, start_hue, end_hue):
      # spectrum is red (0.0), orange, yellow, green, blue, indigo, violet (0.9)
      hue = percent * (end_hue - start_hue) + start_hue
      lightness = 0.5
      saturation = 1
      r, g, b = colorsys.hls_to_rgb(hue, lightness, saturation)
      rgba = "#" + "0x{:02X}".format(int(r * 255))[2:]
      rgba = rgba + "0x{:02X}".format(int(g * 255))[2:]
      rgba = rgba + "0x{:02X}".format(int(b * 255))[2:]
      return rgba
