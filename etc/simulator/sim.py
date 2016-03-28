import sys
import simulator_diagnoser as sd

grammar = sd.Sequence('apt',
                      sd.NonTerminal('exfiltration'),
                      sd.NonTerminal('staging'),
                      sd.NonTerminal('penetration'),
                      matchable=False)

if __name__ == "__main__":
    graph = sd.SegmentationGraph()

    graph.generate(0.10, ranks=(6, 8), per_rank=(4, 5), seed=0)
    symptom = 23

    if len(sys.argv) == 1:
        dx = sd.SimpleDiagnoser(grammar)
        print "Potential APTs: ", dx.diagnose(graph, symptom)
    else:
        if(sys.argv[1] == 'pdf'):
            graph.print_dot()
        elif(sys.argv[1] == 'json'):
            graph.print_json()
        else:
            dx = sd.SimpleDiagnoser(grammar)
            dxs = dx.diagnose(graph, symptom)
            graph.print_dot(dxs)
