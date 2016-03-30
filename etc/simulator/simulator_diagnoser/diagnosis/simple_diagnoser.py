class SimpleDiagnoser(object):

    def __init__(self, grammar):
        self.grammar = grammar

    def diagnose(self, graph, symptom):
        paths = graph.full_paths(symptom)
        dr = DiagnosticResult(symptom)

        for path in paths:
            labelled_path = [graph.get_node_apt_labels(n) for n in path]
            matches = self.grammar.match_reverse_path(labelled_path)
            dr.append(path, matches)
        return dr

class DiagnosticResult(object):
    def __init__(self, symptom):
        self.symptom = symptom
        self.results = []

    def append(self, path, matches):
        self.results.append((path, matches))

    def __repr__(self):
        return str(self.reduced_diagnosis())

    def reduced_diagnosis(self):
        reduced_results = []

        for path, matches in self.results:
            for m in matches:
                matched_indexes = [x[0] for x in m.matches]
                matched_path = path[min(matched_indexes):max(matched_indexes)+1]
                if self.symptom in matched_path and matched_path not in reduced_results:
                    reduced_results.append(matched_path)

        return reduced_results
