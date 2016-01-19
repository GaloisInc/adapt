-- | Translate the parsed AST ('ParseCore's 'Prov' type) into our domain
-- representation (list of 'Entity').
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ViewPatterns               #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE PatternSynonyms            #-}
{-# LANGUAGE CPP                        #-}

module Translate
  ( translate
  ) where

import PP hiding ((<>))
import Namespaces as NS
import Util (parseUTC)
import qualified Types as T
import           Types (TranslateError(..),Range(..))
import           ParserCore

#if (__GLASGOW_HASKELL__ < 710)
import           Control.Applicative
#endif
import           Data.DList hiding (map)
import           Data.Monoid ((<>))
import           Data.Maybe (catMaybes, maybeToList)
import qualified Data.Map.Strict as Map
import           Data.Map.Strict (Map)
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import           MonadLib as ML

--------------------------------------------------------------------------------
--  Translate Monad

newtype Tr a = Tr { unTr :: ReaderT Range (WriterT (DList T.Warning) (ExceptionT TranslateError Id)) a }
  deriving (Monad, Applicative, Functor)

instance ExceptionM Tr TranslateError where
  raise = Tr . raise

instance RunExceptionM Tr TranslateError where
  try = Tr . try . unTr

instance WriterM Tr T.Warning where
  put w = Tr (put $ singleton w)

instance ReaderM Tr Range where
  ask = Tr ask

instance RunReaderM Tr Range where
  local i (Tr m) = Tr (ML.local i m)

runTr :: Tr a -> Either TranslateError (a,[T.Warning])
runTr = either Left (Right . (\(a,b) -> (a,toList b))) . runId . runExceptionT . runWriterT . runReaderT NoLoc . unTr

safely :: T.Range -> Tr (Maybe T.Stmt) -> Tr (Maybe T.Stmt)
safely e m =
  do x <- try m
     case x of
        Right r                   -> return r
        Left (TranslateError txt) ->
            do warn (pretty e <> ": " <> txt)
               return Nothing

warn :: Text -> Tr ()
warn = put . T.Warn

--------------------------------------------------------------------------------
--  Translation

translate ::  Prov -> Either TranslateError ([T.Stmt], [T.Warning])
translate p = runTr (tExprs p)

tExprs :: Prov -> Tr [T.Stmt]
tExprs (Prov _prefix es) = catMaybes <$> mapM (\e -> safely (exprLocation e) (tExprLoc e)) es

tExprLoc :: Expr -> Tr (Maybe T.Stmt)
tExprLoc e = fmap (T.StmtLoc . T.Located l) <$> ML.local l (tExpr e)
 where l = exprLocation e

-- View maybe time
vMTime :: Maybe (Either t a) -> Maybe a
vMTime (Just (Right t)) = Just t
vMTime _                = Nothing

vMIdent :: Maybe (Either a t) -> Maybe a
vMIdent (Just (Left i)) = Just i
vMIdent _               = Nothing

eqIdent :: Ident -> Ident -> Bool
eqIdent p e =
  case e of
    Unqualified i -> i == NS.local p
    _             -> e == p
{-# INLINE eqIdent #-}

pattern Entity i kvs <- RawEntity (eqIdent provEntity -> True) Nothing [vMIdent -> Just i] kvs _
pattern Agent i kvs <- RawEntity (eqIdent provAgent -> True) Nothing [vMIdent -> Just i] kvs _
pattern Activity i mStart mEnd kvs <- RawEntity (eqIdent provActivity -> True) Nothing [vMIdent -> Just i, vMTime -> mStart, vMTime -> mEnd] kvs _
pattern Activity2 i kvs <- RawEntity (eqIdent provActivity -> True) Nothing [vMIdent -> Just i] kvs _
pattern WasGeneratedBy mI subj mObj t kvs <- RawEntity (eqIdent provWasGeneratedBy -> True) mI [vMIdent -> Just subj, vMIdent -> mObj, vMTime -> t] kvs _
pattern Used mI subj mObj mTime kvs <- RawEntity (eqIdent provUsed -> True) mI [vMIdent -> Just subj, vMIdent -> mObj, vMTime -> mTime] kvs _
pattern WasStartedBy mI subj mObj mPT mTime kvs <- RawEntity (eqIdent provWasStartedBy -> True) mI [vMIdent -> Just subj, vMIdent -> mObj, vMIdent -> mPT, vMTime -> mTime] kvs _
pattern NonStandardWasStartedBy mI subj mObj mPT kvs <- RawEntity (eqIdent provWasStartedBy -> True) mI [vMIdent -> Just subj, vMIdent -> mObj, vMIdent -> mPT] kvs _
pattern WasEndedBy mI subj mObj mParentTrigger mTime kvs <- RawEntity (eqIdent provWasEndedBy -> True) mI [vMIdent -> Just subj, vMIdent -> mObj, vMIdent -> mParentTrigger, vMTime -> mTime] kvs _
pattern WasInformedBy mI subj mObj kvs <- RawEntity (eqIdent provWasInformedBy -> True) mI [vMIdent -> Just subj, vMIdent -> mObj] kvs _
-- XXX add WasInvalidatedBy if used.
pattern WasAssociatedWith mI subj mObj mPlan kvs <- RawEntity (eqIdent provWasAssociatedWith -> True) mI ((vMIdent -> Just subj) : (vMIdent -> mObj) : (vMIdent -> mPlan) : _) kvs _
pattern WasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs <- RawEntity (eqIdent provWasDerivedFrom -> True) mI [vMIdent -> Just subj, vMIdent -> Just obj, vMIdent -> mGeneratingEnt, vMIdent -> mGeneratingAct, vMIdent -> useId] kvs _
pattern WasDerivedFrom2 mI subj obj kvs <- RawEntity (eqIdent provWasDerivedFrom -> True) mI [vMIdent -> Just subj, vMIdent -> Just obj] kvs _
pattern WasAttributedTo mI subj obj kvs <- RawEntity (eqIdent provWasAttributedTo -> True) mI [vMIdent -> Just subj, vMIdent -> Just obj] kvs _
pattern IsPartOf subj obj <- RawEntity (eqIdent dcIsPartOf -> True) Nothing [vMIdent -> Just subj, vMIdent -> Just obj] (null -> True) _
pattern Description obj kvs <- RawEntity (eqIdent dcDescription -> True) Nothing [vMIdent -> Just obj] kvs _

mkStmt :: T.Entity -> Maybe T.Stmt
mkStmt = Just . T.StmtEntity

mkPred :: T.Predicate -> Maybe T.Stmt
mkPred = Just . T.StmtPredicate

tExpr :: Expr -> Tr (Maybe T.Stmt)
tExpr (Used mI subj mObj mTime kvs)                                        = mkPred <$> used mI subj mObj mTime kvs
tExpr (Entity i kvs)                                                       = mkStmt <$> entity i kvs
tExpr (Activity i mStart mEnd kvs)                                         = mkStmt <$> activity i mStart mEnd kvs
tExpr (Activity2 i kvs)                                                    = mkStmt <$> activity i Nothing Nothing kvs
tExpr (WasGeneratedBy mI subj mObj mTime kvs)                              = mkPred <$> wasGeneratedBy mI subj mObj mTime kvs
tExpr (WasInformedBy mI subj mObj kvs)                                     = mkPred <$> wasInformedBy mI subj mObj kvs
tExpr (WasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs) = mkPred <$> wasDerivedFrom mI subj obj mGeneratingEnt mGeneratingAct useId kvs
tExpr (WasDerivedFrom2 mI subj obj kvs)                                    = mkPred <$> wasDerivedFrom mI subj obj Nothing Nothing Nothing kvs
tExpr (Agent i kvs)                                                        = mkStmt <$> agent i kvs
tExpr (WasStartedBy mI subj mObj mParentTrigger mTime kvs)                 = mkPred <$> wasStartedBy mI subj mObj mParentTrigger mTime kvs
tExpr (NonStandardWasStartedBy mI subj mObj mParentTrigger kvs)            = mkPred <$> wasStartedBy mI subj mObj mParentTrigger Nothing kvs
tExpr (WasEndedBy mI subj mObj mParentTrigger mTime kvs)                   = mkPred <$> wasEndedBy mI subj  mObj mParentTrigger mTime kvs
tExpr (WasAssociatedWith mI subj mObj mPlan kvs)                           = mkPred <$> wasAssociatedWith mI subj mObj mPlan kvs
tExpr (WasAttributedTo mI subj obj kvs)                                    = mkPred <$> wasAttributedTo mI subj obj kvs
tExpr (IsPartOf subj obj)                                                  = mkPred <$> isPartOf subj obj
tExpr (Description obj kvs)                                                = mkPred <$> description obj kvs
tExpr (RawEntity _pred _mI _ _ loc)                                      = warnN $ "Unrecognized statement " <> L.pack (pretty loc)

--------------------------------------------------------------------------------
--  Entity Translation

-- If
--   * it has 'devType' is Resource.
--   * it has prov:type='adapt:artifact' is Artifact
entity :: Ident -> KVs -> Tr T.Entity
entity i kvs =
  case eTy of
    Just T.TyArtifact -> artifact i kvs
    Just _            -> raise $ TranslateError $ "Invalid type for entity: " <> textOfIdent i
    Nothing ->
      case lookup adaptDevType kvs of
        Just devtype    -> resource i devtype kvs
        Nothing         -> raise $ TranslateError $
                            L.unlines [ "Unrecognized entity: " <> textOfIdent i
                                      , "\tAttributes: " <> (L.pack $ show kvs)
                                      ]
 where eTy = case lookup adaptEntityType kvs of
              Just (ValString "file")    -> Just T.TyArtifact
              Just (ValString "network") -> Just T.TyArtifact
              Just (ValString "registryEntry") -> Just T.TyArtifact
              _                          -> Just T.TyArtifact -- XXX for the demo, for 5D

artifact :: Ident -> KVs -> Tr T.Entity
artifact i kvs = T.Artifact i <$> artifactAttrs kvs

resource :: Ident -> Value -> KVs -> Tr T.Entity
resource i (ValString devTy) kvs = return $ T.Resource i dev devId
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
agent i kvs = T.Agent i <$> agentAttrs kvs

-- All Activities are units of execution, no exceptions.
activity :: Ident -> Maybe Time -> Maybe Time -> KVs -> Tr T.Entity
activity i mStart mEnd kvs =
   do attrs <- uoeAttrs kvs
      return $ T.UnitOfExecution i (uoeTimes mStart mEnd ++ attrs)

uoeTimes :: Maybe Time -> Maybe Time -> [T.UoeAttr]
uoeTimes a b = catMaybes $ zipWith fmap [T.UAStarted, T.UAEnded] [a,b]

--------------------------------------------------------------------------------
--  Entity Attributes

(.->) :: a -> b -> (a,b)
(.->) a b = (a,b)

infixr 5 .->

uoeAttrs :: KVs -> Tr [T.UoeAttr]
uoeAttrs kvs = catMaybes <$> mapM (uncurry uoeAttr) kvs

uoeAttr :: Ident -> Value -> Tr (Maybe T.UoeAttr)
uoeAttr i = attrOper uoeAttrTranslations w i
 where w = warnN $ "Unrecognized attribute for UnitOfExecution: " <> (textOfIdent i)

uoeAttrTranslations :: Map Ident (Value -> Tr (Maybe T.UoeAttr))
uoeAttrTranslations = Map.fromList
  [ adaptMachineID      .-> warnOrOp "Non-string value in adapt:machine" T.UAMachine . valueString
  , adaptPid            .-> warnOrOp "Non-string value in adapt:pid"  T.UAPID . valueString
  , adaptPPid           .-> warnOrOp "Non-string value in adapt:ppid" T.UAPPID . valueString
  , adaptPrivs          .-> warnOrOp "Non-string value in adapt:privs" T.UAHadPrivs . valueString
  , adaptPwd            .-> warnOrOp "Non-string value in adapt:pwd"   T.UAPWD . valueString
  , adaptGroup          .-> warnOrOp "Non-string value in adapt:group" T.UAGroup . valueString
  , adaptCommandLine    .-> warnOrOp "Non-string value in adapt:commandLine" T.UACommandLine . valueString
  , adaptSource         .-> warnOrOp "Non-string value in adapt:source"  T.UASource . valueString
  , adaptCWD            .-> warnOrOp "Non-string value in adapt:cwd" T.UACWD . valueString
  , adaptUID            .-> warnOrOp "Non-string value in adapt:uid" T.UAUID . valueString
  , adaptProgramName    .-> warnOrOp "Non-string value in adapt:programName" T.UAProgramName . valueString
  , provAtTime          .-> ignore
  -- XXX add Started at and ended at time support.
  , adaptTime           .-> ignore
  , provType            .-> ignore
  , foafAccountName     .-> warnOrOp "Non-string value in foaf:accountName" T.UAUser . valueString
  ]

agentAttrs :: KVs -> Tr [T.AgentAttr]
agentAttrs kvs = catMaybes <$> mapM (uncurry agentAttr) kvs

agentAttr :: Ident -> Value -> Tr (Maybe T.AgentAttr)
agentAttr i = attrOper agentAttrTranslations w i
  where w = warnN $ "Unrecognized attribute for Agent: " <> (textOfIdent i)

agentAttrTranslations :: Map Ident (Value -> Tr (Maybe T.AgentAttr))
agentAttrTranslations = Map.fromList
  [ adaptMachineID   .-> warnOrOp "Non-string value in adapt:machine"    T.AAMachine . valueString
  -- XXX Remove? , foafName         .-> warnOrOp "Non-string value in foaf:name"        T.AAName . valueString
  -- XXX Remove? , foafAccountName  .-> warnOrOp "Non-string value in foaf:accountName" T.AAUser . valueString
  , provType         .-> ignore
  ]

artifactAttrs :: KVs -> Tr [T.ArtifactAttr]
artifactAttrs kvs = catMaybes <$> mapM (uncurry artifactAttr) kvs

artifactAttr :: Ident -> Value -> Tr (Maybe T.ArtifactAttr)
artifactAttr i v = attrOper artifactAttrTranslations w i v
 where w = warnN $ "Unrecognized attribute for artifact: " <> (textOfIdent i)

artifactAttrTranslations :: Map Ident (Value -> Tr (Maybe T.ArtifactAttr))
artifactAttrTranslations = Map.fromList
  [ adaptRegistryKey        .-> warnOrOp "Non-string value in adapt:registryKey"  T.ArtARegistryKey . valueString
  , adaptPath               .-> warnOrOp "Non-string value in adapt:path" T.ArtACoarseLoc . valueString
  , adaptDestinationAddress .-> warnOrOp "Non-string value in adapt:destinationAddress" T.ArtADestinationAddress . valueString
  , adaptDestinationPort    .-> warnOrOp "Non-string value in adapt:destinationPort" T.ArtADestinationPort . valueString
  , adaptSourceAddress      .-> warnOrOp "Non-string value in adapt:sourceAddress" T.ArtASourceAddress . valueString
  , adaptSourcePort         .-> warnOrOp "Non-string value in adapt:sourcePort" T.ArtASourcePort . valueString
  , adaptHasVersion         .-> ignore
  , provType                .-> ignore
  , adaptEntityType         .-> ignore
  ]

attrOper :: Map Ident (Value -> a) -> a -> Ident -> Value -> a
attrOper m w i v =
  case Map.lookup i m of
      Just o  -> o v
      Nothing -> w

warnN :: Text -> Tr (Maybe a)
warnN s   = warn s >> return Nothing

ignore :: b -> Tr (Maybe a)
ignore = const (return Nothing)

warnOrOp :: Text -> (a -> b) -> Maybe a -> Tr (Maybe b)
warnOrOp w f m =
  do l <- ask
     let w' = pretty l <> ": " <> w
     maybe (warn w' >> return Nothing) (return . Just . f) m

--------------------------------------------------------------------------------
--  Predicate Translation

-- N.B. We drop all values that start with an underscore, such as the entity
-- and activity that cause a data derivation, or the "plan" for an
-- association.

wasGeneratedBy :: Maybe Ident -> Ident -> Maybe Ident -> Maybe Time -> KVs -> Tr T.Predicate
wasGeneratedBy _mI subj (orBlank -> obj) mTime kvs =
  let constr f = predicate subj obj Nothing T.WasGeneratedBy (f $ generationAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

used :: Maybe Ident -> Ident -> Maybe Ident -> Maybe Time -> KVs -> Tr T.Predicate
used mI subj (orBlank -> obj) mTime kvs =
  return $ predicate subj obj mI T.Used (maybeToList (T.AtTime <$> mTime) ++ usedAttr kvs)

wasStartedBy :: Maybe Ident -> Ident -> Maybe Ident -> d -> Maybe Time -> KVs -> Tr T.Predicate
wasStartedBy _mI subj (orBlank -> obj) _mParentTrigger mTime kvs =
  let constr f = predicate subj obj Nothing T.WasStartedBy (f $ startedByAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

wasEndedBy :: Maybe Ident -> Ident -> Maybe Ident -> d -> Maybe Time -> KVs -> Tr T.Predicate
wasEndedBy _mI subj  (orBlank -> obj) _mParentTrigger mTime kvs =
  let constr f = predicate subj obj Nothing T.WasEndedBy (f $ endedByAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

wasInformedBy :: a -> Ident -> Maybe Ident -> KVs -> Tr T.Predicate
wasInformedBy _i subj (orBlank -> obj) kvs                            = return $ predicate subj obj Nothing T.WasInformedBy (informedByAttr kvs)

wasAssociatedWith :: a -> Ident -> Maybe Ident -> d -> KVs -> Tr T.Predicate
wasAssociatedWith _i subj (orBlank -> obj) _mPlan kvs                 = return $ predicate subj obj Nothing T.WasAssociatedWith (associatedWithAttr kvs)

wasDerivedFrom  :: a -> Ident -> Ident -> d -> e -> f -> KVs -> Tr T.Predicate
wasDerivedFrom _i subj obj _mGeneratingEnt _mGeneratingAct _useId kvs = return $ predicate subj obj Nothing T.WasDerivedFrom (derivedFromAttr kvs)

wasAttributedTo :: a -> Ident -> Ident -> KVs -> Tr T.Predicate
wasAttributedTo _i subj obj kvs                                       = return $ predicate subj obj Nothing T.WasAttributedTo (attributedToAttr kvs)

-- Dublin Core
isPartOf :: Ident -> Ident -> Tr T.Predicate
isPartOf subj obj    = return $ predicate subj obj Nothing T.IsPartOf []

description :: Ident -> KVs -> Tr T.Predicate
description obj  kvs = predicate blankNode obj Nothing T.Description <$> descriptionAttr kvs

predicate :: Ident -> Ident -> Maybe Ident -> T.PredicateType -> [T.PredicateAttr] -> T.Predicate
predicate s o mI ty attr = T.Predicate s o mI ty attr


--------------------------------------------------------------------------------
--  Attributes
--  XXX notice we do not warn when an attribute is dropped.  This might be
--  desirable in the future.

type PredAttrTrans = KVs -> [T.PredicateAttr]

generationAttr :: PredAttrTrans
generationAttr = catMaybes . map ge
 where
 ge (k,ValString v) | eqIdent adaptGenOp k     = Just $ T.GenOp v
                    | eqIdent adaptPermissions k = Just $ T.Permissions v -- XXX prov tc permissions
 ge _                                          = Nothing

usedAttr :: PredAttrTrans
usedAttr = catMaybes . map ua
 where
 ua (k,ValString v)  | eqIdent adaptArgs k      = Just $ T.Args v
                     | eqIdent adaptReturnVal k = Just $ T.ReturnVal v
                     | eqIdent adaptOperation k = Just $ T.Operation v
                     | eqIdent adaptTime k      = T.AtTime <$> parseUTC v
                     -- XXX entry address into file, etc
 ua _                                           = Nothing

startedByAttr :: PredAttrTrans
startedByAttr = const []

endedByAttr :: PredAttrTrans
endedByAttr = catMaybes . map eb
 where
 eb (k,ValString v) | eqIdent adaptExecOp k = Just $ T.ExecOp v
 eb _                                       = Nothing

informedByAttr :: PredAttrTrans
informedByAttr = catMaybes . map go
 where
  go (k,ValString v) | eqIdent provAtTime k = T.AtTime <$> parseUTC v   -- XXX drop
  go (k,ValTime   v) | eqIdent provAtTime k = Just $ T.AtTime v         --- XXX drop
  go (k,ValString v) | eqIdent adaptUseOp k = Just $ T.Operation v -- XXX tc:operation
  go _                                      = Nothing

associatedWithAttr :: PredAttrTrans
associatedWithAttr = const []

derivedFromAttr :: PredAttrTrans
derivedFromAttr = catMaybes . map go
 where
 go (k,ValString v) | eqIdent provAtTime    k = T.AtTime <$> parseUTC v
 go (k,ValTime   v) | eqIdent provAtTime    k = Just $ T.AtTime v
 go (k,ValString v) | eqIdent adaptDeriveOp k = Just $ T.DeriveOp v
 go _                                         = Nothing

attributedToAttr :: PredAttrTrans
attributedToAttr = const []

descriptionAttr :: KVs -> Tr [T.PredicateAttr]
descriptionAttr kvs = catMaybes <$> mapM (uncurry descrAttr) kvs

descrAttr :: Ident -> Value -> Tr (Maybe T.PredicateAttr)
descrAttr i = attrOper descrAttrTranslations w i
  where w = warnN $ "Unrecognized attribute for description: " <> (textOfIdent i)


descrAttrTranslations :: Map Ident (Value -> Tr (Maybe T.PredicateAttr))
descrAttrTranslations = Map.fromList
  [ adaptMachineID          .-> warnOrOp "Non-string value in adapt:machine" T.MachineID . valueString
  , adaptSourceAddress      .-> warnOrOp "Non-string value in adapt:sourceAddress" T.SourceAddress . valueString
  , adaptDestinationAddress .-> warnOrOp "Non-string value in adapt:destinationAddress" T.DestinationAddress . valueString
  , adaptSourcePort         .-> warnOrOp "Non-string value in adapt:sourcePort" T.SourcePort . valueString
  , adaptDestinationPort    .-> warnOrOp "Non-string value in adapt:destinationPort" T.DestinationPort . valueString
  , adaptProtocol           .->
      \v -> warnOrOp ("Non-num value in adapt:protocol" <> L.pack (show v)) T.Protocol . fmap (L.pack . show) . valueNum $ v
  , adaptEntityType         .-> ignore
  ]

orBlank :: Maybe Ident -> Ident
orBlank = maybe blankNode id
