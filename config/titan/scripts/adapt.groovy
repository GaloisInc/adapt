graph = TitanFactory.open('/opt/titan/conf/gremlin-server/titan-cassandra-server.properties')
graph.tx().rollback()
mgmt = graph.openManagement()
i = mgmt.getGraphIndex('byIdent')
if(! i) {
  idKey = mgmt.getPropertyKey('ident')
  idKey = idKey ? idKey : mgmt.makePropertyKey('ident').dataType(String.class).make()
  mgmt.buildIndex('byIdent', Vertex.class).addKey(idKey).buildCompositeIndex()
  mgmt.commit()
  graph.tx().commit()

  mgmt  = graph.openManagement()
  idKey = mgmt.getPropertyKey('ident')
  idx   = mgmt.getGraphIndex('byIdent')
  // Wait for index availability
  if ( idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED) ) {
    mgmt.commit()
    mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.REGISTERED).call()
  } else { mgmt.commit() }
  mgmt  = graph.openManagement()
  mgmt.updateIndex(mgmt.getGraphIndex('byIdent'),SchemaAction.ENABLE_INDEX).get()
  mgmt.commit()
  mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.ENABLED).call()
} else { mgmt.commit() }

// URL index
mgmt = graph.openManagement()
i = mgmt.getGraphIndex('byURL')
if(! i) {
  urlKey = mgmt.getPropertyKey('url')
  urlKey = urlKey ? urlKey : mgmt.makePropertyKey('url').dataType(String.class).make()
  mgmt.buildIndex('byURL', Vertex.class).addKey(urlKey, Mapping.STRING.asParameter()).buildMixedIndex('search')
  mgmt.commit()
  graph.tx().commit()

  mgmt  = graph.openManagement()
  urlKey = mgmt.getPropertyKey('url')
  idx   = mgmt.getGraphIndex('byURL')
  // Wait for index availability
  if ( idx.getIndexStatus(urlKey).equals(SchemaStatus.INSTALLED) ) {
    mgmt.commit()
    mgmt.awaitGraphIndexStatus(graph, 'byURL').status(SchemaStatus.REGISTERED).call()
  } else { mgmt.commit() }
  mgmt  = graph.openManagement()
  mgmt.updateIndex(mgmt.getGraphIndex('byURL'),SchemaAction.ENABLE_INDEX).get()
  mgmt.commit()
  mgmt.awaitGraphIndexStatus(graph, 'byURL').status(SchemaStatus.ENABLED).call()
} else { mgmt.commit() }

g = graph.traversal()
