# DX Gremlin Queries

## Graph Traversal

Start the traversal - any segmentation node without ancestors:
```groovy
g.V().hasLabel('Segment').where(__.in('segment:edge').count().is(0))
```

Finding a segment `X`'s successors:
```groovy
g.V(X).out('segment:edge')
```

Finding a segment `X`'s predecessors:
```groovy
g.V(X).in('segment:edge')
```

Finding a segment `X`'s activity labels:
```groovy
g.V(X).out('segment:includes').hasLabel('Activity')
```

## Graph Annotation

Adding APT instance:
```groovy
g.addV(label, 'APT')
```

Adding first Phase to APT instance 'X':
```groovy
phase = g.addV(label, 'Phase', 'phase:name', 'Name of Phase')
g.V(X).next().addEdge('apt:includes', phase, 'phase:order', '1')
```

Adding adding sub-Phase of Phase 'P':
```groovy
phase = g.addV(label, 'Phase', 'phase:name', 'Name of sub-Phase')
g.V(P).next().addEdge('phase:includes', phase, 'phase:order', '1')
```

Adding Activity 'A' to Phase 'P':
```groovy
g.V(P).next().addEdge('phase:includes', 'A', 'phase:order', '1')
```

## Querying Annotations

Get APT instances:
```groovy
g.V().hasLabel('APT')
```

Get Phase instances of APT `X`:
```groovy
g.V(X).out('apt:includes')
```

Get sub-Phases of Phase 'P':
```groovy
g.V(P).out('phase:includes').hasLabel('Phase')
```

Get Activity of Phase 'P':
```groovy
g.V(P).out('phase:includes').hasLabel('Activity')
```
