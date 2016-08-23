graph = TitanFactory.open('/opt/titan/conf/gremlin-server/titan-cassandra-server.properties')
graph.tx().rollback()

// makeNodeIndex: Makes a node-centric index for fast lookup
// A Node index is a Titan-only index mechanism that supports equality queries
// such as `g.has(indexKey, 'somevalue').next()`
def makeNodeIndex = { String indexName, String indexKey, Boolean mkUnique, indexType ->
    mgmt = graph.openManagement()
    i = mgmt.getGraphIndex(indexName)
    if(! i) {
      idKey = mgmt.getPropertyKey(indexKey)
      idKey = idKey ? idKey : mgmt.makePropertyKey(indexKey).dataType(indexType).make()
      if(mkUnique) {
        mgmt.buildIndex(indexName, Vertex.class).addKey(idKey).unique().buildCompositeIndex()
      } else {
        mgmt.buildIndex(indexName, Vertex.class).addKey(idKey).buildCompositeIndex()
      }
      mgmt.commit()
      graph.tx().commit()

      mgmt  = graph.openManagement()
      idKey = mgmt.getPropertyKey(indexKey)
      idx   = mgmt.getGraphIndex(indexName)
      // Wait for index availability
      if ( idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED) ) {
        mgmt.commit()
        mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.REGISTERED).call()
      } else { mgmt.commit() }

      // enable and wait for enabled status
      mgmt  = graph.openManagement()
      mgmt.updateIndex(mgmt.getGraphIndex(indexName),SchemaAction.ENABLE_INDEX).get()
      mgmt.commit()
      mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.ENABLED).call()
    } else { mgmt.commit() }
}

// Values indexed via an external tool like elastic search provide for a richer query
// lanaugage.  For example, the elastic search query of url's with a regular expression
// is `graph.queryIndex('byURL','v.url:/.*bin.ls.*/').vertices()`
def makeElasticSearchIndex = { String indexName, String indexKey, indexType ->
    mgmt = graph.openManagement()
    i = mgmt.getGraphIndex(indexName)
    if(! i) {
      idKey = mgmt.getPropertyKey(indexKey)
      idKey = idKey ? idKey : mgmt.makePropertyKey(indexKey).dataType(indexType).make()
      mgmt.buildIndex(indexName, Vertex.class).addKey(idKey, Mapping.STRING.asParameter()).buildMixedIndex('search')
      mgmt.commit()
      graph.tx().commit()
    
      mgmt  = graph.openManagement()
      idKey = mgmt.getPropertyKey(indexKey)
      idx   = mgmt.getGraphIndex(indexName)
      // Wait for index availability
      if ( idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED) ) {
        mgmt.commit()
        mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.REGISTERED).call()
      } else { mgmt.commit() }

      mgmt  = graph.openManagement()
      mgmt.updateIndex(mgmt.getGraphIndex(indexName),SchemaAction.ENABLE_INDEX).get()
      mgmt.commit()
      mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.ENABLED).call()
    } else { mgmt.commit() }
}

def makeNumericSearchIndex = { String indexName, String indexKey, indexType ->
    mgmt = graph.openManagement()
    i = mgmt.getGraphIndex(indexName)
    if(! i) {
      idKey = mgmt.getPropertyKey(indexKey)
      idKey = idKey ? idKey : mgmt.makePropertyKey(indexKey).dataType(indexType).make()
      mgmt.buildIndex(indexName, Vertex.class).addKey(idKey).buildMixedIndex('search')
      mgmt.commit()
      graph.tx().commit()
    
      mgmt  = graph.openManagement()
      idKey = mgmt.getPropertyKey(indexKey)
      idx   = mgmt.getGraphIndex(indexName)
      // Wait for index availability
      if ( idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED) ) {
        mgmt.commit()
        mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.REGISTERED).call()
      } else { mgmt.commit() }

      mgmt  = graph.openManagement()
      mgmt.updateIndex(mgmt.getGraphIndex(indexName),SchemaAction.ENABLE_INDEX).get()
      mgmt.commit()
      mgmt.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.ENABLED).call()
    } else { mgmt.commit() }
}


// We index the 'ident' field, which matches CDM 'UUID' but as a Base64 string.
makeNodeIndex('byIdent','ident',true,String.class)
makeNodeIndex('bySegmentName', 'segment:name', false, String.class)
makeNodeIndex('byParentId','segment:parentId',false,Integer.class)
makeNodeIndex('byUrlExact','url',false,String.class)
makeNodeIndex('byDstPort','dstPort',false,Integer.class)
makeNodeIndex('bySrcPort','srcPort',false,Integer.class)
makeNodeIndex('bySrcAddressExact','srcAddress',false,String.class)
makeNodeIndex('byDstAddressExact','dstAddress',false,String.class)
makeNodeIndex('bySubjectType', 'subjectType', false, Short.class)
makeNodeIndex('byEventType', 'eventType', false, Short.class)

// URL index use ElasticSearch which provides richer queries
// including regex
makeElasticSearchIndex('byURL','url',String.class)
makeElasticSearchIndex('bySrcAddress','srcAddress',String.class)
makeElasticSearchIndex('byDstAddress','dstAddress',String.class)

// index PIDs and timestamps for numeric queries
makeNumericSearchIndex('byPID','pid',Integer.class)
makeNumericSearchIndex('byTime','startedAtTime',Long.class)


// The graph traverser captured by variable 'g' is useful to many of
// the insertion and query commands used in normal operation.
g = graph.traversal()
