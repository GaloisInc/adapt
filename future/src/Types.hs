{-# LANGUAGE DeriveDataTypeable #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE DeriveDataTypeable         #-}
module Types
    ( -- * Types
      Error(..)
    , ParseError(..)
    , TypeError(..)
    , TranslateError(..)
    , Warning(..), ppWarning
      -- * Key types
    , Type(..)
    , Stmt(..)
    , Entity(..)
    , ModVec(..), UUID(..), MID, DevID, AgentAttr(..), ArtifactAttr(..), UoeAttr(..)
    , Predicate(..) , PredicateAttr(..), PredicateType(..), DevType(..)
    , Version, CoarseLoc, FineLoc
    , UseOp, GenOp, DeriveOp, ExecOp, PID(..), ArtifactType
    , Time, EntryPoint
    , Text
    ) where

import           Data.Monoid
import           Data.Data
import           Data.IntMap (IntMap)
import qualified Data.IntMap as IntMap
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Char (toLower)
import           Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import           Data.Word (Word64)
import qualified Control.Exception as X
import           ParserCore (ParseError(..))

import ParserCore (Time)

data Error      = PE ParseError | TCE TypeError | TRE TranslateError deriving (Eq, Ord, Show, Data, Typeable)
data Warning    = Warn Text
  deriving (Eq, Ord, Show, Data, Typeable)

ppWarning :: Warning -> Text
ppWarning (Warn w) = w

data TypeError  = TypeError Type Type | CanNotInferType Text
        deriving (Data, Eq, Ord, Show, Read)
data TranslateError = MissingRequiredField (Maybe Text) Text | TranslateError Text
        deriving (Data, Eq, Ord, Show, Read)

instance X.Exception TypeError
instance X.Exception ParseError
instance X.Exception Error

--------------------------------------------------------------------------------
-- The below types encode the diagram found in the "TA1 Data Conceptual Model".
-- However, the types do not enforce proper use; predicates are not
-- inherently affiliated with Entities and there is no type-level
-- enforcement of the fan-in fan-out.

data Stmt = StmtEntity Entity | StmtPredicate Predicate
  deriving (Data, Eq, Ord, Show)

-- | Entities in our conceptual Model
-- Notice each entity has a name (Text field) except for metadata.
data Entity = Agent Text [AgentAttr]
            | UnitOfExecution Text [UoeAttr]
            | Artifact Text [ArtifactAttr]
            | Resource Text DevType (Maybe DevID)
            | Metadata Text Type Text -- Key, Type, Value
  deriving (Eq, Ord, Show, Data)

nameOf :: Entity -> Text
nameOf e = case e of
            Agent n _ -> n
            UnitOfExecution n _ -> n
            Artifact n _    -> n
            Resource n _ _      -> n
            Metadata _ _ _      -> ""

-- | Machine ID
type MID = UUID

-- | Attributes of Agents
data AgentAttr = AAName Text | AAUser Text | AAMachine MID
  deriving (Data, Eq, Ord, Show)

-- | Attributes of units of execution
data UoeAttr = UAUser Text
             | UAPID PID
             | UAMachine MID
             | UAStarted Time
             -- XXX All the below constructors are unused, they lack any
             -- translation path from ProvN, see Translate.hs
             | UAEnded Time
             | UAHadPrivs Privs
             | UAGroup Text
  deriving (Data, Eq, Ord, Show)

-- | Attributes of Artifacts
data ArtifactAttr = ArtAType ArtifactType
                  | ArtARegistryKey Text
                  -- XXX All the below constructors are unused, they lack
                  -- any translation path from ProvN, see Translate.hs
                  | ArtACoarseLoc CoarseLoc
                  | ArtAFineLoc FineLoc
                  | ArtACreated Time
                  | ArtAVersion Version
                  | ArtADeleted Time
                  | ArtAOwner Text
                  | ArtASize Integer
                  | Taint Word64
  deriving (Data, Eq, Ord, Show)

-- | XXX TBD
newtype Privs = Privs Word64
  deriving (Data, Eq, Ord, Show, Num)

type DevID = Text

type Version = Text

-- | FineLoc is specified as a offset, packet id, or memory address.  We
-- currently capture these only as a raw string.
type FineLoc   = Text

-- | CoarseLoc is specified as a file path, packet or page of memory.  This
-- has proven insufficient if it is a required artifact field (ex: what is the
-- "location" of a socket?") so we currently model this as a raw string.
type CoarseLoc = Text


data Predicate = Predicate { predSubject    :: Text
                           , predObject     :: Text
                           , predType       :: PredicateType
                           , predAttrs      :: [PredicateAttr]
                           }
  deriving (Data, Eq, Ord, Show)

data PredicateAttr
        = Raw Text Text
        | AtTime Time
        | StartTime Time
        | EndTime Time
        | GenOp GenOp
        | Permissions ModVec
        | ReturnVal Text
        | Args [Text]
        | Cmd Text
        | DeriveOp DeriveOp
  deriving (Data, Eq, Ord, Show)

data PredicateType
            = ActedOnBehalfOf
            | WasAssociatedWith
            | WasStartedBy
            | WasEndedBy
            | WasInformedBy
            | Used
            | WasGeneratedBy
            | WasAttributedTo
            | WasInvalidatedBy
            | WasDerivedFrom
            | Description
            | IsPartOf
  deriving (Data, Eq, Ord, Show)

-- XXX TBD
newtype ModVec = ModVec Word64
  deriving (Data, Eq, Ord, Show, Num)

data ExecOp = Fork | Clone | ExecVE | Kill | SetUID
  deriving (Data, Eq, Ord, Show)

newtype UUID      = UUID Text -- XXX consider (Word64,Word64)
  deriving (Data, Eq, Ord, Show)

-- | Globally unique process ID - this is _NOT_ a commodity OS PID, but that
-- could be one component.
newtype PID       = PID Text  -- XXX consider MID|Word32
  deriving (Data, Eq, Ord, Show)

-- | Type of artifacts
type ArtifactType = Text

-- | Types of devices
data DevType      = GPS  | Camera | Keyboard | Accelerometer | OtherDev Text
  deriving (Data, Eq, Ord, Show)

-- | Methods of deriving data from a source
data DeriveOp     = Rename | Link
        deriving (Data, Eq, Ord, Show, Read)

-- | Methods of consuming (from) an artifact
data UseOp        = Read | Recv | Accept | Execute
        deriving (Data, Eq, Ord, Show, Read)

-- Methods of pushing data to an artifact
data GenOp        = Write | Send | Connect | Truncate | ChMod | Touch
        deriving (Data, Eq, Ord, Show, Read)

type EntryPoint = Text

--------------------------------------------------------------------------------
--  Types for type checking


-- class Entity
-- instance AnyNotPredicate
--
-- class Agent
-- instance Agent UnitOfExecution
--
-- class ResourceClass
-- instance Resource (should be 'device')
-- instance Artifact
--
-- class String
-- instance UUID
-- instance Time
-- instance Name

-- | Basic types in the ED graph
--
-- Notice most types are enforced by the translation to the Haskell types
-- and thus we do not bother to express them here or validate them in the
-- checker. For example, the DevType is represented by an enumeration and
-- not a typeless string.
--
-- What remains is to ensure the predicates are applied to entities of
-- the correct type.
data Type = EntityClass
          | ActorClass
          | DescribeClass
          | ResourceClass
          | TyUnitOfExecution
          | TyHost
          | TyAgent
          | TyArtifact
          | TyResource
          | TyMetadata
          | TyArrow Type Type
        deriving (Data, Eq, Ord, Show, Read)

