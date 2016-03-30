import sys
import simulator_diagnoser as sd

grammar = sd.Sequence([sd.Terminal('penetration'),
                       sd.Terminal('staging'),
                       sd.Terminal('exfiltration')])

if __name__ == "__main__":
    graph = sd.SegmentationGraph()

    graph.generate(0.10, ranks=(6, 8), per_rank=(4, 5), seed=0)
    symptom = 23

    dx = sd.SimpleDiagnoser(grammar)
    dxs = dx.diagnose(graph, symptom)

    if len(sys.argv) == 1:
        dx = sd.SimpleDiagnoser(grammar)
        print "Potential APTs: ", dx.diagnose(graph, symptom)
    else:
        if(sys.argv[1] == 'pdf'):
            dot = graph.generate_dot(symptoms=[symptom])
            dot2 = graph.generate_dot(dxs.reduced_diagnosis(),
                                      symptoms=[symptom])
            writer = sd.PdfWriter(dot, dot2)
            writer.write('sim.pdf')
        else:
            if(sys.argv[1] == 'json'):
                graph.print_json(dxs.reduced_diagnosis())
