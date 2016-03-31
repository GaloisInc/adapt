import simulator_diagnoser.graph as graph
import simulator_diagnoser.matcher as matcher
import yaml
import random

class ConfigParser(object):
    def __init__(self, filename='dx.yml'):
        self.config = yaml.safe_load(open(filename, 'r'))
        self.segmentation_graph = None
        self.symptoms = None
        self.grammar = None

    def get_graph(self):
        if not self.segmentation_graph:
            sim_config = self.config['simulation']
            self.segmentation_graph = graph.SegmentationGraph()
            self.segmentation_graph.generate(sim_config['link_probability'],
                                             sim_config['ranks'],
                                             sim_config['per_rank'],
                                             sim_config['seed'])
            self.segmentation_graph.annotate(self.get_grammar())
        return self.segmentation_graph

    def get_symptoms(self):
        if not self.symptoms:
            dx_config = self.config['diagnosis']
            s = dx_config.get('symptoms', [])
            if len(s) != 1 and self.segmentation_graph:
                random.seed(self.config['diagnosis']['seed'])
                s = [self.segmentation_graph.get_random_node()]
            self.symptoms = s
        return self.symptoms

    def get_grammar(self):
        if not self.grammar:
            grammar_elems = self.config['grammar']
            assert len(grammar_elems) == 1

            self.grammar = self.parse_grammar(grammar_elems[0])
        return self.grammar

    @staticmethod
    def parse_grammar(elem):
        if isinstance(elem, basestring):
            return matcher.Terminal(elem)
        elif isinstance(elem, dict):
            keys = elem.keys()
            assert len(keys) == 1

            key = keys[0]
            key_rule = ConfigParser.get_rule(key)
            rule_data = elem[key]

            children = [ConfigParser.parse_grammar(value) for value
                                                          in rule_data.get('rules', None)]
            label = elem[key].get('label', None)
            return key_rule(children, label=label)

    @staticmethod
    def get_rule(label):
        rules = {
            'sequence': matcher.Sequence,
            'optional': matcher.Optional,
            'choice': matcher.Choice,
            'one_or_more': matcher.OneOrMore,
            'zero_or_more': matcher.ZeroOrMore,
            'optional_sequence': matcher.OptionalSequence
        }
        return rules[label]
