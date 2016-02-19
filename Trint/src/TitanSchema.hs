{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# OPTIONS_GHC -Wall #-}
module TitanSchema
where
import System.IO (openFile, IOMode(WriteMode), hPutStr, hClose)

data Multiplicity = MULTI | SIMPLE | MANY2ONE | ONE2MANY | ONE2ONE
     deriving (Show, Eq)

data DataType = STRING | CHARACTER | BOOLEAN | BYTE
              | SHORT | INTEGER | LONG | FLOAT | DOUBLE | DECIMAL | PRECISION
              | DATE | GEOSHAPE | UUID
     deriving (Show,Eq)


data Cardinality = SINGLE | LIST | SET
     deriving (Show, Eq)

data Edge = Edge { elabel :: String,
                   multiplicity :: Multiplicity}
                   
data Vertex = Vertex { vlabel :: String }

data PropertyKey = PropertyKey { klabel :: String,
                                 dataType :: DataType,
                                 cardinality :: Cardinality}

data Schema = Schema { vertices :: [Vertex],
                          edges :: [Edge],
                          propertyKeys :: [PropertyKey]}


class Emit a where
  emit :: a -> String

instance Emit Multiplicity where
  emit x = show x

instance Emit Cardinality where
  emit x = show x

instance Emit DataType where
  emit STRING = "String"
  emit CHARACTER = "Character"
  emit BOOLEAN = "Boolean"
  emit BYTE = "Byte"
  emit SHORT = "Short"
  emit INTEGER = "Integer"
  emit LONG = "Long"
  emit FLOAT = "Float"
  emit DOUBLE = "Double"
  emit DECIMAL = "Decimal"
  emit PRECISION = "Precision"
  emit DATE = "Date"
  emit GEOSHAPE = "Geoshape"
  emit UUID = "UUID"


remote :: String -> String
remote cmd = ":> mgmt = graph.openManagement() ; " ++ cmd ++ " ; mgmt.commit()\n"

instance Emit Vertex where
  emit v = remote ("mgmt.makeVertexLabel('" ++ vlabel v ++ "').make()")

instance Emit Edge where
  emit e = remote ("mgmt.makeEdgeLabel('" ++ elabel e ++ "').multiplicity("++ emit(multiplicity e) ++").make()")

instance Emit PropertyKey where
  emit k = remote ("mgmt.makePropertyKey('" ++ klabel k ++ "').dataType(" ++ emit (dataType k) ++  ".class).cardinality("++ emit(cardinality k ) ++").make()")

instance Emit Schema where
  emit schema = concat (map emit (vertices schema) ++ map emit (edges schema) ++ map emit (propertyKeys schema))


adaptSchema :: Schema
adaptSchema = Schema  [Vertex "segment"]
                      [Edge "wasAssociatedWith" SIMPLE,
                       Edge "used" SIMPLE,
                       Edge "wasStartedBy" SIMPLE,
                       Edge "wasGeneratedBy" SIMPLE,
                       Edge "wasEndedBy" SIMPLE,
                       Edge "wasAttributedTo" SIMPLE,
                       Edge "wasDerivedFrom" SIMPLE,
                       Edge "actedOnBehalfOf" SIMPLE,
                       Edge "wasInvalidatedBy" SIMPLE,
                       Edge "segmentContains" SIMPLE]
                      [PropertyKey "vertexType" STRING SINGLE,
                       PropertyKey "name" STRING SINGLE,
                       PropertyKey "user" STRING SINGLE,
                       PropertyKey "machine" STRING SINGLE,
                       PropertyKey "PID" STRING SINGLE,
                       PropertyKey "PPID" STRING SINGLE,
                       PropertyKey "started" DATE SINGLE,
                       PropertyKey "hadPrivs" STRING SINGLE,
                       PropertyKey "PWD" STRING SINGLE,
                       PropertyKey "ended" DATE SINGLE,
                       PropertyKey "group" STRING SINGLE,
                       PropertyKey "commandLine" STRING SINGLE,
                       PropertyKey "source" STRING SINGLE,
                       PropertyKey "programName" STRING SINGLE,
                       PropertyKey "CWD" STRING SINGLE,
                       PropertyKey "UID" STRING SINGLE,
                       PropertyKey "type" STRING SINGLE,
                       PropertyKey "registryKey" STRING SINGLE,
                       PropertyKey "coarseLoc" STRING SINGLE,
                       PropertyKey "fineLoc" STRING SINGLE,
                       PropertyKey "created" DATE SINGLE,
                       PropertyKey "version" STRING SINGLE,
                       PropertyKey "deleted" DATE SINGLE,
                       PropertyKey "owner" STRING SINGLE,
                       PropertyKey "size" STRING SINGLE,
                       PropertyKey "taint" STRING SET,
                       PropertyKey "atTime" DATE SINGLE,
                       PropertyKey "startTime" DATE SINGLE,
                       PropertyKey "endTime" DATE SINGLE,
                       PropertyKey "genOp" STRING SINGLE,
                       PropertyKey "returnVal" STRING SINGLE,
                       PropertyKey "operation" STRING SINGLE,
                       PropertyKey "args" STRING LIST,
                       PropertyKey "cmd" STRING SINGLE,
                       PropertyKey "deriveOp" STRING SINGLE,
                       PropertyKey "execOp" STRING SINGLE,
                       PropertyKey "machineId" STRING SINGLE,
                       PropertyKey "sourceAddress" STRING SINGLE,
                       PropertyKey "destinationAddress" STRING SINGLE,
                       PropertyKey "sourcePort" STRING SINGLE,
                       PropertyKey "destinationPort" STRING SINGLE,
                       PropertyKey "protocol" STRING SINGLE,
                       PropertyKey "permissions" STRING LIST
                       ]
                       

main :: IO ()
main = do h <- openFile "adaptSchema.gremlin" WriteMode
          hPutStr h (emit adaptSchema)
          hClose h