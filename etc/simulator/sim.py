import sys
import simulator_diagnoser as sd

transitions = {'exfiltration': (True, False, ['staging']),
               'staging': (False, False, ['penetration']),
               'penetration': (False, True, [])}

if __name__ == "__main__":
    graph = sd.SegmentationGraph()

    graph.generate(0.10, ranks=(6, 8), per_rank=(4, 5), seed=0)
    symptom = 23

    if len(sys.argv) == 1:
        dx = sd.SimpleDiagnoser(transitions)
        print "Potential APTs: ", dx.diagnose(graph, symptom)
    else:
        if(sys.argv[1] == 'pdf'):
            graph.print_dot()
        elif(sys.argv[1] == 'json'):
            graph.print_json()
        else:
            dx = sd.SimpleDiagnoser(transitions)
            dxs = dx.diagnose(graph, symptom)
            graph.print_dot(dxs)
