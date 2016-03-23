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


class CommExpr:
    def __init__(self, informed, informant, att_val_list):
        self.informed = informed
        self.informant = informant
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'WasInformedBy({0}, {1}, {2})'.format(
            self.informed, self.informant, self.att_val_dict)


class DerivationExpr:
    def __init__(self, derived, deriverer, att_val_list):
        self.derived = derived
        self.deriverer = deriverer
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'WasDerived({0}, {1}, {2})'.format(
            self.informed, self.informant, self.att_val_dict)


class AssociationExpr:
    def __init__(self, associated, associator, att_val_list, timestamp=None):
        self.associated = associated
        self.associator = associator
        self.timestamp = None if timestamp == '-' else timestamp
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'wasAssociatedWith({0}, {1}, {2}, {3})'.format(
            self.associated, self.associator,
            self.timestamp, self.att_val_dict)


class UsageExpr:
    def __init__(self, user, used, att_val_list, timestamp=None):
        self.user = user
        self.used = used
        self.timestamp = None if timestamp == '-' else timestamp
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'Used({0}, {1}, {2}, {3})'.format(
            self.user, self.used, self.timestamp, self.att_val_dict)


class ValidationExpr:
    def __init__(self, invalidated, invalidator, att_val_list, timestamp=None):
        self.invalidated = invalidated
        self.invalidator = invalidator
        self.timestamp = None if timestamp == '-' else timestamp
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'wasInvalidatedBy({0}, {1}, {2}, {3})'.format(
            self.invalidated, self.invalidator,
            self.timestamp, self.att_val_dict)


class GenerationExpr:
    def __init__(self, generated, generator, att_val_list, timestamp=None):
        self.generated = generated
        self.generator = generator
        self.timestamp = None if timestamp == '-' else timestamp
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'wasGeneratedBy({0}, {1}, {2}, {3})'.format(
            self.generated, self.generator, self.timestamp, self.att_val_dict)


class Agent:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'Agent({0},{1})'.format(self.id, self.att_val_dict)


class Activity:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'Activity({0},{1})'.format(self.id, self.att_val_dict)


class Entity:
    def __init__(self, id_, att_val_list):
        self.id = id_
        self.att_val_dict = dict([(k, v) for (k, v) in att_val_list])

    def __str__(self):
        return 'Entity({0},{1})'.format(self.id, self.att_val_dict)


class PrefixDecl:
    def __init__(self, id_, url):
        self.id = id_
        self.url = url


class Document:
    """
    A program in our language is just a list of functions
    """
    def __init__(self, filename=None, entry_block_label='entry'):
        if filename:
            Parser.bnf(self)['document'].parseFile(filename, True)
            assert self.expression_list != None
            assert self.prefix_decls != None

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
            return UsageExpr(t['user'][0], t['used'][0], t['att_val_list'])
        except:
            return UsageExpr(t['user'][0], t['used'][0], [])

    def make_association_expression(self, t):
        try:
            return AssociationExpr(
                t['associated'][0], t['associator'][0], t['att_val_list'])
        except:
            return AssociationExpr(t['associated'][0], t['associator'][0], [])

    def make_validation_expression(self, t):
        try:
            return ValidationExpr(
                t['invalidated'][0], t['invalidator'][0], t['att_val_list'])
        except:
            return ValidationExpr(t['invalidated'][0], t['invalidator'][0], [])

    def make_generation_expression(self, t):
        try:
            return GenerationExpr(
                t['generated'][0], t['generator'][0], t['att_val_list'])
        except KeyError:
            return GenerationExpr(
                t['generated'][0], t['generator'][0], [])

    def make_communication_expression(self, t):
        try:
            return CommExpr(
                t['informed'][0], t['informant'][0], t['att_val_list'])
        except KeyError:
            return CommExpr(t['informed'][0], t['informant'][0], [])

    def make_derivation_expression(self, t):
        return DerivationExpr(
            t['derived'][0], t['deriverer'][0], t['att_val_list'])

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
    word_with_spaces = Word(alphanums + ' /:_-().,{}[]')
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
    att_val_pair = Combine(att.setDebug() + equal +
        double_quote + word_with_spaces.setResultsName('val').setDebug() + double_quote).\
        setParseAction(doc.make_att_val_pair)

    # Expressions
    generation_expr = (Keyword('wasGeneratedBy') + lpar +
        identifier.setResultsName('generated') + comma +
        identifier.setResultsName('generator') +
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
        identifier.setResultsName('user') + comma +
        identifier.setResultsName('used') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_usage_expression)
    association_expr = (Keyword('wasAssociatedWith') + lpar +
        identifier.setResultsName('associated') + comma +
        identifier.setResultsName('associator') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_association_expression)
    validation_expr = (Keyword('wasInvalidatedBy') + lpar +
        identifier.setResultsName('invalidated') + comma +
        identifier.setResultsName('invalidator') +
        Optional(comma + (timestamp | dash).setResultsName('timestamp')) +
        Optional(
            comma +
            (lbrack + delimitedList(att_val_pair)
                .setResultsName('att_val_list') +
            rbrack) | (lbrack + rbrack)) +
        rpar).setParseAction(doc.make_validation_expression)
    communication_expr = (Keyword('wasInformedBy') + lpar +
        identifier.setResultsName('informed') + comma +
        identifier.setResultsName('informant') + comma +
        ((lbrack + delimitedList(att_val_pair).setResultsName('att_val_list') +
        rbrack) | (lbrack + rbrack)) + rpar).\
        setParseAction(doc.make_communication_expression)
    derivation_expr = (Keyword('wasDerivedFrom') + lpar +
        identifier.setResultsName('derived') + comma +
        identifier.setResultsName('deriverer') + comma +
        ((lbrack + delimitedList(att_val_pair).setResultsName('att_val_list') +
        rbrack) | (lbrack + rbrack)) + rpar).\
        setParseAction(doc.make_derivation_expression)
    expression = (association_expr | activity_expr | communication_expr | entity_expr | usage_expr | generation_expr | derivation_expr | agent_expr | validation_expr).\
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
        print s, '\n\t=>\n', result
        print '---------------------'

    def test_file(f, nt):
        doc = Document()
        result = bnf(doc)[nt].parseFile(f, True)
        print f, '\n\t=>\n', result
        print '---------------------'

    f = sys.argv[1]
    test_file(f, 'document')
