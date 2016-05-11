import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem

// Create the base graph and traversal
graph = TitanFactory.open('cassandra:localhost')

// Delete the old index if it exists.
// XXX This doesn't work for unknown reasons
// graph.tx().rollback()
// mgmt = graph.openManagement()
// i = mgmt.getGraphIndex('byIdent')
// if(i) {
// 	println("[Deleting index] 1. Disabling")
// 	ui = mgmt.updateIndex(i, SchemaAction.DISABLE_INDEX)
// 	if (ui) { ui.get() }
// 	mgmt.commit()
// 	ManagementSystem.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.DISABLED).call()
// 	println("[Deleting index] 2. Removing")
// 	mgmt = graph.openManagement()
// 	i = mgmt.getGraphIndex('byIdent')
// 	mgmt.updateIndex(i, SchemaAction.REMOVE_INDEX).get()
// 	mgmt.commit()
// } else {
// 	println("[Deleting index] No index to delete")
// 	mgmt.commit()
// }
// graph.tx().commit()

// Create the index
mgmt = graph.openManagement()
i = mgmt.getGraphIndex('byIdent')
if(! i) {
	idKey = mgmt.getPropertyKey('ident')
	idKey = idKey ? idKey : mgmt.makePropertyKey('ident').dataType(String.class).make()
	mgmt.buildIndex('byIdent',Vertex.class).addKey(idKey).buildCompositeIndex()
	i = mgmt.getGraphIndex('byIdent')
	ui = mgmt.updateIndex(i, SchemaAction.ENABLE_INDEX)
	if(ui) { ui.get() }

	// Reindex the graph
	mgmt = graph.openManagement()
	mgmt.awaitGraphIndexStatus(graph, 'byIdent').call()
	i = mgmt.getGraphIndex('byIdent')
	ui = mgmt.updateIndex(i, SchemaAction.REINDEX)
	if(ui) { ui.get() }
	mgmt.commit()
}
mgmt.commit()

// Block until the SchemaStatus is ENABLED
mgmt = graph.openManagement()
mgmt.awaitGraphIndexStatus(graph, 'byIdent').status(SchemaStatus.ENABLED).call()
mgmt.commit()

g = graph.traversal()
