// graph = TitanFactory.open('cassandra:localhost')
graph = TitanFactory.open('/opt/titan/conf/gremlin-server/titan-cassandra-server.properties')
graph.tx().rollback()
mgmt = graph.openManagement()
if(!mgmt.containsRelationType('EDGE_SUBJECT_HASPARENT_SUBJECT out')) {
    def command = "python /vagrant/adapt/config/titan/GenerateSchema.py"
    def proc = command.execute()
    proc.waitFor()
}
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
    mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.REGISTERED).call()
  }
  mgmt.updateIndex(mgmt.getGraphIndex('byIdent'), SchemaAction.REINDEX).get()
  mgmt.commit()
  mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.ENABLED).call()
} else { mgmt.commit() }

g = graph.traversal()
