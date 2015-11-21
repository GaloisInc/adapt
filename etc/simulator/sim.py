import sys
import simulator_diagnoser as sd

transitions = {'exfiltration': (True, False, ['staging']),
               'staging': (False, False, ['penetration']),
               'penetration': (False, True, [])}

if __name__ == "__main__":
  graph = sd.SegmentationGraph()
  
  graph.generate(0.10, ranks=(6,8), per_rank=(4,5), seed=0)
  symptom = 10
  
  if len(sys.argv) > 1:
    graph.print_dot()
  else:
    dx = sd.SimpleDiagnoser(transitions)
    print "Potential APTs: ", dx.diagnose(graph, symptom)