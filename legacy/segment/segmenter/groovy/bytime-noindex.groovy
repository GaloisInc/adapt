def addTimeSegments(graph,g,delta) {
  t1 = new Date().getTime();

  segments =  g.V().has('startedAtTime').values('startedAtTime').is(gte(0)).map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime').has(label,neq('Segment')).as('a').values('startedAtTime').is(gte(s).and(lt(s+delta))).select('a');
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
  t2 = new Date().getTime();
  return t2 - t1
}



def addTimeSegmentsFromTo(graph,g,Integer delta,Long start,Long end)  {
  t1 = new Date().getTime();
  start = start - start % delta;
  end = end - end % delta + delta;
  segments =  g.V().has('startedAtTime').values('startedAtTime').is(gte(start).and(lt(end))).map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime').has(label,neq('Segment')).as('a').values('startedAtTime').is(gte(s).and(lt(s+delta))).select('a');
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
  t2 = new Date().getTime();
  return t2 - t1
}



def removeTimeSegments(g) {
  g.V().has(label,'Segment').has('segment:name','byTime').drop().iterate()
}

// ts  = new Long[5]; for (int i = 0; i < 5; i++) { addTimeSegments(graph,g,1000*1000*60); removeTimeSegments(g) } 
