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
reindex(graph,'byParentId')
reindex(graph,'byURL')
reindex(graph,'byPID')
reindex(graph,'byTime')
reindex(graph,'byUrlExact')
reindex(graph,'byDstPort')
reindex(graph,'bySrcPort')
reindex(graph,'bySrcAddressExact')
reindex(graph,'byDstAddressExact')
reindex(graph,'bySrcAddress')
reindex(graph,'byDstAddress')
