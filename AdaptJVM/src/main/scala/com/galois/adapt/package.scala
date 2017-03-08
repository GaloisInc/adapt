package com.galois

import java.util.UUID

package object adapt {

  trait DBWritable {
    def getUuid: UUID
    def asDBKeyValues: List[Any]
    def asDBEdges: List[(String,UUID)]
  }
}
