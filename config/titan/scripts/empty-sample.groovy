graph = TitanFactory.open('cassandra:localhost')
graph.tx().rollback()
mgmt = graph.openManagement()
i = mgmt.getGraphIndex('byIdent')
if(! i) {
  idKey = mgmt.getPropertyKey('ident')
  idKey = idKey ? idKey : mgmt.makePropertyKey('ident').dataType(String.class).make()
  mgmt.buildIndex('byIdent', Vertex.class).addKey(idKey).buildCompositeIndex()
  mgmt.commit()
  graph.tx().commit()

  // Wait for index availability
  mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.REGISTERED).call()
  mgmt = graph.openManagement()
  mgmt.updateIndex(mgmt.getGraphIndex('byIdent'), SchemaAction.REINDEX).get()
  mgmt.commit()
  mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.ENABLED).call()
} else { mgmt.commit() }

g = graph.traversal()
