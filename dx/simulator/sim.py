import sys
import simulator_diagnoser as sd

if __name__ == "__main__":
    config = sd.ConfigParser()

    grammar = config.get_grammar()
    graph = config.get_graph()
    symptoms = config.get_symptoms()

    dx = sd.SimpleDiagnoser(grammar)
    dxs = dx.diagnose(graph, symptoms)

    if len(sys.argv) == 1:
        print "Reduced diagnosis: ", dxs
        dxs.print_dx()
    else:
        if(sys.argv[1] == 'pdf'):
            writer = sd.PdfWriter()
            writer.append_dx(graph, dxs)
            writer.write('sim.pdf')
        else:
            if(sys.argv[1] == 'json'):
                graph.print_json(dxs.reduced_diagnosis())
