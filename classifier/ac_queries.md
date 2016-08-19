# AC Gremlin Queries

## Graph Traversal

Get all segment nodes:
```groovy
g.V().hasLabel('Segment')
```

Get all unclassified segment nodes:
```groovy
g.V().hasLabel('Segment').where(__.not(inE('activity:includes')))
```

Get a segment:
```groovy
g.V(X).out('segment:includes')
```

Get all activity nodes:
```groovy
g.V().has(label, 'Activity')
```

Get the activity nodes associated with a list of segments nodes:
```groovy
g.V(X).in('activity:includes')
```

where X is a comma separated list of segment identifiers.

## Graph Annotation

Add an activty node and link it to a segment:
```groovy
graph.addVertex(label, 'Activity', 'activity:type', X).addEdge('activity:includes', g.V(Y).next())
```
where X is the activity type and Y is the segment identifier.

## Miscellaneous queries

Delete all activities:
```groovy
g.V().has(label, 'Activity').drop().iterate()
```
