{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE DeriveDataTypeable #-}

module Types
    ( -- * Types
      Error(..)
    , ParseError(..)
    , TypeError(..)
    , TranslateError(..)
    , Warning(..)
      -- * Key types
    , Type(..)
    , Stmt(..)
    , Entity(..)
    , UUID, MID, DevID, AgentAttr(..), ArtifactAttr(..), UoeAttr(..)
    , Predicate(..) , PredicateAttr(..), PredicateType(..), DevType(..)
    , Version, CoarseLoc, FineLoc
    , UseOp, GenOp, DeriveOp, ExecOp, PID, ArtifactType
    , Time, EntryPoint
    , Text
    , Located(..), Range(..)
    -- * Helpers
    , nameOf
    ) where

import qualified Control.Exception as X
import           Data.Data
import           Data.Text.Lazy (Text)
import           Data.Time (UTCTime)
import           Data.Word (Word64)
import           Text.Show.Functions ()


import           Namespaces (Ident(..), textOfIdent)
import           PP as PP
import           Position
import           LexerCore (Token)

type Time = UTCTime

data Error      = PE ParseError | TCE TypeError | TRE TranslateError
  deriving (Show, Data, Typeable)
data Warning    = Warn Text
  deriving (Eq, Ord, Show, Data, Typeable)

data TypeError  = TypeError Range Text Type Type | CanNotInferType Text
        deriving (Eq, Ord, Show, Data, Typeable)
data TranslateError = TranslateError Text
        deriving (Show, Data, Typeable)

data ParseError = HappyError (Maybe (Located Token))
                | HappyErrorMsg String
                  deriving (Data,Typeable, Eq, Ord, Show)

instance X.Exception TypeError
instance X.Exception ParseError
instance X.Exception Error

--------------------------------------------------------------------------------
-- The below types encode the diagram found in the "TA1 Data Conceptual Model".
-- However, the types do not enforce proper use; predicates are not
-- inherently affiliated with Entities and there is no type-level
-- enforcement of the fan-in fan-out.

data Stmt = StmtEntity Entity
          | StmtPredicate Predicate
          | StmtLoc (Located Stmt)
  deriving (Eq, Ord, Show, Data, Typeable)

-- | Entities in our conceptual Model
-- Notice each entity has a name (Text field) except for metadata.
data Entity = Agent Ident [AgentAttr]
            | UnitOfExecution Ident [UoeAttr]
            | Artifact Ident [ArtifactAttr]
            | Resource Ident DevType (Maybe DevID)
  deriving (Eq, Ord, Show, Data, Typeable)

nameOf :: Entity -> Text
nameOf e = 
 textOfIdent $ case e of
            Agent n _ -> n
            UnitOfExecution n _ -> n
            Artifact n _    -> n
            Resource n _ _      -> n

-- | Machine ID
type MID = UUID

-- | Attributes of Agents
data AgentAttr = AAName Text | AAUser Text | AAMachine MID
  deriving (Eq, Ord, Show, Data, Typeable)

-- | Attributes of units of execution
data UoeAttr = UAUser Text
             | UAPID PID
             | UAPPID PID
             | UAMachine MID
             | UAStarted Time
             | UAHadPrivs Privs
             | UAPWD Text
             | UAEnded Time
             | UAGroup Text
             | UACommandLine Text
             | UASource Text
             | UAProgramName Text
             | UACWD Text
             | UAUID Text
  deriving (Eq, Ord, Show, Data, Typeable)

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
                  | ArtADestinationAddress Text
                  | ArtADestinationPort Text
                  | ArtASourceAddress Text
                  | ArtASourcePort Text
                  | Taint Word64
  deriving (Eq, Ord, Show, Data, Typeable)

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


data Predicate = Predicate { predSubject    :: Ident
                           , predObject     :: Ident
                           , predType       :: PredicateType
                           , predAttrs      :: [PredicateAttr]
                           }
  deriving (Eq, Ord, Show, Data, Typeable)

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
        | MachineID MID
        | SourceAddress Text
        | DestinationAddress Text
        | SourcePort Text
        | DestinationPort Text
        | Protocol Text
  deriving (Eq, Ord, Show, Data, Typeable)

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
  deriving (Eq, Ord, Show, Data, Typeable)

type ExecOp = Text

type UUID      = Text

-- | Globally unique process ID - this is _NOT_ a commodity OS PID, but that
-- could be one component.
type PID       = Text

-- | Type of artifacts
type ArtifactType = Text

-- | Types of devices
data DevType      = GPS  | Camera | Keyboard | Accelerometer | OtherDev Text
  deriving (Eq, Ord, Show, Data, Typeable)

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
        deriving (Eq, Ord, Show, Read, Data, Typeable)

--------------------------------------------------------------------------------
--  Pretty Printing

instance PP Error where
  ppPrec _ (PE e)  = text "Parse error: " PP.<> pp e
  ppPrec _ (TCE e) = text "Typecheck error: " PP.<> pp e
  ppPrec _ (TRE e) = text "Translation error: " PP.<> pp e

instance PP ParseError where
  ppPrec _ (HappyError Nothing)    = text "Unknown parser error."
  ppPrec _ (HappyError (Just loc)) = text "Could not parse token " PP.<> pp loc
  ppPrec _ (HappyErrorMsg s)       = text s

instance PP TranslateError where
  ppPrec _ (TranslateError t) = pp t

instance PP TypeError where
  ppPrec _ (CanNotInferType t) = text "Can not infer type for " PP.<> pp t
  ppPrec _ (TypeError r a b c)   =
      pp r <> text ": Can not unify inferred types of " PP.<>
               pp b PP.<> " and " PP.<> pp c PP.<> " for " PP.<> pp a

instance PP Warning where
  ppPrec _ (Warn w) = pp w

instance PP Type where
  ppPrec _ t =
    case t of
        TyArrow a b       -> pp a PP.<> text " -> " PP.<> pp b
        EntityClass       -> text "Entity class"
        ActorClass        -> text "Actor class"
        DescribeClass     -> text "Describable class"
        ResourceClass     -> text "Resource class"
        TyUnitOfExecution -> text "UOE"
        TyHost            -> text "Host"
        TyAgent           -> text "Agent"
        TyArtifact        -> text "Artifact"
        TyResource        -> text "Resource"
        TyVoid            -> text "Void"

instance PP PredicateType where
   ppPrec _ t =
    case t of
       ActedOnBehalfOf   -> text "actedOnBehalfOf"
       WasAssociatedWith -> text "wasAssociatedWith"
       WasStartedBy      -> text "wasStartedBy"
       WasEndedBy        -> text "wasEndedBy"
       WasInformedBy     -> text "wasInformedBy"
       Used              -> text "used"
       WasGeneratedBy    -> text "wasGeneratedBy"
       WasAttributedTo   -> text "wasAttributedTo"
       WasInvalidatedBy  -> text "wasInvalidatedBy"
       WasDerivedFrom    -> text "wasDerivedFrom"
       Description       -> text "description"
       IsPartOf          -> text "isPartOf"
