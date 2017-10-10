package com.galois

import java.util.UUID

package object adapt {

  // Anything that can be converted into properties on a node
  trait DBWritable {
    // Returns an (even-length) list alternating between the string label of the property and its
    // value.
    def asDBKeyValues: List[(String, Any)]
  }

  // Anything that corresponds to a node in the graph
  trait DBNodeable extends DBWritable {
    // All nodes in the graph have a UUID, even if our current DB (Titan) doesn't support using that
    // as the internal ID.
    def getUuid: UUID

    def getLabels: List[String]

    // Outgoing edges coming off the node. The key is the label on the edge, the UUID the node
    // (which should be 'DBNodeable' too) the edge goes to.
    def asDBEdges: List[(cdm17.CDM17.EdgeTypes.EdgeTypes,UUID)]

    // Some CDM statements translate to more than one node. We put extra nodes into 'supportNodes'
    def supportNodes: List[(UUID, List[Any], List[(cdm17.CDM17.EdgeTypes.EdgeTypes,UUID)])] = List()
  }

  type ProcessUUID = UUID
  type FileUUID = UUID
  type MemoryUUID = UUID
  type NetFlowUUID = UUID
  type EventUUID = UUID
  type PredicateUUID = UUID
}
