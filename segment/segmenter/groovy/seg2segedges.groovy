mgmt=graph.openManagement();
mgmt.makePropertyKey('dest').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('origin').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('segment:startedAtTime').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('segment:endedAtTime').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
mgmt.commit();
mgmt.close()

def createSeg2SegEdgesWithTimestamps(graph,g) {
  t1 = new Date().getTime();
  snodes=g.V().has('segment:name','byPID').id().fold().next();
  for (s in snodes){
     res=g.V(s).as('a').out('segment:includes').as('b').out().as('c').in('segment:includes').as('d').dedup().where(neq('a')).select('a','b','c','d').by(id).toList();
     for (r in res){
        originTimestamps=g.V(r.b).values('startedAtTime','endedAtTime').toList();
        destTimestamps=g.V(r.c).values('startedAtTime','endedAtTime').toList();
        timestamps=(destTimestamps+originTimestamps).sort();
        len=timestamps.size();
        switch(len){
            case 0:
                 g.V(r.a).next().addEdge('segment:edge',g.V(r.d).next(),'origin',r.b,'dest',r.c)
                 break
            case 1:
                 g.V(r.a).next().addEdge('segment:edge',g.V(r.d).next(),'origin',r.b,'dest',r.c,'segment:startedAtTime',timestamps[0])
                 break
            default:
                 g.V(r.a).next().addEdge('segment:edge',g.V(r.d).next(),'origin',r.b,'dest',r.c,'segment:startedAtTime',timestamps[0],'segment:endedAtTime',timestamps[len-1])
                 break
            }
        }
     }
  t2 = new Date().getTime();
  return t2 - t1;
}
