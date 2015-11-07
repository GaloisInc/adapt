{-# LANGUAGE GeneralizedNewtypeDeriving   #-}
{-# LANGUAGE OverloadedStrings            #-}
{-# LANGUAGE MultiParamTypeClasses        #-}
{-# LANGUAGE TypeSynonymInstances         #-}
{-# LANGUAGE FlexibleInstances            #-}
{-# LANGUAGE RecordWildCards              #-}
{-# LANGUAGE CPP                          #-}

module Typecheck
  ( typecheck, TypeError(..)
  ) where

import Types as T
import Namespaces (Ident)
import PP (pretty)
import Namespaces (blankNode)

#if (__GLASGOW_HASKELL__ < 710)
import Control.Applicative
#endif
import MonadLib

import           Data.Map (Map)
import qualified Data.Map as Map

--------------------------------------------------------------------------------
--  Typechecker Monad

type TyEnv = Map Ident Type

newtype TC a = TC { unTC :: ExceptionT TypeError (ReaderT Range (StateT TyEnv Id)) a }
  deriving(Monad, Applicative, Functor)

instance ExceptionM TC TypeError where
  raise = TC . raise

instance StateM TC TyEnv where
  get = TC get
  set = TC . set

instance ReaderM TC Range where
  ask = TC ask

instance RunReaderM TC Range where
  local i (TC m) = TC (local i m)

runTC :: TC a -> Either TypeError a
runTC = fst . runId . runStateT Map.empty . runReaderT NoLoc . runExceptionT . unTC

assignTy :: Ident -> Type -> TC ()
assignTy k v = sets_ (Map.insert k v)

getType :: Ident -> TC (Maybe Type)
getType k = Map.lookup k <$> get

unifyM :: Ident -> Type -> TC ()
unifyM k ty
  | k == blankNode = return ()
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
unify :: Ident -> Type -> Type -> TC Type
-- Identical types unify
unify _ x y | x == y    = return x
-- Unifier comes second. Actually possible in this primitive schema.
unify t x y | x > y     = unify t y x
-- Classes are strictly hierarchical
unify _ EntityClass x                                                            = return x
unify _ ActorClass  x   | x `elem` [ActorClass,TyAgent,TyUnitOfExecution,TyHost] = return x
unify _ ResourceClass x | x `elem` [ResourceClass, TyResource, TyArtifact]       = return x
unify _ DescribeClass x | x `elem` [DescribeClass, TyUnitOfExecution, TyArtifact] = return x
unify i x y = ask >>= \r -> raise (TypeError r (pretty i) x y)

-- | Using the namespaces and verbs to infer types, type check raw triples
-- and return the annotated version of the AST.
typecheck :: [Stmt] -> Either TypeError ()
typecheck g = runTC $ mapM_ tcStmt g

tcStmt :: Stmt -> TC ()
tcStmt (StmtEntity e)          = tcEntity e
tcStmt (StmtPredicate p)       = tcPredicate p
tcStmt (StmtLoc (Located r s)) = local r (tcStmt s)

tcEntity :: Entity -> TC ()
tcEntity (Agent i _as)           = unifyM i TyAgent
tcEntity (UnitOfExecution i _as) = unifyM i TyUnitOfExecution
tcEntity (Artifact i _as)        = unifyM i TyArtifact
tcEntity (Resource i _ty _mDevId) = unifyM i TyResource

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
