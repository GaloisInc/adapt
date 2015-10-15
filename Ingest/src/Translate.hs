-- | Translate the parsed AST ('ParseCore's 'Prov' type) into our domain
-- representation (list of 'Entity').
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ViewPatterns               #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE PartialTypeSignatures      #-}

module Translate
  ( translate
  ) where

import Namespaces as NS
import ParserCore
import LexerCore (parseUTC) -- XXX move this
import qualified Types as T
import           Types (TranslateError(..))

import           Data.DList
import           Data.Monoid ((<>))
import           Data.Maybe (catMaybes)
import qualified Data.Map.Strict as Map
import           Data.Map.Strict (Map)
import qualified Data.Generics.Uniplate.Operations as Uniplate
import           Data.Generics.Uniplate.Data ()
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import           MonadLib


--------------------------------------------------------------------------------
--  Translate Monad

newtype Tr a = Tr { unTr :: WriterT (DList T.Warning) (ExceptionT TranslateError Id) a }
  deriving (Monad, Applicative, Functor)

instance ExceptionM Tr TranslateError where
  raise = Tr . raise

instance RunExceptionM Tr TranslateError where
  try = Tr . try . unTr

instance WriterM Tr T.Warning where
  put w = Tr (put $ singleton w)

runTr :: Tr a -> Either TranslateError (a,[T.Warning])
runTr = either Left (Right . (\(a,b) -> (a,toList b))) . runId . runExceptionT . runWriterT . unTr

safely :: Tr a -> Tr (Maybe a)
safely m =
  do x <- try m
     case x of
        Right r -> return $ Just r
        Left (TranslateError txt) -> warn txt >> return Nothing
        Left e  -> warn (L.pack $ show e) >> return Nothing

warn :: Text -> Tr ()
warn = put . T.Warn

--------------------------------------------------------------------------------
--  Translation

translate ::  Prov -> Either TranslateError ([T.Stmt], [T.Warning])
translate p = runTr $ tExprs (expandDomains p)

-- Everywhere the 'domain' of an 'Ident' is a member prefixMap, replace
-- it with the key value.
expandDomains :: Prov -> Prov
expandDomains (Prov ps es) = Prov ps (Uniplate.transformBi f es)
  where
  f :: Ident -> Ident
  f i@(Qualified d l) = maybe i (\d' -> mkIdent d' l) $ Map.lookup d m
  f i                 = i

  m = Map.fromList $ ("prov", NS.prov) : [(l,v) | Prefix (Unqualified l) v <- ps]

tExprs :: Prov -> Tr [T.Stmt]
tExprs (Prov _prefix es) = catMaybes <$> mapM tExpr es

tExpr :: Expr -> Tr (Maybe T.Stmt)
tExpr (Entity i kvs                                                        ) = safely (T.StmtEntity <$> entity i kvs)
tExpr (Activity i mStart mEnd kvs                                          ) = safely (T.StmtEntity <$> activity i mStart mEnd kvs)
tExpr (Agent i kvs                                                         ) = safely (T.StmtEntity    <$> agent i kvs)
tExpr (WasGeneratedBy mI subj mObj mTime kvs                               ) = safely (T.StmtPredicate <$> wasGeneratedBy mI subj mObj mTime kvs)
tExpr (Used mI subj mObj mTime kvs                                         ) = safely (T.StmtPredicate <$> used mI subj mObj mTime kvs)
tExpr (WasStartedBy mI subj mObj mParentTrigger mTime kvs                  ) = safely (T.StmtPredicate <$> wasStartedBy mI subj mObj mParentTrigger mTime kvs)
tExpr (WasEndedBy mI subj  mObj mParentTrigger mTime kvs                   ) = safely (T.StmtPredicate <$> wasEndedBy mI subj  mObj mParentTrigger mTime kvs)
tExpr (WasInformedBy mI subj mObj kvs                                      ) = safely (T.StmtPredicate <$> wasInformedBy mI subj mObj kvs)
tExpr (WasAssociatedWith mI subj mObj mPlan kvs                            ) = safely (T.StmtPredicate <$> wasAssociatedWith mI subj mObj mPlan kvs)
tExpr (WasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs  ) = safely (T.StmtPredicate <$> wasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs)
tExpr (WasAttributedTo mI subj obj kvs                                     ) = safely (T.StmtPredicate <$> wasAttributedTo mI subj obj kvs)
tExpr (IsPartOf subj obj                                                   ) = safely (T.StmtPredicate <$> isPartOf subj obj)
tExpr (Description subj kvs                                                ) = safely (T.StmtPredicate <$> description subj kvs)
tExpr (RawEntity i args kvs                                                )
    | i == mkIdent dc "description" =
        case args of
          [s]   -> tExpr (Description s kvs)
          _     -> fail $ "Dublin core 'description' requires a single argument, has " ++ show (length args)
    | i == mkIdent dc "isPartOf" =
        case (args,kvs) of
          ([s,o],[]) -> tExpr (IsPartOf s o)
          _     -> fail $ "Dublin core 'isPartOf' requires two argument and no attributes, but has " ++ show (length args, length kvs)
tExpr (RawEntity i _args _kvs) = fail $ "Unknown prov element: " ++ show i


--------------------------------------------------------------------------------
--  Entity Translation

-- If
--   * it has 'devType' is Resource.
--   * it has prov:type='adapt:artifact' is Artifact
entity :: Ident -> KVs -> Tr T.Entity
entity i kvs =
  case pTy of
    Just T.TyArtifact -> artifact i kvs
    Nothing ->
      case lookup adaptDevType kvs of
        Just devtype    -> resource i devtype kvs
        Nothing         -> raise $ TranslateError $
                            L.unlines [ "Unrecognized entity: " <> textOfIdent i
                                      , "\tprovType: " <> L.pack (show pTy)
                                      , "\tAttributes: " <> (L.pack $ show kvs)
                                      ]
 where pTy = getProvType kvs

artifact :: Ident -> KVs -> Tr T.Entity
artifact i kvs = T.Artifact (textOfIdent i) <$> artifactAttrs kvs

resource :: Ident -> Value -> KVs -> Tr T.Entity
resource i (ValString devTy) kvs = return $ T.Resource (textOfIdent i) dev devId
 where dev =
        case L.toLower devTy of
          "gps"             -> T.GPS
          "camera"          -> T.Camera
          "keyboard"        -> T.Keyboard
          "accelerometer"   -> T.Accelerometer
          _                 -> T.OtherDev devTy
       devId =
        case lookup adaptDeviceID kvs of
          Just (ValString x) -> Just x
          _                  -> Nothing
resource i _ _ = raise $ TranslateError $ "Device types must be string literals.  See device " <> textOfIdent i

-- Agents are either units of execution or exist largely for the metadata
-- of machine ID, foaf:name, and foaf:accountName.
agent :: Ident -> KVs -> Tr T.Entity
agent i kvs
  | getProvType kvs == Just T.TyUnitOfExecution =
      do attrs <- uoeAttrs kvs
         return $ T.UnitOfExecution (textOfIdent i) attrs
  | otherwise = T.Agent (textOfIdent i) <$> agentAttrs kvs

-- All Activities are units of execution, no exceptions.
activity :: Ident -> Maybe Time -> Maybe Time -> KVs -> Tr T.Entity
activity i mStart mEnd kvs
  | getProvType kvs == Just T.TyUnitOfExecution =
       do attrs <- uoeAttrs kvs
          return $ T.UnitOfExecution (textOfIdent i) (uoeTimes mStart mEnd ++ attrs)
  | otherwise = raise $ TranslateError "The only recognized activities are prov:type=adapt:UnitOfExecution"

uoeTimes :: Maybe Time -> Maybe Time -> [T.UoeAttr]
uoeTimes a b = catMaybes $ zipWith fmap [T.UAStarted, T.UAEnded] [a,b]

getProvType :: KVs -> Maybe T.Type
getProvType m =
  case lookup provType m of
    Nothing -> Nothing
    Just (ValIdent i) | i == adaptUnitOfExecution -> Just T.TyUnitOfExecution
                      | i == adaptArtifact        -> Just T.TyArtifact
    _                                  -> Nothing -- warn?

--------------------------------------------------------------------------------
--  Entity Attributes

uoeAttrs :: KVs -> Tr [T.UoeAttr]
uoeAttrs kvs = catMaybes <$> mapM (uncurry uoeAttr) kvs

uoeAttr :: Ident -> Value -> Tr (Maybe T.UoeAttr)
uoeAttr i = attrOper uoeAttrTranslations w i
 where w = warnN $ "Unrecognized attribute for UnitOfExecution: " <> (textOfIdent i)

(.->) :: a -> b -> (a,b)
(.->) a b = (a,b)

infixr 5 .->

uoeAttrTranslations :: Map Ident (Value -> Tr (Maybe T.UoeAttr))
uoeAttrTranslations = Map.fromList
  [ adaptMachineID .-> warnOrOp "Non-string value in adapt:machine" (T.UAMachine . T.UUID) . valueString
  , adaptPid       .-> warnOrOp "Non-Num value in adapt:PID" (T.UAPID . T.PID . L.pack . show) . valueNum
  , provAtTime     .-> (\v -> return $ let mtime = case v of
                                                      ValString s -> parseUTC s
                                                      ValTime t   -> Just t
                                                      _           -> Nothing
                                       in fmap T.UAStarted mtime)
  , foafAccountName .-> warnOrOp "Non-string value in foaf:accountName" T.UAUser . valueString
  , adaptCmdLine    .-> warnConst "Command lines have no matchine Adapt UoEAttr"
  , adaptCmdString  .-> warnConst "Command string have no matchine Adapt UoEAttr"
  ]


agentAttrs :: KVs -> Tr [T.AgentAttr]
agentAttrs kvs = catMaybes <$> mapM (uncurry agentAttr) kvs

agentAttr :: Ident -> Value -> Tr (Maybe T.AgentAttr)
agentAttr i = attrOper agentAttrTranslations w i
  where w = warnN $ "Unrecognized attribute for Agent: " <> (textOfIdent i)

agentAttrTranslations :: Map Ident (Value -> Tr (Maybe T.AgentAttr))
agentAttrTranslations = Map.fromList
  [ adaptMachineID   .-> warnOrOp "Non-string value in adapt:machine"    (T.AAMachine . T.UUID) . valueString
  , foafName         .-> warnOrOp "Non-string value in foaf:name"        T.AAName . valueString
  , foafAccountName  .-> warnOrOp "Non-string value in foaf:accountName" T.AAUser . valueString
  ]

artifactAttrs :: KVs -> Tr [T.ArtifactAttr]
artifactAttrs kvs = catMaybes <$> mapM (uncurry artifactAttr) kvs

artifactAttr :: Ident -> Value -> Tr (Maybe T.ArtifactAttr)
artifactAttr i v = attrOper artifactAttrTranslations w i v
 where w = warnN $ "Unrecognized attribute for artifact: " <> (textOfIdent i)

artifactAttrTranslations :: Map Ident (Value -> Tr (Maybe T.ArtifactAttr))
artifactAttrTranslations = Map.fromList
  [ adaptArtifactType .-> warnOrOp "Non-string value in adapt:ArtifactType" T.ArtAType . valueString
  , adaptRegistryKey  .-> warnOrOp "Non-string value in adapt:RegistryKey"  T.ArtARegistryKey . valueString
  ]

attrOper :: Map Ident (Value -> a) -> a -> Ident -> Value -> a
attrOper m w i v =
  do let op = Map.lookup i m
     case Map.lookup i m of
      Just o  -> o v
      Nothing -> w

warnConst = const . warnN
warnN s   = warn s >> return Nothing

warnOrOp :: Text -> (a -> b) -> Maybe a -> Tr (Maybe b)
warnOrOp w f m = maybe (warn w >> return Nothing) (return . Just . f) m

--------------------------------------------------------------------------------
--  Predicate Translation

-- N.B. We drop all values that start with an underscore, such as the entity
-- and activity that cause a data derivation, or the "plan" for an
-- association.

-- XXX Parital type signatures until thing stop changing.

wasGeneratedBy :: _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
wasGeneratedBy mI subj (orBlank -> obj) (Just at) kvs = return $ predicate subj obj T.WasGeneratedBy (T.AtTime at : predAttr kvs)
wasGeneratedBy i  _    _          Nothing   _         = raise (MissingRequiredField (fmap textOfIdent i) "AtTime")

used :: _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
used _i subj (orBlank -> obj) (Just at) kvs = return $ predicate subj obj T.Used (T.AtTime at : predAttr kvs)
used i  _    _                 Nothing    _ = raise (MissingRequiredField (fmap textOfIdent i) "AtTime")

wasStartedBy :: _ -> _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
wasStartedBy _i subj (orBlank -> obj) _mParentTrigger (Just at) kvs = return $ predicate subj obj T.WasStartedBy (T.AtTime at : predAttr kvs)
wasStartedBy i  _    _                 _               Nothing    _ = raise (MissingRequiredField (fmap textOfIdent i) "AtTime")

wasEndedBy :: _ -> _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
wasEndedBy _i subj  (orBlank -> obj) _mParentTrigger (Just at) kvs = return $ predicate subj obj T.WasEndedBy (T.AtTime at : predAttr kvs)
wasEndedBy i  _     _                _               Nothing   _   = raise (MissingRequiredField (fmap textOfIdent i) "AtTime")

wasInformedBy :: _ -> _ -> _ -> _ -> Tr T.Predicate
wasInformedBy _i subj (orBlank -> obj) kvs                            = return $ predicate subj obj T.WasInformedBy (predAttr kvs)

wasAssociatedWith :: _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
wasAssociatedWith _i subj (orBlank -> obj) _mPlan kvs                 = return $ predicate subj obj T.WasAssociatedWith (predAttr kvs)

wasDerivedFrom  :: _ -> _ -> _ -> _ -> _ -> _ -> _ -> Tr T.Predicate
wasDerivedFrom _i subj obj _mGeneratingEnt _mGeneratingAct _useId kvs = return $ predicate subj obj T.WasDerivedFrom (predAttr kvs)

wasAttributedTo :: _ -> _ -> _ -> _ -> Tr T.Predicate
wasAttributedTo _i subj obj kvs                                       = return $ predicate subj obj T.WasAttributedTo (predAttr kvs)

-- Dublin Core
isPartOf :: _ -> _ -> Tr T.Predicate
isPartOf subj obj    = return $ predicate subj obj T.IsPartOf []

description :: _ -> _ -> Tr T.Predicate
description subj kvs = return $ predicate subj blankNode T.Description (predAttr kvs)

predicate :: Ident -> Ident -> T.PredicateType -> [T.PredicateAttr] -> T.Predicate
predicate s o ty attr = T.Predicate (textOfIdent s) (textOfIdent o) ty attr


predAttr :: KVs -> [T.PredicateAttr]
predAttr _ = [] -- XXX

orBlank :: Maybe Ident -> Ident
orBlank = maybe blankNode id

blankNode :: Ident
blankNode = Unqualified "_"
