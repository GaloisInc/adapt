graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();

def startWindow(t,d) { return t - t % d }
def endWindow(t,d) { return t + d - t % d }

def startHour(t) { startWindow(t,1000*1000*60*60) }
def endHour(t) { endWindow(t,1000*1000*60*60) }
def startMin(t) { startWindow(t,1000*1000*60) }
def endMin(t) { endWindow(t,1000*1000*60) }

def getTimes(g) { g.V().has('startedAtTime').values('startedAtTime') }

def addTimeSegmentsFromTo(graph,g,delta) {
 
  segments =  g.V().has('startedAtTime',gte(0)).values('startedAtTime').map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime',gte(s).and(lt(s+delta))).has(label,neq('Segment'));
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
}

def addTimeSegmentsFromTo(graph,g,Integer delta,Long start,Long end} =
  start = start - start % delta;
  end = end - end % delta + delta;
  segments =  g.V().has('startedAtTime',gte(start).and(lt(end))).values('startedAtTime').map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime',gte(s).and(lt(s+delta))).has(label,neq('Segment'));
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
}



def removeTimeSegments(g) {
  g.V().has('segment:name','byTime').has(label,'Segment').drop().iterate()
}

addTimeSegments(graph,g,1000*1000*60)
