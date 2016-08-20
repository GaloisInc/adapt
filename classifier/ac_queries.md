# AC Gremlin Queries

## Graph Traversal

Get all segment nodes:
```groovy
g.V().hasLabel('Segment')
```

Get all unclassified segment nodes:
```groovy
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

## Graph Annotation

Add an activity node and link it to a segment X:
```groovy
activity = g.addV(label, 'Activity', 'activity:type', 'Name of Activity in APT Grammar')
g.V(X).next().addEdge('segment:includes', activity)
```
where X is the activity type and Y is the segment identifier.

## Miscellaneous queries

Delete all activities:
```groovy
g.V().has(label, 'Activity').drop().iterate()
```
