-- | Translate the parsed AST ('ParseCore's 'Prov' type) into our domain
-- representation (list of 'Entity').
{-# LANGUAGE OverloadedStrings          #-}

module Translate
  ( translate
  ) where

import Namespaces
import ParserCore
import qualified Data.Map.Strict as Map
import           Data.Map.Strict (Map)
import qualified Data.Generics.Uniplate.Operations as Uniplate
import           Data.Generics.Uniplate.Data ()
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)

type Entity = () -- XXX

translate :: (Monad m) => Prov -> m [Entity]
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

tExprs :: (Monad m) => Prov -> m [Entity]
tExprs (Prov _prefix es) = mapM tExpr es

tExpr :: (Monad m) => Expr -> m Entity
tExpr (Entity i kvs                                                        ) = entity i kvs
tExpr (Agent i kvs                                                         ) = agent i kvs
tExpr (Activity i mStart mEnd kvs                                          ) = activity i mStart mEnd kvs
tExpr (WasGeneratedBy mI subj mObj mTime kvs                               ) = wasGeneratedBy mI subj mObj mTime kvs
tExpr (Used mI subj mObj mTime kvs                                         ) = used mI subj mObj mTime kvs
tExpr (WasStartedBy mI subj mObj mParentTrigger mTime kvs                  ) = wasStartedBy mI subj mObj mParentTrigger mTime kvs
tExpr (WasEndedBy mI subj  mObj mParentTrigger mTime kvs                   ) = wasEndedBy mI subj  mObj mParentTrigger mTime kvs
tExpr (WasInformedBy mI subj mObj kvs                                      ) = wasInformedBy mI subj mObj kvs
tExpr (WasAssociatedWith mI subj mObj mPlan kvs                            ) = wasAssociatedWith mI subj mObj mPlan kvs
tExpr (WasDerivedFrom mI subj mObj mGeneratingEnt mGeneratingAct useId kvs ) = wasDerivedFrom mI subj mObj mGeneratingEnt mGeneratingAct useId kvs
tExpr (WasAttributedTo mI subj obj kvs                                     ) = wasAttributedTo mI subj obj kvs
tExpr (IsPartOf subj obj                                                   ) = isPartOf subj obj
tExpr (Description subj kvs                                                ) = description subj kvs
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

entity i kvs = undefined
agent i kvs = undefined
activity i mStart mEnd kvs = undefined
wasGeneratedBy mI subj mObj mTime kvs = undefined
used mI subj mObj mTime kvs = undefined
wasStartedBy mI subj mObj mParentTrigger mTime kvs = undefined
wasEndedBy mI subj  mObj mParentTrigger mTime kvs = undefined
wasInformedBy mI subj mObj kvs = undefined
wasAssociatedWith mI subj mObj mPlan kvs = undefined
wasDerivedFrom mI subj mObj mGeneratingEnt mGeneratingAct useId kvs = undefined
wasAttributedTo mI subj obj kvs = undefined
isPartOf subj obj = undefined
description subj kvs = undefined
