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
unify ResourceClass x | x `elem` [DataClass, Socket, File, Packet] = return x
unify DataClass x     | x `elem` [File, Packet]                    = return x
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
     ty1        <- tcObject subj
     (ty2,ty3)  <- tcVerb   verb
     ty4        <- tcObject obj
     tyLeft     <- unify ty1 ty2
     tyRight    <- unify ty4 ty3
     let tyF    = TyArrow tyLeft tyRight
     return $ TypeAnnotatedTriple $ Triple subj { objectTag = tyLeft } verb { objectTag = tyF }
                                           obj { objectTag = tyRight }

tcObject :: Object () -> Except TypeError Type
tcObject (IRI {..}) =
    case namespace of
        "agent"  -> return Agent
        "mid"    -> return Host
        "cid"    -> return UnitOfExecution
        "socket" -> return Socket
        "packet" -> return Packet
        "mem"    -> return Memory
        "file"   -> return File
        _        -> throwE $ CanNotInferType namespace
tcObject (Lit {..}) =
    case litValue of
         _ -> return TyString -- XXX need to parse dates and stuff... so we do need namespace tags a la RDF.

tcVerb   :: Object ()   -> Except TypeError (Type,Type)
tcVerb (IRI {..}) =
  let f a b = return (a,b)
  in case theObject of
        "wasDerivedFrom"  -> f EntityClass EntityClass
        "spawnedBy"       -> f ActorClass ActorClass
        "wasInformedBy"   -> f ActorClass ActorClass
        "actedOnBehalfOf" -> f UnitOfExecution ActorClass
        "wasKilledBy"     -> f UnitOfExecution UnitOfExecution
        "wasAttributedTo" -> f ResourceClass ActorClass
        "modified"        -> f UnitOfExecution DataClass
        "generated"       -> f UnitOfExecution ResourceClass
        "destroyed"       -> f UnitOfExecution ResourceClass
        "read"            -> f UnitOfExecution ResourceClass
        "write"           -> f UnitOfExecution ResourceClass
        "is"              -> f ResourceClass ResourceClass
        _                 -> throwE $ CanNotInferType $ "(" <> theObject <> ") Unsupported object.  Is this an attribute? We don't support those yet."

