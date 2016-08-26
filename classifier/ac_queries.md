# AC Gremlin Queries

## Graph Traversal

Get all segment nodes:
```groovy
g.V().hasLabel('Segment')
```

Get all unclassified segment nodes:
```groovy
<<<<<<< HEAD
g.V().hasLabel('Segment').where(__.not(inE('activity:includes')))
```

Get a segment:
>>>>>>> Added a query for getting all unclassified nodes.
```groovy
=======
>>>>>>> b4a07086e30058268f932ca2a52007f1e1287f41
g.V().hasLabel('Segment').where(__.not(out('segment:includes').hasLabel('Activity')))
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

<<<<<<< HEAD
Get the suspicion score of an activity:
=======
Get the suspiscion score of an activity:
>>>>>>> b4a07086e30058268f932ca2a52007f1e1287f41
```groovy
g.V(X).property('activity:suspicionScore')
```
where X is the activity node.

<<<<<<< HEAD
This property specifies a value from 0 (benign) to 1 (suspicious). It
=======
This property specifies a value from 0 (benign) to 1 (suspicios). It
>>>>>>> b4a07086e30058268f932ca2a52007f1e1287f41
is intended to aid DX in prioritizing interesting symptoms. When in
doubt, a value of 0.1 shall be used.


<<<<<<< HEAD
where X is a comma separated list of segment identifiers.

=======
>>>>>>> b4a07086e30058268f932ca2a52007f1e1287f41
## Graph Annotation

Add an activity node and link it to a segment X:
```groovy
segmentNode = g.V({}).next();
activityNode = graph.addVertex(label, 'Activity', 'activity:type', 'TYPE', 'activity:suspicionScore', SUSPICION);
edge = segmentNode.addEdge('activity:includes', activityNode);
activityNode
```
where TYPE is the activity type and SUSPICION is a real value between
<<<<<<< HEAD
0.0 and 1.0 denoting the activity suspicion score.
=======
0.0 and 1.0 denoting the acitivty suspicion score.
>>>>>>> b4a07086e30058268f932ca2a52007f1e1287f41

## Miscellaneous queries

Delete all activities:
```groovy
g.V().has(label, 'Activity').drop().iterate()
```
