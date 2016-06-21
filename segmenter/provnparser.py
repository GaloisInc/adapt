"""
    =======
    `Parser`
    =======

    This module contains a pyparsing bnf for
    provn.

    Adria Gascon, 2016.
"""

from pyparsing import *
import sys


class ProvRelation:
    def __init__(self, s, t, att_val_list, timestamp=None):
        self.s = s
        self.t = t
        self.timestamp = None if timestamp == '-' else timestamp
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])
        self.name = None


class SegmentExpr(ProvRelation):
    def __str__(self):
        return 'includes({0}, {1}, [{2}])'.format(
            self.s, self.t, ','.join(['{0}=\"{1}\"'.format(k, v)
                for k, v in self.att_val_dict.items()]))

    def label(self):
        return 'includes'

class EdgeExpr(ProvRelation):
    def __init__(self, etype, s, t):
        super().__init__(s, t, {})
        self.etype = etype

    def label(self):
        return self.etype


class CommExpr(ProvRelation):
    def __str__(self):
        return 'wasInformedBy({0}, {1}, {2})'.format(
            self.s, self.t, self.att_val_dict)

    def label(self):
        return 'wasInformedBy'


class DerivationExpr(ProvRelation):
    def __str__(self):
        return 'wasDerived({0}, {1}, {2})'.format(
            self.s, self.t, self.att_val_dict)

    def label(self):
        return 'wasDerivedFrom'


class AssociationExpr(ProvRelation):
    def __str__(self):
        return 'wasAssociatedWith({0}, {1}, {2}, {3})'.format(
            self.s, self.t,
            self.timestamp if self.timestamp else '-'   , self.att_val_dict)

    def label(self):
        return 'wasAssociatedWith'


class UsageExpr(ProvRelation):
    def __str__(self):
        return 'used({0}, {1}, {2}, {3})'.format(
            self.s, self.t, self.timestamp, self.att_val_dict)

    def label(self):
        return 'used'


class ValidationExpr(ProvRelation):
    def __str__(self):
        return 'wasInvalidatedBy({0}, {1}, {2}, {3})'.format(
            self.s, self.t,
            self.timestamp, self.att_val_dict)

    def label(self):
        return 'wasInvalidatedBy'


class GenerationExpr(ProvRelation):
    def __str__(self):
        return 'wasGeneratedBy({0}, {1}, {2}, {3})'.format(
            self.s, self.t, self.timestamp, self.att_val_dict)

    def label(self):
        return 'wasGeneratedBy'


class Segment:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'segment({0},[{1}])'.format(self.id,
            ','.join(['{0}=\"{1}\"'.format(k, v)
                for k, v in self.att_val_dict.items()]))

    def label(self):
        return 'segment'


class Agent:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'agent({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'agent'


class Activity:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'activity({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'activity'


class Entity:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'entity({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'entity'


class Subject:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'subject({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'subject'


class EntityFile:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'entity-file({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'entity-file'


class EntityNetFlow:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'entity-netflow({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'entity-netflow'


class EntityMemory:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'entity-memory({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'entity-memory'


class Resource:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'resource({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'resource'


class Host:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'host({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'host'


class Pattern:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'pattern({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'pattern'


class Phase:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'phase({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'phase'


class APT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'apt({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'apt'


class EDGE_EVENT_AFFECTS_MEMORY:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_affects_memory({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_affects_memory'


class EDGE_EVENT_AFFECTS_FILE:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_affects_file({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_affects_file'


class EDGE_EVENT_AFFECTS_NETFLOW:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_affects_netflow({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_affects_netflow'


class EDGE_EVENT_AFFECTS_SUBJECT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_affects_subject({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_affects_subject'


class EDGE_EVENT_AFFECTS_SRCSINK:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_affects_srcsink({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_affects_srcsink'


class EDGE_EVENT_HASPARENT_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_hasparent_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_hasparent_event'


class EDGE_EVENT_ISGENERATEDBY_SUBJECT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_isgeneratedby_subject({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_isgeneratedby_subject'


class EDGE_EVENT_CAUSES_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_event_causes_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_event_causes_event'


class EDGE_SUBJECT_AFFECTS_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_subject_affects_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_subject_affects_event'


class EDGE_SUBJECT_HASPARENT_SUBJECT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_subject_hasparent_subject({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_subject_hasparent_subject'


class EDGE_SUBJECT_HASPRINCIPAL:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_subject_hasprincipal({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_subject_hasprincipal'


class EDGE_SUBJECT_RUNSON:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_subject_runson({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_subject_runson'


class EDGE_FILE_AFFECTS_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_file_affects_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_file_affects_event'


class EDGE_NETFLOW_AFFECTS_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_netflow_affects_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_netflow_affects_event'


class EDGE_MEMORY_AFFECTS_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_memory_affects_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_memory_affects_event'


class EDGE_SRCSINK_AFFECTS_EVENT:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_srcsink_affects_event({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_srcsink_affects_event'


class EDGE_OBJECT_PREV_VERSION:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_object_prev_version({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_object_prev_version'


class EDGE_SUBJECT_HASLOCALPRINCIPAL:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'edge_subject_haslocalprincipal({0},{1})'.format(self.id, self.att_val_dict)

    def label(self):
        return 'edge_subject_haslocalprincipal'


class ResourceFactory:
    @classmethod
    def create(cls, type_, id_, att_val_list=[]):
        if type_ == 'Activity':
            return Activity(id_, att_val_list)
        elif type_ == 'Entity':
            return Entity(id_, att_val_list)
        elif type_ == 'Entity-File':
            return EntityFile(id_, att_val_list)
        elif type_ == 'Entity-NetFlow':
            return EntityNetFlow(id_, att_val_list)
        elif type_ == 'Entity-Memory':
            return EntityMemory(id_, att_val_list)
        elif type_ == 'Resource':
            return Resource(id_, att_val_list)
        elif type_ == 'Subject':
            return Subject(id_, att_val_list)
        elif type_ == 'Host':
            return Host(id_, att_val_list)
        elif type_ == 'Agent':
            return Agent(id_, att_val_list)
        elif type_ == 'Pattern':
            return Pattern(id_, att_val_list)
        elif type_ == 'Phase':
            return Phase(id_, att_val_list)
        elif type_ == 'APT':
            return APT(id_, att_val_list)
        elif type_ == 'Segment':
            return Segment(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_AFFECTS_MEMORY':
            return EDGE_EVENT_AFFECTS_MEMORY(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_AFFECTS_FILE':
            return EDGE_EVENT_AFFECTS_FILE(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_AFFECTS_NETFLOW':
            return EDGE_EVENT_AFFECTS_NETFLOW(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_AFFECTS_SUBJECT':
            return EDGE_EVENT_AFFECTS_SUBJECT(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_AFFECTS_SRCSINK':
            return EDGE_EVENT_AFFECTS_SRCSINK(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_HASPARENT_EVENT':
            return EDGE_EVENT_HASPARENT_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_ISGENERATEDBY_SUBJECT':
            return EDGE_EVENT_ISGENERATEDBY_SUBJECT(id_, att_val_list)
        elif type_ == 'EDGE_EVENT_CAUSES_EVENT':
            return EDGE_EVENT_CAUSES_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_SUBJECT_AFFECTS_EVENT':
            return EDGE_SUBJECT_AFFECTS_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_SUBJECT_HASPARENT_SUBJECT':
            return EDGE_SUBJECT_HASPARENT_SUBJECT(id_, att_val_list)
        elif type_ == 'EDGE_SUBJECT_HASPRINCIPAL':
            return EDGE_SUBJECT_HASPRINCIPAL(id_, att_val_list)
        elif type_ == 'EDGE_SUBJECT_RUNSON':
            return EDGE_SUBJECT_RUNSON(id_, att_val_list)
        elif type_ == 'EDGE_FILE_AFFECTS_EVENT':
            return EDGE_FILE_AFFECTS_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_NETFLOW_AFFECTS_EVENT':
            return EDGE_NETFLOW_AFFECTS_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_MEMORY_AFFECTS_EVENT':
            return EDGE_MEMORY_AFFECTS_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_SRCSINK_AFFECTS_EVENT':
            return EDGE_SRCSINK_AFFECTS_EVENT(id_, att_val_list)
        elif type_ == 'EDGE_OBJECT_PREV_VERSION':
            return EDGE_OBJECT_PREV_VERSION(id_, att_val_list)
        elif type_ == 'EDGE_SUBJECT_HASLOCALPRINCIPAL':
            return EDGE_SUBJECT_HASLOCALPRINCIPAL(id_, att_val_list)
        raise Exception('Unknown resource type: {}'.format(type_))


class EventFactory:
    @classmethod
    def create(cls, type_, s, t):
        if s is None or t is None:
            return
        if type_ == 'includes':
            return SegmentExpr(s, t, att_val_list, timestamp)
        else:
            return EdgeExpr(type_, s, t)
        raise Exception('Unknown event type: {}'.format(type_))


class PrefixDecl:
    def __init__(self, id_, url):
        self.id = id_
        self.url = url

    def __str__(self):
        return 'prefix {0} {1}'.format(self.id, self.url)


class Document:
    def __init__(self):
        self.filename = None
        self.expression_list = []
        self.prefix_decls = []

    def parse_provn(self, filename):
        self.filename = filename
        bnf(self)['document'].parseFile(filename, True)
        assert self.expression_list != None
        assert self.prefix_decls != None

    def union(self, doc):
        for e in doc.expression_list:
            self.expression_list.append(e)
        self.prefix_decls = list(set(self.prefix_decls) | set(doc.prefix_decls))

    def __str__(self):
        def f(x): return '\t' + str(x)
        l = ['document']
        l += map(f, self.prefix_decls)
        l += map(f, self.expression_list)
        l += ['endDocument']
        return '\n'.join(l)

    ########################################################
    # Parsing actions

    def make_document(self, t):
        """
        Parsing action for the Program non-terminal
        """
        self.prefix_decls = t['prefix_decls']
        self.expression_list = t['expression_list']

    def make_expression(self, t):
        # This is to make sure that we do not miss any expression
        assert isinstance(t[0], Activity) or \
               isinstance(t[0], CommExpr) or \
               isinstance(t[0], UsageExpr) or \
               isinstance(t[0], DerivationExpr) or \
               isinstance(t[0], GenerationExpr) or \
               isinstance(t[0], Agent) or \
               isinstance(t[0], ValidationExpr) or \
               isinstance(t[0], AssociationExpr) or \
               isinstance(t[0], Entity), t

    def make_prefix_decl(self, t):
        return PrefixDecl(t['id'], t['url'])

    def make_activity_expression(self, t):
        return Activity(t['id'][0], t['att_val_list'])

    def make_agent_expression(self, t):
        return Agent(t['id'][0], t['att_val_list'])

    def make_entity_expression(self, t):
        return Entity(t['id'][0], t['att_val_list'])

    def make_usage_expression(self, t):
        try:
            return UsageExpr(t['s'][0], t['t'][0],
                t['att_val_list'], t['timestamp'])
        except KeyError:
            return UsageExpr(t['s'][0], t['t'][0], [], t['timestamp'])

    def make_association_expression(self, t):
        try:
            return AssociationExpr(
                t['s'][0], t['t'][0], t['att_val_list'], t['timestamp'])
        except KeyError:
            return AssociationExpr(
                t['s'][0], t['t'][0], [], t['timestamp'])

    def make_validation_expression(self, t):
        try:
            return ValidationExpr(
                t['s'][0], t['t'][0], t['att_val_list'], t['timestamp'])
        except KeyError:
            return ValidationExpr(t['s'][0], t['t'][0], [], t['timestamp'])

    def make_generation_expression(self, t):
        try:
            return GenerationExpr(
                t['s'][0], t['t'][0], t['att_val_list'], t['timestamp'])
        except KeyError:
            return GenerationExpr(
                t['s'][0], t['t'][0], [], t['timestamp'])

    def make_communication_expression(self, t):
        try:
            return CommExpr(
                t['s'][0], t['t'][0], t['att_val_list'], t['timestamp'])
        except KeyError:
            timestamp = t.get('timestamp', None)
            return CommExpr(t['s'][0], t['t'][0], [], timestamp)

    def make_derivation_expression(self, t):
        timestamp = t.get('timestamp', None)
        return DerivationExpr(
            t['s'][0], t['t'][0], t['att_val_list'], timestamp)

    def make_att_val_pair(self, t):
        return (t['att'], t['val'])


def bnf(doc):
    """
    Defines the Grammar for provn
    :return: Pyparsing objects for parsing provn
    """
    """
    Grammar Definition
    """
    lbrack = Suppress("[")
    rbrack = Suppress("]")
    lpar = Literal("(")
    rpar = Literal(")")
    colon = Literal(":")
    comma = Suppress(",")
    double_quote = Suppress('\"')
    equal = Suppress("=")
    prefix_name = Word(alphanums + '-')
    name = Word(alphanums)
    word_with_spaces = Word(alphanums + ' /:_-().,{}[]+*=$')
    dash = '-'
    la = '<'
    ra = '>'
    url_word = Word(alphanums + '_/-#:.')
    date = Word(nums + '-')
    time = Word(nums + ':')
    timestamp = Combine(date + 'T' + time)
    # Identifiers and att-value pairs
    identifier = Combine(prefix_name.setResultsName('prefix') + colon +
        name.setResultsName('name'))
    att = Combine(prefix_name + colon + name).setResultsName('att')
    att_val_pair = Combine(att + equal +
        double_quote + word_with_spaces.setResultsName('val') + double_quote).\
        setParseAction(doc.make_att_val_pair)

    # Expressions
    generation_expr = (Keyword('wasGeneratedBy') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_generation_expression)
    activity_expr = (Keyword('activity') + lpar +
        identifier.setResultsName('id') + comma + lbrack +
        delimitedList(att_val_pair).setResultsName('att_val_list') + rbrack +
            rpar).setParseAction(doc.make_activity_expression)
    agent_expr = (Keyword('agent') + lpar +
        identifier.setResultsName('id') + comma + lbrack +
        delimitedList(att_val_pair).setResultsName('att_val_list') + rbrack +
            rpar).setParseAction(doc.make_agent_expression)
    entity_expr = (Keyword('entity') + lpar +
        identifier.setResultsName('id') + comma + lbrack +
        delimitedList(att_val_pair).setResultsName('att_val_list') + rbrack +
            rpar).setParseAction(doc.make_entity_expression)
    usage_expr = (Keyword('used') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_usage_expression)
    association_expr = (Keyword('wasAssociatedWith') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_association_expression)
    validation_expr = (Keyword('wasInvalidatedBy') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_validation_expression)
    communication_expr = (Keyword('wasInformedBy') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') + comma +
        ((lbrack + delimitedList(att_val_pair).setResultsName('att_val_list') +
        rbrack) | (lbrack + rbrack)) + rpar).\
        setParseAction(doc.make_communication_expression)
    derivation_expr = (Keyword('wasDerivedFrom') + lpar +
        identifier.setResultsName('s') + comma +
        identifier.setResultsName('t') + comma +
        ((lbrack + delimitedList(att_val_pair).setResultsName('att_val_list') +
        rbrack) | (lbrack + rbrack)) + rpar).\
        setParseAction(doc.make_derivation_expression)
    expression = ( association_expr | activity_expr | communication_expr | entity_expr | usage_expr | generation_expr | derivation_expr | agent_expr | validation_expr).\
        setParseAction(doc.make_expression)
    prefix_decl = (Keyword('prefix') + prefix_name.setResultsName('id') + la +
        url_word.setResultsName('url') + ra).\
        setParseAction(doc.make_prefix_decl)

    # Document
    document = (Keyword('document') +
        OneOrMore(prefix_decl).setResultsName('prefix_decls') +
        OneOrMore(expression).setResultsName('expression_list') +
        Keyword('endDocument'))
    document.setParseAction(doc.make_document).setParseAction(
        doc.make_document)

    return {"document": document}


if __name__ == "__main__":
    def test(s, nt):
        doc = Document()
        result = bnf(doc)[nt].parseString(s, True)
        print(s, '\n\t=>\n', result)
        print('---------------------')

    def test_file(f, nt):
        doc = Document()
        result = bnf(doc)[nt].parseFile(f, True)
        print(f, '\n\t=>\n', result)
        print('---------------------')

    f = sys.argv[1]
    test_file(f, 'document')
