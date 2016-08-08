:remote connect tinkerpop.server /opt/titan/conf/remote-objects.yaml 

:> def startWindow(t,d) { return t - t % d }
:> def endWindow(t,d) { return t + d - t % d }

def startHour(t) { startWindow(t,1000*1000*60*60) }
def endHour(t) { endWindow(t,1000*1000*60*60) }
def startMin(t) { startWindow(t,1000*1000*60) }
def endMin(t) { endWindow(t,1000*1000*60) }

:> def getTimes(g) { g.V().has('startedAtTime').values('startedAtTime').is(gt(0)) }

script1 = """
def addTimeSegments(g,delta) {
  segments = getTimes(g).map{startWindow((long)it.get(),delta)}.dedup()
  for(s in segments) {
    g.addV(label,'Segment','segment:name','byTime').next()
  }
}
"""

script2 = """
def addTimeSegments(g,delta) {
  segments = getTimes(g).map{startWindow((long)it.get(),delta)}.dedup()
  for(s in segments) {
    g.addV(label,'Segment','segment:name','byTime','startedAtTime',s).next()
  }
}
"""

:> @script1

script3 = """
def removeSegments(g) {
  g.V().has(label,'Segment').has('segment:name','byTime').drop().iterate()
}
"""

:> @script3