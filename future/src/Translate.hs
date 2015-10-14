-- | Translate the parsed AST ('ParseCore's 'Prov' type) into our domain
-- representation (list of 'Entity').
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ViewPatterns               #-}

module Translate
  ( translate
  ) where

import Namespaces
import ParserCore
import qualified Types as T

import           Data.Maybe (catMaybes)
import qualified Data.Map.Strict as Map
import           Data.Map.Strict (Map)
import qualified Data.Generics.Uniplate.Operations as Uniplate
import           Data.Generics.Uniplate.Data ()
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)

translate :: (Monad m) => Prov -> m [T.Stmt]
translate p = tExprs (expandDomains p)

-- Everywhere the 'domain' of an 'Ident' is a member prefixMap, replace
-- it with the key value.
expandDomains :: Prov -> Prov
expandDomains (Prov ps es) = Prov ps (Uniplate.transformBi f es)
  where
  f :: Ident -> Ident
  f i@(Qualified d l) = maybe i (\d' -> mkIdent d' l) $ Map.lookup d m
  f i                 = i

  m = Map.fromList [(l,v) | Prefix (Unqualified l) v <- ps]

tExprs :: (Monad m) => Prov -> m [T.Stmt]
tExprs (Prov _prefix es) = mapM tExpr es

tExpr :: (Monad m) => Expr -> m T.Stmt
tExpr (Entity i kvs                                                        ) = T.StmtEntity <$> entity i kvs
tExpr (Agent i kvs                                                         ) = T.StmtEntity <$> agent i kvs
tExpr (Activity i mStart mEnd kvs                                          ) = T.StmtEntity <$> activity i mStart mEnd kvs
tExpr (WasGeneratedBy mI subj mObj mTime kvs                               ) = T.StmtPredicate <$> wasGeneratedBy mI subj mObj mTime kvs
tExpr (Used mI subj mObj mTime kvs                                         ) = T.StmtPredicate <$> used mI subj mObj mTime kvs
tExpr (WasStartedBy mI subj mObj mParentTrigger mTime kvs                  ) = T.StmtPredicate <$> wasStartedBy mI subj mObj mParentTrigger mTime kvs
tExpr (WasEndedBy mI subj  mObj mParentTrigger mTime kvs                   ) = T.StmtPredicate <$> wasEndedBy mI subj  mObj mParentTrigger mTime kvs
tExpr (WasInformedBy mI subj mObj kvs                                      ) = T.StmtPredicate <$> wasInformedBy mI subj mObj kvs
tExpr (WasAssociatedWith mI subj mObj mPlan kvs                            ) = T.StmtPredicate <$> wasAssociatedWith mI subj mObj mPlan kvs
tExpr (WasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs  ) = T.StmtPredicate <$> wasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs
tExpr (WasAttributedTo mI subj obj kvs                                     ) = T.StmtPredicate <$> wasAttributedTo mI subj obj kvs
tExpr (IsPartOf subj obj                                                   ) = T.StmtPredicate <$> isPartOf subj obj
tExpr (Description subj kvs                                                ) = T.StmtPredicate <$> description subj kvs
tExpr (RawEntity i args kvs                                                )
    | i == mkIdent dc "description" =
        case args of
          [s]   -> tExpr (Description s kvs)
          _     -> fail $ "Dublic core 'description' requires a single argument, has " ++ show (length args)
    | i == mkIdent dc "isPartOf" =
        case (args,kvs) of
          ([s,o],[]) -> tExpr (IsPartOf s o)
          _     -> fail $ "Dublic core 'isPartOf' requires two argument and no attributes, but has " ++ show (length args, length kvs)
tExpr (RawEntity i _args _kvs) = fail $ "Unknown prov element: " ++ show i


--------------------------------------------------------------------------------
--  Entity Translation

-- If
--   * it has 'devType' is Resource.
--   * it has prov:type='adapt:artifact' is Artifactj
entity i kvs = undefined

-- Agents are either units of execution or exist largely for the metadata
-- of machine ID, foaf:name, and foaf:accountName.
agent i kvs
  | provType kvs == Just T.TyUnitOfExecution = return $ T.UnitOfExecution (textOfIdent i) (uoeAttr kvs)
  | otherwise                              = return (T.Agent (textOfIdent i) $ agentAttr kvs)

-- All Activities are units of execution, no exceptions.
activity i mStart mEnd kvs
  | provType kvs == Just T.TyUnitOfExecution = return $ T.UnitOfExecution (textOfIdent i) (uoeTimes mStart mEnd ++ uoeAttr kvs)
  | otherwise = undefined -- XXX error

uoeTimes :: Maybe Time -> Maybe Time -> [T.UoeAttr]
uoeTimes a b = catMaybes $ zipWith fmap [T.UAStarted, T.UAEnded] [a,b]

provType :: KVs -> Maybe T.Type
provType = undefined -- XXX

--------------------------------------------------------------------------------
--  Entity Attributes

uoeAttr :: KVs -> [T.UoeAttr]
uoeAttr = undefined -- XXX

agentAttr :: KVs -> [T.AgentAttr]
agentAttr = undefined -- XXX

--------------------------------------------------------------------------------
--  Predicate Translation
-- XXX we drop all values that start with an underscore, such as the entity
-- and activity that cause a data derivation, or the "plan" for an
-- association.

wasGeneratedBy mI subj (orBlank -> obj) (Just at) kvs = return $ predicate subj obj T.WasGeneratedBy (T.AtTime at : predAttr kvs)
wasGeneratedBy _  _    _          Nothing   _   = undefined -- XXX error, we do not tolerate unknown times...

used _i subj (orBlank -> obj) (Just at) kvs = return $ predicate subj obj T.Used (T.AtTime at : predAttr kvs)
used _  _    _                 Nothing    _ = undefined -- XXX error, we do not tolerate unknown times...

wasStartedBy _i subj (orBlank -> obj) _mParentTrigger (Just at) kvs = return $ predicate subj obj T.WasStartedBy (T.AtTime at : predAttr kvs)
wasEndedBy _i subj  (orBlank -> obj) _mParentTrigger (Just at) kvs = return  $ predicate subj obj T.WasEndedBy (T.AtTime at : predAttr kvs)
wasInformedBy _i subj (orBlank -> obj) kvs = return $ predicate subj obj T.WasInformedBy (predAttr kvs)
wasAssociatedWith _i subj (orBlank -> obj) _mPlan kvs = return $ predicate subj obj T.WasAssociatedWith (predAttr kvs)

wasDerivedFrom _i subj obj _mGeneratingEnt _mGeneratingAct _useId kvs = return $ predicate subj obj T.WasDerivedFrom (predAttr kvs)
wasAttributedTo _i subj obj kvs = return $ predicate subj obj T.WasAttributedTo (predAttr kvs)
isPartOf subj obj    = return $ predicate subj obj T.IsPartOf []
description subj kvs = return $ predicate subj (orBlank Nothing) T.Description (predAttr kvs)

predicate :: Ident -> Ident -> T.PredicateType -> [T.PredicateAttr] -> T.Predicate
predicate s o ty attr = T.Predicate (textOfIdent s) (textOfIdent o) ty attr


predAttr :: KVs -> [T.PredicateAttr]
predAttr _ = [] -- XXX

orBlank :: Maybe Ident -> Ident
orBlank = maybe (Unqualified "_") id
