import asyncio
from aiogremlin import GremlinClient
import sys


url='http://localhost:8182/'
loop = asyncio.get_event_loop()
gc = GremlinClient(url=url, loop=loop)


def run_query(query, bindings={}):
    execute = gc.execute(query, bindings=bindings)
    result = loop.run_until_complete(execute)
    return result[0].data


# insert segment nodes and connect them with the subgraphs
def _insert_node(label, vertex_type, *av_pairs):
    bindings = {'p1': label, 'p2': vertex_type}
    gremlin = ("graph.addVertex(label, p1, 'vertexType', p2")
    for (attr, val) in av_pairs:
        gremlin += ", '%s', '%s'" % (attr, val)
    gremlin += ')'
    return run_query(gremlin, bindings)

idx = [i for i in range(1,102,10)]
def insert_dummy_segment_nodes():
    print("Inserting dummy segment nodes")
    for i in range(0,10):
        _insert_node('seg_' + str(i), 'segment', ('rangeStart', idx[i]), ('rangeEnd', idx[i+1]))

    for i in range(0,10):
        vertices = run_query("g.V().range(" + str(idx[i]) + "," + str(idx[i+1]) + ")")
        query = "v=g.V().hasLabel('seg_" + str(i) + "')[0];"
        for id in [v['id'] for v in vertices]:
            query += "v.addEdge('partOfSeg', graph.vertices(" + str(id) + ")[0], 'segid'," + str(i) + ");"
        run_query(query)
    print("Finished Inserting dummy segment nodes")

# feature extraction queries
f_h = ['EVENT_READ', 'EVENT_WRITE', 'EVENT_EXECUTE', 'NUM_FILES', 'NUM_SUBJECTS']
f_q = [	"has('eventType',   16).count()", # EVENT_READ
        "has('eventType',   17).count()", # EVENT_WRITE
        "has('eventType',    8).count()", # EVENT_EXECUTE
        "has('type',    'file').count()", # NUM_FILES
        "has('type', 'subject').count()"] # NUM_SUBJECTS

def extract_and_attach_features():
    print("Extracting features from segments")
    # get number segment vertices
    n_seg_v = run_query("g.V().has('vertexType','segment').count()")[0]
    # for all segment vertices extract and attach features 
    for i in range(0,n_seg_v):
        print("Segment " + str(i))
        for j in range(0,len(f_h)):
            val = run_query("g.V().has('vertexType','segment').hasLabel('seg_" + str(i) + "').out()." + f_q[j])[0]
            run_query("g.V().has('vertexType','segment').hasLabel('seg_" + str(i) + "')[0].property('" + f_h[j] + "'," + str(val) + ")")
    print("Feature extraction and attaching finished")

def write_features_to_file(out_file):
    print("Writing features to file: "+ out_file)
    f = open(out_file, "w")
    f.write("segment_id,segment_type,segment_type_instance")
    for h in f_h:
        f.write("," + h)
    f.write("\n")
    n_seg_v = run_query("g.V().has('vertexType','segment').count()")[0]
    for i in range(0,n_seg_v):
        f.write("seg_" + str(i) + ",VRangeType," + str(idx[i]) + "-" + str(idx[i+1]))
        for j in range(0,len(f_h)):
            f.write("," + str(run_query("g.V().has('vertexType','segment').hasLabel('seg_" + str(i) + "')[0].values('" + f_h[j] + "')")[0]))
        f.write("\n")    
    f.close()
    print("Writing Finished")



in_file = sys.argv[1]
# main
insert_dummy_segment_nodes()
extract_and_attach_features()
write_features_to_file(in_file)
loop.run_until_complete(gc.close())
loop.close()
