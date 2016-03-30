import sys
import simulator_diagnoser as sd

grammar = sd.Sequence([sd.NonTerminal('penetration'),
                       sd.NonTerminal('staging'),
                       sd.NonTerminal('exfiltration')])

if __name__ == "__main__":
    graph = sd.SegmentationGraph()

    graph.generate(0.10, ranks=(6, 8), per_rank=(4, 5), seed=0)
    symptom = 23

    if len(sys.argv) == 1:
        dx = sd.SimpleDiagnoser(grammar)
        print "Potential APTs: ", dx.diagnose(graph, symptom)
    else:
        if(sys.argv[1] == 'pdf'):
            dot = graph.generate_dot()
            writer = sd.PdfWriter(dot)
            writer.write('sim.pdf')
        else:
            dx = sd.SimpleDiagnoser(grammar)
            dxs = dx.diagnose(graph, symptom)
            if(sys.argv[1] == 'json'):
                graph.print_json(dxs)
            else:
                dot = graph.generate_dot(dxs)
                writer = sd.PdfWriter(dot)
                writer.write('sim.pdf')
