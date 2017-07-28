import string

class Px:

    # Placeholders for enums
    SUBJECT_PROCESS = 0
    EVENT_READ = 16
    PROCESS_READS_FILE = 0

    def __init__(self, db):
        self.db = db

    def findProcessReadsFile(self):
        # Search DB for:
        # 1. Subject vertex with prov-tc:subjectType = SUBJECT_PROCESS (0) connected by an edge to a Subject vertex with prov-tc:subjectType = SUBJECT_EVENT (4) and prov-tc:eventType = EVENT_READ (16) and connected by an edge to Entity-File vertex
        paths = self.db.query("g.V().hasLabel('Subject').has('prov-tc:subjectType', " + str(self.SUBJECT_PROCESS) + ").out().hasLabel('Subject').has('prov-tc:subjectType', " + str(self.EVENT_READ) + ").out().hasLabel('Entity-File').path()")
        self.addPattern(self.PROCESS_READS_FILE, paths)
        # 2. Subject vertex with prov-tc:subjectType = SUBJECT_PROCESS (0) connected by an "prov:wasGeneratedBy out" edge to a PRov:wasGeneratedBy vertex to a "prov:wasGeneratedBy in" edge to an Entity-File vertex or prov:used
        paths = self.db.query("g.V().hasLabel('Subject').has('prov-tc:subjectType', " + str(self.SUBJECT_PROCESS) + ").out('prov:wasGeneratedBy out', 'prov:used out').out('prov:wasGeneratedBy in', 'prov:used in').hasLabel('Entity-File').path()")
        self.addPattern(self.PROCESS_READS_FILE, paths)

    def addPattern(self, patternID, paths):
        # For each matching set, insert a pattern vertex with prov-tc:patternID = ProcReadsFile and "prov-tc:partOfPattern out" edge, prov-tc:partOfPattern vertex, "prov-tc:partOfPattern in" edge to each vertex in the pattern
        # Path will be of the form [[v[id11], v[id12], v[id13]], [v[id21], v[id22], v[id23]], ...]
        pathsVec = paths.rstrip("]").split("]]")
        for path in pathsVec:
            vertices = path.lstrip("[[v[").lstrip(", [v[").split("], v[")
            vid = self.db.addVertex("label, 'Pattern', 'prov-tc:patternID', " + str(patternID))
            for vertex in vertices:
                self.db.query("g.addV(label, 'prov-tc:partOfPattern').addInE('prov-tc:partOfPattern out', g.V(" + str(vertex) + ")).addOutE('prov-tc:partOfPattern in', g.V(" + str(vid) + "))")

