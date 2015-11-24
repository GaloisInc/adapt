import simulator_diagnoser.matcher as matcher


class SimpleDiagnoser:

    def __init__(self, transitions):
        self.sequence_matcher = matcher.SequenceMatcher(transitions)

    def diagnose(self, graph, symptom):
        self.sequence_matcher.clear()
        paths = graph.full_paths(symptom)
        results = []
        for path in paths:
            for i, node in enumerate(path):
                self.sequence_matcher.accept_list(i, [x[0] for x in graph.G.node[node]['apt']])

            for match in self.sequence_matcher.get_matches():
                matched_path = path[min(match):max(match)+1]
                if symptom in matched_path and matched_path not in results:
                    results.append(matched_path)
            self.sequence_matcher.clear()

        return results
