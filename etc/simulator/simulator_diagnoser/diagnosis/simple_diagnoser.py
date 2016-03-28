import simulator_diagnoser.matcher as matcher


class SimpleDiagnoser:

    def __init__(self, grammar):
        self.grammar = grammar

    def diagnose(self, graph, symptom):
        paths = graph.full_paths(symptom)
        results = []

        for path in paths:
            labelled_path = [graph.get_node_apt_labels(n) for n in path]
            matches = self.grammar.match_path(labelled_path)

            for m in matches:
                matched_indexes = [x[0] for x in m.matches]
                matched_path = path[min(matched_indexes):max(matched_indexes)+1]
                if symptom in matched_path and matched_path not in results:
                    results.append(matched_path)

        return results
