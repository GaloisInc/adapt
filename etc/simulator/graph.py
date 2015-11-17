import sys
import random
import networkx as nx

def penetration_rnd(G, n):
  pred = len(G.predecessors(n))
  if pred:
    return random.triangular(mode=0) / float(pred+1)
  else:
    return random.triangular(mode=1)

def exfiltration_rnd(G, n):
  succ = len(G.successors(n))
  if len(G.successors(n)):
    return random.triangular(mode=0) / float(succ+1)
  else:
    return random.triangular(mode=1)

def staging_rnd(G, n):
  return random.random()

APT_labels = {'penetration': penetration_rnd, \
              'staging': staging_rnd, \
              'exfiltration': exfiltration_rnd}


def random_dag(p, ranks=(6,8), per_rank=(4,5), seed=None):
  min_ranks, max_ranks = ranks
  
  if seed is not None:
    random.seed(seed)
  
  G=nx.DiGraph()
  
  r = ranks[0] + int(round(random.random() % (ranks[1] - ranks[0] + 1)))
  
  nodes = 0
  for i in xrange(r):
    new_nodes = per_rank[0] + int(round(random.random() % (per_rank[1] - per_rank[0] + 1)))
    
    for j in xrange(nodes):
      for k in xrange(new_nodes):
        if random.random() < p:
          G.add_edge(j, k + nodes)
    
    nodes = nodes + new_nodes
  return G

def cone(func, n):
  f_res = func(n)
  s = set(f_res)
  for p in f_res:
    s = s | cone(func, p)
  return s

successor_cone = lambda G, n: cone(G.successors, n)
predecessor_cone = lambda G,n: cone(G.predecessors, n)

def annotate_graph(G):
  for n in G.nodes_iter():
    apt_elems = []
    
    for k, v in APT_labels.iteritems():
      prob = v(G, n)
      if prob > 0.3:
        apt_elems.append((k, prob))
    
    G.node[n]['apt'] = apt_elems

def node_str(G, n):
  node = G.node[n]
  node_s = "%d\\n" % (n)
  for apt_elem in node['apt']:
    node_s += "%s: %.2f\\n" % apt_elem
  return node_s

def print_dot(G, out=sys.stdout):
  out.write("digraph {\n node [margin=0 fontsize=6];\n")
  for node in G.nodes_iter():
    out.write(" %d [label=\"%s\"];\n" % (node, node_str(G, node)))
  for edge in G.edges_iter():
    out.write(" %d -> %d;\n" % edge)
  out.write("}\n")

G = random_dag(0.10, ranks=(6,8), per_rank=(4,5), seed=0)
annotate_graph(G)
print_dot(G)

#print G.nodes(data=True)

