{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}

module Typecheck
  ( typecheck, TypeError(..), RawTA1(..), TypeAnnotatedTriple(..)
  -- * Low level interface
  , tcVerb, tcTriple
  ) where

import Types
import MonadLib
import Data.Monoid ((<>))
import Data.Text ()

--------------------------------------------------------------------------------
--  Typechecking (mostly just inference)
--  Notice there is no need for constraint propagation, so we don't need
--  any sort of MGU or typical type checking.

-- Really bad hard-coded type schema to get us hobbling.
unify :: Type -> Type -> Except TypeError Type
-- Identical types unify
unify x y | x == y    = return x
unify x y | x > y     = unify y x
-- ^^ Most general unifier (mgu) comes first.  Actually possible in this primitive schema.
unify EntityClass x                                                = return x
unify ActorClass  x   | x `elem` [Agent,UnitOfExecution,Host]      = return x
unify x y = throwE (TypeError x y)

-- | Using the namespaces and verbs to infer types, type check raw triples
-- and return the annotated version of the AST.
typecheck :: [RawTA1] -> Either TypeError [TypeAnnotatedTriple]
typecheck = runExcept . mapM tcTriple

runExcept :: Except i b -> Either i b
runExcept = runId . runExceptionT
throwE = raise

type Except i a = ExceptionT i Id a

tcTriple :: RawTA1 -> Except TypeError TypeAnnotatedTriple
tcTriple (RawTA1 (Triple subj verb obj)) = do
     ty1        <- tcEntity subj
     (ty2,ty3)  <- tcVerb   verb
     ty4        <- tcEntity obj
     tyLeft     <- unify ty1 ty2
     tyRight    <- unify ty4 ty3
     let tyF    = TyArrow tyLeft tyRight
     return $ TypeAnnotatedTriple $ Triple subj { objectTag = tyLeft } verb { objectTag = tyF }
                                           obj { objectTag = tyRight }

tcEntity :: Entity () -> Except TypeError Type
tcEntity (Obj {..}) =
    case namespace theObject of
        "agent"  -> return Agent
        "mid"    -> return Host
        "cid"    -> return UnitOfExecution
        "socket" -> return Resource
        "packet" -> return Resource
        "mem"    -> return Resource
        "file"   -> return Resource
        "string" -> return TyString
        x        -> throwE $ CanNotInferType x

tcVerb   :: Object Verb () -> Except TypeError (Type,Type)
tcVerb (Obj {..}) =
  let f a b = return (a,b)
  in case theObject of
        WasDerivedFrom  -> f EntityClass EntityClass
        SpawnedBy       -> f ActorClass ActorClass
        WasInformedBy   -> f ActorClass ActorClass
        ActedOnBehalfOf -> f UnitOfExecution ActorClass
        WasKilledBy     -> f UnitOfExecution UnitOfExecution
        WasAttributedTo -> f Resource ActorClass
        Modified        -> f UnitOfExecution Resource
        Generated       -> f UnitOfExecution Resource
        Destroyed       -> f UnitOfExecution Resource
        Read            -> f UnitOfExecution Resource
        Write           -> f UnitOfExecution Resource
        Is              -> f Resource Resource
        A               -> f Resource Resource
        _               -> throwE $ CanNotInferType $ "(" <> pp theObject <> ") Unsupported object.  Is this an attribute? We don't support those yet."

