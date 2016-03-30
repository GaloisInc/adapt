import sys
import simulator_diagnoser as sd

grammar = sd.Sequence([sd.Terminal('penetration'),
                       sd.Terminal('staging'),
                       sd.Terminal('exfiltration')])

if __name__ == "__main__":
    graph = sd.SegmentationGraph()

    graph.generate(0.10, ranks=(6, 8), per_rank=(4, 5), seed=0)
    symptoms = [23]

    dx = sd.SimpleDiagnoser(grammar)
    dxs = dx.diagnose(graph, symptoms)

    if len(sys.argv) == 1:
        dx = sd.SimpleDiagnoser(grammar)
        print "Potential APTs: ", dxs
    else:
        if(sys.argv[1] == 'pdf'):
            writer = sd.PdfWriter()
            writer.append_dx(graph, dxs)
            writer.write('sim.pdf')
        else:
            if(sys.argv[1] == 'json'):
                graph.print_json(dxs.reduced_diagnosis())
