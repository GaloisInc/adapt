{-# LANGUAGE GeneralizedNewtypeDeriving   #-}
{-# LANGUAGE OverloadedStrings            #-}
{-# LANGUAGE MultiParamTypeClasses        #-}
{-# LANGUAGE TypeSynonymInstances         #-}
{-# LANGUAGE FlexibleInstances            #-}

module Typecheck
  ( typecheck, TypeError(..)
  ) where

import Types as T
import MonadLib
import Data.Monoid ((<>))
import qualified Data.Text.Lazy as Text
import           Data.Text.Lazy (Text)

import           Data.Map (Map)
import qualified Data.Map as Map

import Network.URI (URI(..))

--------------------------------------------------------------------------------
--  Typechecker Monad

type TyEnv = Map Text Type

newtype TC a = TC { unTC :: ExceptionT TypeError (StateT TyEnv Id) a }
  deriving(Monad, Applicative, Functor)

instance ExceptionM TC TypeError where
  raise = TC . raise

instance StateM TC TyEnv where
  get = TC get
  set = TC . set

runTC :: TC a -> Either TypeError a
runTC = fst . runId . runStateT Map.empty . runExceptionT . unTC

throwE :: TypeError -> TC a
throwE = raise

--------------------------------------------------------------------------------
--  Typechecking (mostly just inference)
--  Notice there is no need for constraint propagation, so we don't need
--  any sort of MGU or typical type checking.

-- Really bad hard-coded type schema to get us hobbling.
unify :: Type -> Type -> TC Type
-- Identical types unify
unify x y | x == y    = return x
unify x y | x > y     = unify y x
-- ^^ Most general unifier (mgu) comes first.  Actually possible in this primitive schema.
unify EntityClass x                                                 = return x
unify ActorClass  x   | x `elem` [TyAgent,TyUnitOfExecution,TyHost] = return x
unify x y = throwE (TypeError x y)

-- | Using the namespaces and verbs to infer types, type check raw triples
-- and return the annotated version of the AST.
typecheck :: [Stmt] -> Either TypeError [Stmt]
typecheck g =
    runTC $ do mapM_ tcStmt g
               return g

tcStmt :: Stmt -> TC ()
tcStmt (StmtEntity e)    = tcEntity e
tcStmt (StmtPredicate p) = tcPredicate p

tcEntity :: Entity -> TC ()
tcEntity = undefined -- XXX

tcPredicate :: Predicate -> TC ()
tcPredicate = undefined -- XXX

--------------------------------------------------------------------------------
--  Hard-coded types (could be read in from file)

(.->) :: Type -> Type -> Type
(.->) = TyArrow

infixr 4 .->

-- tcTypes :: Map ScopedName Type
-- tcTypes =
--   [ -- Entities
--     TC.uOE          .: UnitOfExecution
--     -- Predicates
--   , TC.machineID    .: Agent .-> UUID
--   , TC.artifactType .: Artifact .-> ArtifactType
--     -- Ugly 'attribute edges'
--   , TC.execOp       .: WasInformedBy .-> ExecOp
--   , TC.deriveOp     .: Artifact .-> Artifact
--   , TC.useOp        .: stringAttribute Used
--   , TC.genOp        .: stringAttribute 
--   , TC.devType      .: 
--   , TC.typedData    .: 
--   , TC.modVec       .: 
--   , TC.filePath     .: 
--   , TC.portID       .: 
--   , TC.pageID       .: 
--   , TC.packetID     .: 
--   , TC.ipAddress    .: 
--   , TC.privs        .: 
--   , TC.pid          .: 
--   , TC.pwd          .: 
--   , TC.address      .: 
--   ]

--   [ ("file",            TCArtifactType)
--   , ("packet",          TCArtifactType)
--   , ("memory",          TCArtifactType)
--   , ("fork",            TCExecOp)
--   , ("clone",           TCExecOp)
--   , ("execve",          TCExecOp)
--   , ("kill",            TCExecOp)
--   , ("setuid",          TCExecOp)
--   , ("rename",          TCDeriveOp)
--   , ("link",            TCDeriveOp)
--   , ("read",            TCUseOp)
--   , ("recv",            TCUseOp)
--   , ("accept",          TCUseOp)
--   , ("execute",         TCUseOp)
--   , ("write",           TCGenOp)
--   , ("send",            TCGenOp)
--   , ("connect",         TCGenOp)
--   , ("truncate",        TCGenOp)
--   , ("chmod",           TCGenOp)
--   , ("touch",           TCGenOp)
--   , ("gps",             TCDevType)
--   , ("camera",          TCDevType)
--   , ("keyboard",        TCDevType)
--   , ("accelerometer",   TCDevType)
--   -- XXX TMD WTF? , ("typedData",    DevType)
--   , ("modVec",          ModVec)
--   , ("filePath",        TCFilePath)
--   , ("portID",          TCPortID)
--   , ("pageID",          TCPageID)
--   , ("packetID",        TCPacketID)
--   , ("ipAddress",       TCIP)
--   , ("privs",           TCPrivs)
--   , ("pid",             TCPID)
--   , ("pwd",             TCPWD)
--   , ("Address",         TCAddress)
--   ]

-- foafTypes :: Map ScopedName Type
-- foafTypes = Map.fromList
--   [ foafname            .: Name
--   , foafaccountName     .: Name
--   , foafGroup           .: Name
--   ]
-- 
-- xsdTypes :: Map ScopedName Type
-- xsdTypes = Map.fromList
--   [ xsdString           .: TyString
--   ]
-- 
-- provTypes :: [(ScopedName,Type)]
-- provTypes =
--   [ -- Entitites
--     provStartedAt       .: Time
--   , provActivity        .: Type
--   , provAgent           .: Type
--   , provEntity          .: Type
--   , provGeneration      .: Type
--   , provLocation        .: Type
--     -- Predicates
--   , provwasAttributedTo .: Artifact .-> Agent
--   , provwasStartedBy    .: UnitOfExecution .-> UnitOfExecution
--   , provwasEndedBy      .: UnitOfExecution .-> UnitOfExecution
--   , provwasInformedBy   .: UnitOfExecution .-> UnitOfExecution
--   , provactedOnBehalfOf  .: UnitOfExecution .-> Agent
--   , provwasAssociatedWith .: UnitOfExecution .-> Agent
--   , provused             .: UnitOfExecution .-> Resource
--   , provwasGeneratedBy   .: Artifact .-> UnitOfExecution
--   , provwasDerivedFrom   .: Artifact .-> Artifact
--   , provwasInvalidatedBy .: Artifact .-> UnitOfExecution
--   -- prov predicates with bad types (edge labels) in the ED graph
--   , provstartedAtTime   .: UnitOfExecution .-> Time
--   , provendedAtTime     .: UnitOfExecution .-> Time
--   , provwasInformedBy   .: UnitOfExecution .-> UnitOfExecution
--   , provqualifiedUsage  .: Entity .-> Entity
--   ]

data Range = MaybeOne           -- 0..1
            | Any               -- 0..
            | One               -- 1..1
            deriving (Eq,Ord,Show,Enum)

-- (<->) :: ScopedName -> Range -> (ScopedName,Range)
-- (<->) a b = (a,b)

-- fanInFanOut :: Map ScopedName Range
-- fanInFanOut = Map.fromList
--   [ provwasAttributedTo         <-> (Any, Any)
--   , provactedOnBehalfOf         <-> (Any, MaybeOne)
--   , provwasAssociatedWith       <-> (Any, MaybeOne)
--   , provwasInvalidatedBy        <-> (MaybeOne, Any)
--   , provused                    <-> (Any, Any)
--   , provwasStartedBy            <-> (Any, MaybeOne)
--   , provwasEndedBy              <-> (Any, MaybeOne)
--   , provwasInformedBy           <-> (Any, MaybeOne)
--   , provwasGeneratedBy          <-> (Any, Any)
--   , provwasDerivedFrom          <-> (Any, MaybeOne)
--   , tcDevType                   <-> (Any,Any)
--   ]

-- tcEntity :: Entity () -> Except TypeError Type
-- tcEntity (Obj {..}) =
--     case namespace theObject of
--         "agent"  -> return Agent
--         "mid"    -> return Host
--         "cid"    -> return UnitOfExecution
--         "socket" -> return Resource
--         "packet" -> return Resource
--         "mem"    -> return Resource
--         "file"   -> return Resource
--         "string" -> return TyString
--         x        -> throwE $ CanNotInferType x
-- 
-- tcVerb   :: Object Verb () -> Except TypeError (Type,Type)
-- tcVerb (Obj {..}) =
--   let f a b = return (a,b)
--   in case theObject of
--         WasDerivedFrom  -> f EntityClass EntityClass
--         SpawnedBy       -> f ActorClass ActorClass
--         WasInformedBy   -> f ActorClass ActorClass
--         ActedOnBehalfOf -> f UnitOfExecution ActorClass
--         WasKilledBy     -> f UnitOfExecution UnitOfExecution
--         WasAttributedTo -> f Resource ActorClass
--         Modified        -> f UnitOfExecution Resource
--         Generated       -> f UnitOfExecution Resource
--         Destroyed       -> f UnitOfExecution Resource
--         Read            -> f UnitOfExecution Resource
--         Write           -> f UnitOfExecution Resource
--         Is              -> f Resource Resource
--         A               -> f Resource Resource
--         _               -> throwE $ CanNotInferType $ "(" <> pp theObject <> ") Unsupported object.  Is this an attribute? We don't support those yet."
-- 
