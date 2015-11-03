{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE RecordWildCards #-}
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
    , UUID(..), MID, DevID, AgentAttr(..), ArtifactAttr(..), UoeAttr(..)
    , Predicate(..) , PredicateAttr(..), PredicateType(..), DevType(..)
    , Version, CoarseLoc, FineLoc
    , UseOp, GenOp, DeriveOp, ExecOp, PID(..), ArtifactType
    , Time, EntryPoint
    , Text
    ) where

import qualified Control.Exception as X
import           Data.Monoid
import           Data.Data
import           Data.IntMap (IntMap)
import qualified Data.IntMap as IntMap
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Char (toLower)
import           Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import           Data.Time (UTCTime)
import           Data.Word (Word64)
import           ParserCore (ParseError(..))
import           Text.Show.Functions ()

import ParserCore (Time)

data Error      = PE ParseError | TCE TypeError | TRE TranslateError deriving (Show)
data Warning    = Warn Text
  deriving (Eq, Ord, Show)

ppWarning :: Warning -> Text
ppWarning (Warn w) = w

data TypeError  = TypeError Text Type Type | CanNotInferType Text
        deriving (Eq, Ord, Show, Read)
data TranslateError = MissingRequiredField (Maybe Text) Text
                    | MissingRequiredTimeField (Maybe Text) Text (UTCTime -> Stmt)
                    | TranslateError Text
        deriving (Show)

instance X.Exception TypeError
instance X.Exception ParseError
instance X.Exception Error

--------------------------------------------------------------------------------
-- The below types encode the diagram found in the "TA1 Data Conceptual Model".
-- However, the types do not enforce proper use; predicates are not
-- inherently affiliated with Entities and there is no type-level
-- enforcement of the fan-in fan-out.

data Stmt = StmtEntity Entity | StmtPredicate Predicate
  deriving (Eq, Ord, Show)

-- | Entities in our conceptual Model
-- Notice each entity has a name (Text field) except for metadata.
data Entity = Agent Text [AgentAttr]
            | UnitOfExecution Text [UoeAttr]
            | Artifact Text [ArtifactAttr]
            | Resource Text DevType (Maybe DevID)
  deriving (Eq, Ord, Show)

nameOf :: Entity -> Text
nameOf e = case e of
            Agent n _ -> n
            UnitOfExecution n _ -> n
            Artifact n _    -> n
            Resource n _ _      -> n

-- | Machine ID
type MID = UUID

-- | Attributes of Agents
data AgentAttr = AAName Text | AAUser Text | AAMachine MID
  deriving (Eq, Ord, Show)

-- | Attributes of units of execution
data UoeAttr = UAUser Text
             | UAPID PID
             | UAPPID PID
             | UAMachine MID
             | UAStarted Time
             | UAHadPrivs Privs
             | UAPWD Text
             -- XXX All the below constructors are unused, they lack any
             -- translation path from ProvN, see Translate.hs
             | UAEnded Time
             | UAGroup Text
  deriving (Eq, Ord, Show)

-- | Attributes of Artifacts
data ArtifactAttr = ArtAType ArtifactType
                  | ArtARegistryKey Text
                  | ArtACoarseLoc CoarseLoc
                  -- XXX All the below constructors are unused, they lack
                  -- any translation path from ProvN, see Translate.hs
                  | ArtAFineLoc FineLoc
                  | ArtACreated Time
                  | ArtAVersion Version
                  | ArtADeleted Time
                  | ArtAOwner Text
                  | ArtASize Integer
                  | Taint Word64
  deriving (Eq, Ord, Show)

-- | XXX TBD
type Privs = Text

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
  deriving (Eq, Ord, Show)

data PredicateAttr
        = Raw Text Text
        | AtTime Time
        | StartTime Time
        | EndTime Time
        | GenOp GenOp
        | Permissions Text
        | ReturnVal Text
        | Operation UseOp
        | Args Text
        | Cmd Text
        | DeriveOp DeriveOp
        | ExecOp ExecOp
  deriving (Eq, Ord, Show)

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
  deriving (Eq, Ord, Show)

type ExecOp = Text

newtype UUID      = UUID Text -- XXX consider (Word64,Word64)
  deriving (Eq, Ord, Show)

-- | Globally unique process ID - this is _NOT_ a commodity OS PID, but that
-- could be one component.
newtype PID       = PID Text  -- XXX consider MID|Word32
  deriving (Eq, Ord, Show)

-- | Type of artifacts
type ArtifactType = Text

-- | Types of devices
data DevType      = GPS  | Camera | Keyboard | Accelerometer | OtherDev Text
  deriving (Eq, Ord, Show)

-- | Methods of deriving data from a source
type DeriveOp     = Text

-- | Methods of consuming (from) an artifact
type UseOp        = Text

-- Methods of pushing data to an artifact
type GenOp        = Text

type EntryPoint = Text

-- | Basic types in the ED graph
data Type = EntityClass
          | ActorClass
          | DescribeClass
          | ResourceClass
          | TyUnitOfExecution
          | TyHost
          | TyAgent
          | TyArtifact
          | TyResource
          | TyArrow Type Type
          | TyVoid
        deriving (Eq, Ord, Show, Read)

