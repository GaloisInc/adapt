# AC Gremlin Queries

## Graph Traversal

Get all segment nodes:
```groovy
g.V().hasLabel('Segment')
```

Get all unclassified segment nodes:
```groovy
g.V().hasLabel('Segment').where(__.not(outE('activity:includes')))
```

Get segment X:
```groovy
g.V(X)
```

Get all activity nodes:
```groovy
g.V().has(label, 'Activity')
```

Get the activity nodes associated with a list of segments nodes:
```groovy
g.V(X).out('activity:includes')
```
where X is a comma separated list of segment identifiers.

Get the suspiscious score of an activity A:
```groovy
g.V(A).property('activity:suspicionScore')
```

This property specifies a value from 0 (benign) to 1 (evil). It is intended to aid DX in prioritizing interesting symptoms. When in doubt, a value of 0.1 shall be used.


## Graph Annotation

Add an activity node and link it to a segment X:
```groovy
segmentNode = g.V({}).next();
activityNode = graph.addVertex(label, 'Activity', 'activity:type', 'TYPE', 'activity:suspicionScore', SUSPICION);
edge = segmentNode.addEdge('activity:includes', activityNode);g.V(X).next().addEdge('segment:includes', activity);
activityNode
```

## Miscellaneous queries

Delete all activities:
```groovy
g.V().has(label, 'Activity').drop().iterate()
```
