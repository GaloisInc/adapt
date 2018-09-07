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
  trait DBNodeable[+EdgeType] extends DBWritable {
    // All nodes in the graph have a UUID, even if our current DB (Titan) doesn't support using that
    // as the internal ID.
    def getUuid: UUID

    // Find the host ID
    def getHostId: Option[UUID] = None

    // Outgoing edges coming off the node. The key is the label on the edge, the UUID the node
    // (which should be 'DBNodeable' too) the edge goes to.
    def asDBEdges: List[(EdgeType, UUID)]

    // Some CDM statements translate to more than one node. We put extra nodes into 'supportNodes'
    def supportNodes: List[(UUID, List[Any], List[(EdgeType, UUID)])] = List()
  }


  implicit class StringSetMethods(val set: Set[String]) extends AnyVal {
    def suffixFold: Set[String] = set.foldLeft(Set.empty[String])((acc, p) => if ((acc ++ set.-(p)).exists(s => s.endsWith(p))) acc else acc + p)
  }

  def time[A](name: String)(f: => A) = {
    val t0 = System.nanoTime
    val ans = f
    printf(s"$name elapsed: %.6f sec\n",(System.nanoTime-t0)*1e-9)
    ans
  }
}
