{-# LANGUAGE GeneralizedNewtypeDeriving   #-}
{-# LANGUAGE OverloadedStrings            #-}
{-# LANGUAGE MultiParamTypeClasses        #-}
{-# LANGUAGE TypeSynonymInstances         #-}
{-# LANGUAGE FlexibleInstances            #-}
{-# LANGUAGE RecordWildCards              #-}

module Typecheck
  ( typecheck, TypeError(..)
  ) where

import Types as T
import Namespaces (blankNode, textOfIdent)

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

assignTy :: Text -> Type -> TC ()
assignTy k v = sets_ (Map.insert k v)

getType :: Text -> TC (Maybe Type)
getType k = Map.lookup k <$> get

unifyM :: Text -> Type -> TC ()
unifyM k ty
  | k == textOfIdent blankNode = return ()
  | otherwise      =
  do tyOld <- getType k
     case tyOld of
      Nothing -> assignTy k ty
      Just t  -> do newTy <- unify k ty t
                    when (t /= newTy) (assignTy k newTy)

--------------------------------------------------------------------------------
--  Type-checking (mostly just inference)
--  Notice there is no need for constraint propagation, so we don't need
--  any sort of MGU or typical type checking.  In fact, all types are
--  existential - there is no proper polymorphism.

-- hard-coded type schema, since it should never change.
unify :: Text -> Type -> Type -> TC Type
-- Identical types unify
unify _ x y | x == y    = return x
-- Unifier comes second. Actually possible in this primitive schema.
unify t x y | x > y     = unify t y x
-- Classes are strictly hierarchical
unify _ EntityClass x                                                            = return x
unify _ ActorClass  x   | x `elem` [ActorClass,TyAgent,TyUnitOfExecution,TyHost] = return x
unify _ ResourceClass x | x `elem` [ResourceClass, TyResource, TyArtifact]       = return x
unify _ DescribeClass x | x `elem` [DescribeClass, TyUnitOfExecution, TyArtifact] = return x
unify i x y = raise (TypeError i x y)

-- | Using the namespaces and verbs to infer types, type check raw triples
-- and return the annotated version of the AST.
typecheck :: [Stmt] -> Either TypeError ()
typecheck g = runTC $ mapM_ tcStmt g

tcStmt :: Stmt -> TC ()
tcStmt (StmtEntity e)    = tcEntity e
tcStmt (StmtPredicate p) = tcPredicate p

tcEntity :: Entity -> TC ()
tcEntity (Agent i as)           = unifyM i TyAgent
tcEntity (UnitOfExecution i as) = unifyM i TyUnitOfExecution
tcEntity (Artifact i as)        = unifyM i TyArtifact
tcEntity (Resource i ty mDevId) = unifyM i TyResource

tcPredicate :: Predicate -> TC ()
tcPredicate (Predicate { .. }) =
  do unifyM predSubject EntityClass
     unifyM predObject EntityClass
     let TyArrow tyA tyB = predicateTypeToType predType
     unifyM predSubject tyA
     unifyM predObject tyB

--------------------------------------------------------------------------------
--  Hard-coded types (could be read in from file)

predicateTypeToType :: PredicateType -> Type
predicateTypeToType p =
  case p of
     ActedOnBehalfOf    -> TyUnitOfExecution .-> TyAgent
     WasAssociatedWith  -> TyUnitOfExecution .-> TyAgent
     WasStartedBy       -> TyUnitOfExecution .-> TyUnitOfExecution
     WasEndedBy         -> TyUnitOfExecution .-> TyUnitOfExecution
     WasInformedBy      -> TyUnitOfExecution .-> TyUnitOfExecution
     Used               -> TyUnitOfExecution .-> ResourceClass
     WasGeneratedBy     -> TyArtifact        .-> TyUnitOfExecution
     WasAttributedTo    -> TyArtifact        .-> ActorClass
     WasInvalidatedBy   -> TyArtifact        .-> TyUnitOfExecution
     WasDerivedFrom     -> TyArtifact        .-> TyArtifact
     Description        -> TyVoid            .-> DescribeClass
     IsPartOf           -> TyArtifact        .-> TyArtifact


(.->) :: Type -> Type -> Type
(.->) = TyArrow

infixr 4 .->


-- data Range = MaybeOne           -- 0..1
--             | Any               -- 0..
--             | One               -- 1..1
--             deriving (Eq,Ord,Show,Enum)
--
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
