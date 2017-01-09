package com.galois

package object adapt {

  trait DBWritable {
    def asDBKeyValues: List[Any]
  }

}
