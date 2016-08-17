def reindex(graph,idxName) {
  graph.tx().rollback()
  mgmt = graph.openManagement()
  i = mgmt.getGraphIndex(idxName)
  mgmt.updateIndex(i, SchemaAction.REINDEX)
  mgmt.commit()
  mgmt.awaitGraphIndexStatus(graph, idxName).status(SchemaStatus.ENABLED).call()
}


reindex(graph,'byIdent')
reindex(graph,'bySegmentName')
reindex(graph,'byURL')
reindex(graph,'byPID')
reindex(graph,'byTime')
