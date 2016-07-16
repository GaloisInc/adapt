
mgmt=graph.openManagement();if (mgmt.getEdgeLabel('segment:includes')==null) {test=mgmt.makeEdgeLabel('segment:includes').make();mgmt.commit();mgmt.close()}
idWithProp=g.V().has('pid').has(label,neq('Segment')).id().fold().next(); existingSegNodes_parentIds=g.V().hasLabel('Segment').values('parentVertexId').fold().next(); ;idsToStore=idWithProp-existingSegNodes_parentIds; for (i in idWithProp) {sub=g.V(i).repeat(__.bothE().subgraph('sub').bothV().has(label,neq('Segment'))).times(2).cap('sub').next();subtr=sub.traversal(); if (i in idsToStore) {s=graph.addVertex(label,'Segment','segment:name','s'+i.toString(),'pid',g.V(i).values('pid').next(),'parentVertexId',i)} else {s=g.V().hasLabel('Segment').has('parentVertexId',i).next()}; idNonLinkedNodes=subtr.V().id().fold().next()-g.V().hasLabel('Segment').has('parentVertexId',i).outE('segment:includes').inV().id().fold().next();for (node in idNonLinkedNodes) {s.addEdge('segment:includes',g.V(node).next())}}



// Always, be kind to the Gentle Reader.

mgmt = graph.openManagement()
if (mgmt.getEdgeLabel('segment:includes') == null) {
    test = mgmt.makeEdgeLabel('segment:includes').make()
    mgmt.commit()
    mgmt.close()
}
idWithProp = g.V().has('pid').has(label, neq('Segment')).id().fold().next()
existingSegNodes_parentIds = g.V().hasLabel('Segment').values('parentVertexId').fold().next()
idsToStore = idWithProp - existingSegNodes_parentIds
for (i in idWithProp) {
    sub = g.V(i).repeat(__.bothE().subgraph('sub').bothV().has(label,neq('Segment'))).times(2).cap('sub').next()
    subtr = sub.traversal()
    if (i in idsToStore) {
        s = graph.addVertex(label, 'Segment',
                           'segment:name', 's' + i.toString(),
                           'pid', g.V(i).values('pid').next(),
                           'parentVertexId', i)
    } else {
        s = g.V().hasLabel('Segment').has('parentVertexId',i).next()
    }
    idNonLinkedNodes = subtr.V().id().fold().next()
        - g.V().hasLabel('Segment').has('parentVertexId',i).outE('segment:includes').inV().id().fold().next()
    for (node in idNonLinkedNodes) {
        s.addEdge('segment:includes',g.V(node).next())
    }
}
