from src.px import Px

class FakeDB:
    def __init__(self):
        self.currV = 0

    def query(self, query):
        if query == "g.V().hasLabel('Subject').has('prov-tc:subjectType', 0).out().hasLabel('Subject').has('prov-tc:subjectType', 16).out().hasLabel('Entity-File').path()":
            return "[[v[1], v[10], v[100]], [v[2], v[20], v[200]], [v[3], v[30], v[300]]]"
        elif query == "g.V().hasLabel('Subject').has('prov-tc:subjectType', 0).out('prov:wasGeneratedBy out', 'prov:used out').out('prov:wasGeneratedBy in', 'prov:used in').hasLabel('Entity-File').path()":
            return "[[v[4], v[40], v[400]]]"
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(1)).addOutE('prov-tc:partOfPattern in', g.V(1000))":
            return 'v[1001]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(10)).addOutE('prov-tc:partOfPattern in', g.V(1000))":
            return 'v[1002]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(100)).addOutE('prov-tc:partOfPattern in', g.V(1000))":
            return 'v[1003]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(2)).addOutE('prov-tc:partOfPattern in', g.V(2000))":
            return 'v[2001]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(20)).addOutE('prov-tc:partOfPattern in', g.V(2000))":
            return 'v[2002]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(200)).addOutE('prov-tc:partOfPattern in', g.V(2000))":
            return 'v[2003]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(3)).addOutE('prov-tc:partOfPattern in', g.V(3000))":
            return 'v[3001]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(30)).addOutE('prov-tc:partOfPattern in', g.V(3000))":
            return 'v[3002]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(300)).addOutE('prov-tc:partOfPattern in', g.V(3000))":
            return 'v[3003]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(4)).addOutE('prov-tc:partOfPattern in', g.V(4000))":
            return 'v[4001]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(40)).addOutE('prov-tc:partOfPattern in', g.V(4000))":
            return 'v[4002]'
        elif query == "g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(400)).addOutE('prov-tc:partOfPattern in', g.V(4000))":
            return 'v[4003]'
        else:
            raise Exception("Unexpected query", query)

    def addVertex(self, query):
        if query == "label, 'Pattern', 'prov-tc:patternID', 0":
            self.currV = self.currV + 1000
            return self.currV
        else:
            raise Exception("Unexpected query", query)


db = FakeDB()
px = Px(db)
px.findProcessReadsFile()
print "Process reads file test pass"
