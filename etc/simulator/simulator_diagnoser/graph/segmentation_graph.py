import sys
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
    color = "#8BEF91"
    out.write("digraph {\n node [margin=0 fontsize=6];\n")
    for node in self.G.nodes_iter():
      pos = [ind for ind in xrange(len(dxs)) if node in dxs[ind]]
      if len(pos) > 0: #any(node in dx for dx in dxs):
          pos = map(lambda x: x+1, pos)
          mx = sum(list(range(1, int(len(dxs)))))
          pp = map(lambda x: self.convert_to_rgba_pair(0, len(dxs), x, [(0, 0, 255), (0, 255, 0), (255, 0, 0)]), pos)
          pp = reduce(lambda x, y: ((x[0]+y[0]) % 255, (x[1]+y[1]) % 255, (x[2]+y[2]) % 255), pp)
          color = "#" + "0x{:02X}".format(int(pp[0]))[2:]
          color = color + "0x{:02X}".format(int(pp[1]))[2:]
          color = color + "0x{:02X}".format(int(pp[2]))[2:]
          #color = self.convert_to_rgba(0, mx, sum(pos), [(0, 0, 255), (0, 255, 0), (255, 0, 0)]) #"#8BEF91"
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

  def convert_to_rgba(self,minval, maxval, val, colors):
      max_index = len(colors)-1
      v = float(val-minval) / float(maxval-minval) * max_index
      i1, i2 = int(v), min(int(v)+1, max_index)
      (r1, g1, b1), (r2, g2, b2) = colors[i1], colors[i2]
      f = v - i1
      rgba = "#" + "0x{:02X}".format(int(r1 + f*(r2-r1)))[2:]
      rgba = rgba + "0x{:02X}".format(int(g1 + f*(g2-g1)))[2:]
      rgba = rgba + "0x{:02X}".format(int(b1 + f*(b2-b1)))[2:]
      return rgba

  def convert_to_rgba_pair(self,minval, maxval, val, colors):
      max_index = len(colors)-1
      v = float(val-minval) / float(maxval-minval) * max_index
      i1, i2 = int(v), min(int(v)+1, max_index)
      (r1, g1, b1), (r2, g2, b2) = colors[i1], colors[i2]
      f = v - i1
      return (int(r1 + f*(r2-r1)), int(g1 + f*(g2-g1)), int(b1 + f*(b2-b1)))
