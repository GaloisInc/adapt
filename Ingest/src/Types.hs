{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE FlexibleInstances          #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE RecordWildCards            #-}
{-# LANGUAGE DeriveDataTypeable         #-}
{-# LANGUAGE TypeOperators              #-}
{-# LANGUAGE DeriveGeneric              #-}
{-# LANGUAGE FlexibleContexts           #-}
{-# OPTIONS_GHC -fno-warn-orphans       #-}

module Types
    ( -- * Types
      Error(..)
    , ParseError(..)
    , TypeError(..)
    , TranslateError(..)
    , Warning(..)
      -- * Key types
    , Ident(..)
    , Prov(..)
    , Prefix(..)
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
    , OnIdent(..)
    ) where

import           GHC.Generics -- For generic 'onIdent' traversal
import qualified Control.Exception as X
import           Data.Char (toLower)
import           Data.Data
import           Data.Text.Lazy (Text)
import           Data.Time
import           Numeric (showFFloat)
import           Data.Word (Word64)
import           Text.Show.Functions ()

import           ParserCore (ParseError(..), Time, Prov(..), Prefix(..))
import           Namespaces (Ident(..), textOfIdent)
import           PP as PP
import           Position
import           LexerCore (Type(..))

data Error      = PE ParseError | TCE TypeError | TRE TranslateError
  deriving (Show, Data, Generic, Typeable)
data Warning    = Warn Text
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

data TypeError  = TypeError Range Text Type Type | CanNotInferType Text
        deriving (Eq, Ord, Show, Data, Generic, Typeable)
data TranslateError = TranslateError Text
        deriving (Show, Data, Generic, Typeable)

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
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

-- | Entities in our conceptual Model
data Entity = Agent Ident [AgentAttr]
            | UnitOfExecution Ident [UoeAttr]
            | Artifact Ident [ArtifactAttr]
            | Resource Ident DevType (Maybe DevID)
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

nameOf :: Entity -> Text
nameOf e =
 textOfIdent $ case e of
            Agent n _           -> n
            UnitOfExecution n _ -> n
            Artifact n _        -> n
            Resource n _ _      -> n

-- | Machine ID
type MID = UUID

-- | Attributes of Agents
data AgentAttr = AAName Text | AAUser Text | AAMachine MID
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

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
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

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
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

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
                           , predIdent      :: Maybe Ident
                           , predType       :: PredicateType
                           , predAttrs      :: [PredicateAttr]
                           }
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

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
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

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
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

type ExecOp = Text

type UUID      = Text

-- | Globally unique process ID - this is _NOT_ a commodity OS PID, but that
-- could be one component.
type PID       = Text

-- | Type of artifacts
type ArtifactType = Text

-- | Types of devices
data DevType      = GPS  | Camera | Keyboard | Accelerometer | OtherDev Text
  deriving (Eq, Ord, Show, Data, Generic, Typeable)

-- | Methods of deriving data from a source
type DeriveOp     = Text

-- | Methods of consuming (from) an artifact
type UseOp        = Text

-- Methods of pushing data to an artifact
type GenOp        = Text

type EntryPoint = Text

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

instance PP Stmt where
  ppPrec _ (StmtEntity e)        = pp e
  ppPrec _ (StmtPredicate p)     = pp p
  ppPrec _ (StmtLoc (Located _ s)) = pp s

instance PP Predicate where
  ppPrec _ (Predicate { .. }) =
      case predType of
        Used -> text "used(" <> identDoc <> pp predSubject <> text ", " <> pp predObject <> text ", " <> start <> text ", [" <> ppList predAttrs' <> text "])"
        _    -> pp predType  <> text "(" <> pp predSubject <> text ", " <> pp predObject <> text ", [" <> ppList predAttrs <> text "])"
    where
    start = maybe (text "-") pp (getAtTime predAttrs)
    getAtTime  = foldr (\x a -> case x of { AtTime t -> Just t ; _ -> a }) Nothing
    predAttrs' = filter (\x -> case x of { AtTime _ -> False ; _ -> True }) predAttrs
    identDoc   = case predIdent of
                   Nothing  -> mempty
                   Just i   -> pp i <> text "; "

instance PP PredicateAttr where
  ppPrec _ p0 =
    case p0 of
        Raw k v              -> pp k                              <=> ppq v
        StartTime t          -> text "prov:startTime"             <=> ppq t
        EndTime t            -> text "prov:endTime"               <=> ppq t
        AtTime t             -> text "prov-tc:time"               <=> ppq t
        GenOp genOp          -> text "prov-tc:genOp"              <=> ppq genOp
        Permissions t        -> text "prov-tc:permissions"        <=> ppq t
        ReturnVal t          -> text "prov-tc:returnVal"          <=> ppq t
        Operation useOp      -> text "prov-tc:operation"          <=> ppq useOp
        Args t               -> text "prov-tc:args"               <=> ppq t
        Cmd t                -> text "prov-tc:cmd"                <=> ppq t
        DeriveOp deriveOp    -> text "prov-tc:deriveOp"           <=> ppq deriveOp
        ExecOp execOp        -> text "prov-tc:execOp"             <=> ppq execOp
        MachineID mid        -> text "prov-tc:machineID"          <=> ppq mid
        SourceAddress t      -> text "prov-tc:sourceAddress"      <=> ppq t
        DestinationAddress t -> text "prov-tc:destinationAddress" <=> ppq t
        SourcePort t         -> text "prov-tc:sourcePort"         <=> ppq t
        DestinationPort t    -> text "prov-tc:destinationPort"    <=> ppq t
        Protocol t           -> text "prov-tc:protocol"           <=> ppq t

ppq :: PP a => a -> Doc
ppq x = text "\"" <> pp x <> text "\""

instance PP Time where
  ppPrec _ t = pp year <-> pp month <-> pp day <> pp 'T' <> pp hour <:> pp minute <:> secDoc <> pp 'Z'
     where
        (year,month,day)               = toGregorian (utctDay t)
        TimeOfDay hour minute picosec  = timeToTimeOfDay (utctDayTime t)
        sec                            = realToFrac picosec / (10^(12::Int) :: Double)
        secDoc                         = text $ showFFloat (Just 6) sec ""

instance PP Entity where
  ppPrec _ e0 =
    case e0 of
      Agent i as            -> text "agent(" <> pp i <> text ", [" <> ppList as <> text "])"
      UnitOfExecution i as  -> ppActivity i as
      Artifact i as         -> text "entity(" <> pp i <> text ", [" <> ppList as <> text "])"
      Resource i dt did     -> text "entity(" <> pp i <> text ", [" <> text "prov-tc:devType" <=> pp dt <> didDoc <> text "])"
        where didDoc = case did of
                        Just d -> text ", prov-tc:devID" <=> pp d
                        Nothing -> mempty

ppActivity :: Ident -> [UoeAttr] -> Doc
ppActivity i as = text "activity(" <> pp i <> comma <> start <> comma <> end <> text ", [" <> ppList as' <> text "])"
 where
 start = maybe (text "-") pp (getUAStarted as)
 end   = maybe (text "-") pp (getUAEnded as)
 getUAStarted = foldr (\x a -> case x of { UAStarted t -> Just t ; _ -> a }) Nothing
 getUAEnded   = foldr (\x a -> case x of { UAEnded t   -> Just t ; _ -> a }) Nothing
 as'       = filter notTime as
 notTime x = case x of
              UAStarted _ -> False
              UAEnded   _ -> False
              _           -> True

instance PP DevType where
  ppPrec _ dt = pp (map toLower $ show dt)

instance PP AgentAttr where
  ppPrec _ a0 =
    case a0 of
      AAName t      -> text "prov-tc:name"      <=> ppq  t
      AAUser t      -> text "prov-tc:user"      <=> ppq t
      AAMachine mid -> text "prov-tc:machineID" <=> ppq mid

instance PP UoeAttr where
  ppPrec _ a0 =
    case a0 of
      UAUser t        -> text "foaf:accountName"    <=> ppq t
      UAPID pid       -> text "prov-tc:pid"         <=> ppq pid
      UAPPID pid      -> text "prov-tc:ppid"        <=> ppq pid
      UAMachine mid   -> text "prov-tc:machineID"   <=> ppq mid
      UAStarted t     -> text "prov:startedAtTime"  <=> ppq t
      UAHadPrivs p    -> text "prov-tc:privs"       <=> ppq p
      UAPWD t         -> text "prov-tc:pwd"         <=> ppq t
      UAEnded t       -> text "prov-tc:endedAtTime" <=> ppq t
      UAGroup t       -> text "prov-tc:group"       <=> ppq t
      UACommandLine t -> text "prov-tc:commandLine" <=> ppq t
      UASource t      -> text "prov-tc:source"      <=> ppq t
      UAProgramName t -> text "prov-tc:programName" <=> ppq t
      UACWD t         -> text "prov-tc:cwd"         <=> ppq t
      UAUID t         -> text "prov-tc:uid"         <=> ppq t

instance PP ArtifactAttr where
  ppPrec _ a0 =
    case a0 of
      ArtAType ty              -> text "prov-tc:artifactType"       <=> ppq ty
      ArtARegistryKey t        -> text "prov-tc:registryKey"        <=> ppq t
      ArtACoarseLoc cloc       -> text "prov-tc:coarseLoc"          <=> ppq cloc
      ArtADestinationAddress t -> text "prov-tc:destinationAddress" <=> ppq t
      ArtADestinationPort t    -> text "prov-tc:destinationPort"    <=> ppq t
      ArtASourceAddress t      -> text "prov-tc:sourceAddress"      <=> ppq t
      ArtASourcePort t         -> text "prov-tc:sourcePort"         <=> ppq t
      --  XXX All the below constructors are unused, they lack
      --  any translation path from ProvN, see Translate.hs
      ArtAFineLoc floc         -> text "prov-tc:fineLoc"            <=> ppq floc
      ArtACreated t            -> text "prov-tc:created"            <=> ppq t
      ArtAVersion ver          -> text "prov-tc:version"            <=> ppq ver
      ArtADeleted t            -> text "prov-tc:deleted"            <=> ppq t
      ArtAOwner t              -> text "prov-tc:owner"              <=> ppq t
      ArtASize i               -> text "prov-tc:size"               <=> ppq i
      Taint w64                -> text "prov-tc:taint"              <=> pp (fromIntegral w64 :: Integer)

(<=>) :: Doc -> Doc -> Doc
(<=>) a b = a <> text " = " <> b

(<:>) :: Doc -> Doc -> Doc
(<:>) a b = a <> pp ':' <> b

(<->) :: Doc -> Doc -> Doc
(<->) a b = a <> pp '-' <> b

-- XXX
-- The below is a demonstration of why 'Instant Generics' aka GHC Generics
-- are a bad thing in their current instantiation.
class GOnIdent f where
  gonIdent :: (Ident -> Ident) -> f a -> f a

class OnIdent a where
  onIdent :: (Ident -> Ident) -> a -> a

instance GOnIdent U1 where
  gonIdent _ U1 = U1

instance (GOnIdent a, GOnIdent b) => GOnIdent (a :*: b) where
  gonIdent f (a :*: b) = gonIdent f a :*: gonIdent f b

instance (GOnIdent a, GOnIdent b) => GOnIdent (a :+: b) where
  gonIdent f (L1 x) = L1 $ gonIdent f x
  gonIdent f (R1 x) = R1 $ gonIdent f x

instance GOnIdent a => GOnIdent (M1 i c a) where
  gonIdent f (M1 x) = M1 (gonIdent f x)

instance OnIdent c => GOnIdent (K1 i c) where
  gonIdent f (K1 x) = K1 (onIdent f x)

instance OnIdent Stmt where
  onIdent f = to . gonIdent f . from
instance OnIdent Entity where
  onIdent f = to . gonIdent f . from
instance OnIdent Predicate where
  onIdent f = to . gonIdent f . from
instance OnIdent Ident where
  onIdent f = f
instance (OnIdent s) => OnIdent (Located s) where
  onIdent f (Located x a) = Located x (onIdent f a)
instance OnIdent PredicateType where
  onIdent _ = id
instance OnIdent UTCTime where
  onIdent _ = id
instance OnIdent Text where
  onIdent _ = id
instance OnIdent a => OnIdent [a] where
  onIdent f = map (onIdent f)
instance OnIdent PredicateAttr where
  onIdent f = to . gonIdent f . from
instance OnIdent a => OnIdent (Maybe a) where
  onIdent f = fmap (onIdent f)
instance OnIdent UoeAttr where
  onIdent f = to . gonIdent f . from
instance OnIdent ArtifactAttr where
  onIdent f = to . gonIdent f . from
instance OnIdent Word64 where
  onIdent _ = id
instance OnIdent AgentAttr where
  onIdent f = to . gonIdent f . from
instance OnIdent DevType where
  onIdent _ = id
instance OnIdent Integer where
  onIdent _ = id
