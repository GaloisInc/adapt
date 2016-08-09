graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();

def startWindow(t,d) { return t - t % d }
def endWindow(t,d) { return t + d - t % d }

def startHour(t) { startWindow(t,1000*1000*60*60) }
def endHour(t) { endWindow(t,1000*1000*60*60) }
def startMin(t) { startWindow(t,1000*1000*60) }
def endMin(t) { endWindow(t,1000*1000*60) }

def getTimes(g) { g.V().has('startedAtTime').values('startedAtTime').is(gt(0)) }

def addTimeSegments(graph,g,delta) {
  segments =  g.V().has('startedAtTime')
               .values('startedAtTime')
               .is(gt(0))
               .map{t = it.get(); t - t % delta}.dedup().order();
  vtime = 0;
  v = graph.addVertex(label,'Segment',
                      'segment:name','byTime',
                      'startedAtTime',0);
  content = g.V().has('startedAtTime').has(label,neq('Segment')).as('a').values('startedAtTime').is(gte(vtime).and(lt(vtime+delta))).select('a');
  for(z in content) {
    v.addEdge('segment:includes',z) 
  }    
  for(s in segments) {
    w = graph.addVertex(label,'Segment',                                                                'segment:name','byTime',
                        'startedAtTime',s);
    v.addEdge('segment:edge',w);
 
    v = w;
    vtime = s
    content = g.V().has('startedAtTime').has(label,neq('Segment')).as('a').values('startedAtTime').is(gte(vtime).and(lt(vtime+delta))).select('a');
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
  
}



def removeTimeSegments(g) {
  g.V().has(label,'Segment').has('segment:name','byTime').drop().iterate()
}

addTimeSegments(graph,g,1000*1000*60)
