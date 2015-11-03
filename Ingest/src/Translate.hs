-- | Translate the parsed AST ('ParseCore's 'Prov' type) into our domain
-- representation (list of 'Entity').
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ViewPatterns               #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE PatternSynonyms            #-}

module Translate
  ( translate
  ) where

import PP hiding ((<>))
import Namespaces as NS
import ParserCore
import Util (parseUTC)
import qualified Types as T
import           Types (TranslateError(..))

import           Control.Applicative
import           Data.DList hiding (map)
import           Data.Monoid ((<>))
import           Data.Maybe (catMaybes, maybeToList)
import qualified Data.Map.Strict as Map
import           Data.Map.Strict (Map)
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import           Data.Time (UTCTime(..), fromGregorian)
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

safely :: Tr (Maybe T.Stmt) -> Tr (Maybe T.Stmt)
safely m =
  do x <- try m
     case x of
        Right r                   -> return r
        Left (TranslateError txt) -> warn txt >> return Nothing
        Left (MissingRequiredTimeField i f stmt) ->
          do warn $ "Filling missing required time field with epoch (entity/field: " <> maybe "" id i <> " " <> f <> ")"
             return $ Just stmt
        Left e                    -> warn (L.pack $ show e) >> return Nothing

warn :: Text -> Tr ()
warn = put . T.Warn

--------------------------------------------------------------------------------
--  Translation

translate ::  Prov -> Either TranslateError ([T.Stmt], [T.Warning])
translate p = runTr (tExprs p)

tExprs :: Prov -> Tr [T.Stmt]
tExprs (Prov _prefix es) = catMaybes <$> mapM (safely . tExpr) es

pattern PIdent i <- Just (Left i)
pattern PTime t  <- Just (Right t)

-- View maybe time
vMTime (PTime t)   = Just t
vMTime _           = Nothing
vMIdent (PIdent i) = Just i
vMIdent _          = Nothing

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
tExpr r@(RawEntity pred mI _ _ loc)                                        = warnN $ "Unrecognized statement at " <> L.pack (pretty loc) <> "(" <> L.pack (show r) <> ")"

--------------------------------------------------------------------------------
--  Entity Translation

-- If
--   * it has 'devType' is Resource.
--   * it has prov:type='adapt:artifact' is Artifact
entity :: Ident -> KVs -> Tr T.Entity
entity i kvs =
  case eTy of
    Just T.TyArtifact -> artifact i kvs
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
              _                          -> Nothing

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
agent i kvs = T.Agent (textOfIdent i) <$> agentAttrs kvs

-- All Activities are units of execution, no exceptions.
activity :: Ident -> Maybe Time -> Maybe Time -> KVs -> Tr T.Entity
activity i mStart mEnd kvs =
   do attrs <- uoeAttrs kvs
      return $ T.UnitOfExecution (textOfIdent i) (uoeTimes mStart mEnd ++ attrs)

uoeTimes :: Maybe Time -> Maybe Time -> [T.UoeAttr]
uoeTimes a b = catMaybes $ zipWith fmap [T.UAStarted, T.UAEnded] [a,b]

getProvType :: KVs -> Maybe T.Type
getProvType m =
  case lookup provType m of
    Nothing -> Nothing
    Just (ValIdent i) | eqIdent adaptUnitOfExecution i -> Just T.TyUnitOfExecution
                      | eqIdent adaptArtifact i        -> Just T.TyArtifact
    _                                  -> Nothing -- warn?

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
  [ adaptMachineID .-> warnOrOp "Non-string value in adapt:machine" (T.UAMachine . T.UUID) . valueString
  , adaptPid       .-> warnOrOp "Non-string value in adapt:pid"  (T.UAPID . T.PID) . valueString
  , adaptPPid      .-> warnOrOp "Non-string value in adapt:ppid" (T.UAPPID . T.PID) . valueString
  , adaptPrivs     .-> warnOrOp "Non-string value in adapt:privs" T.UAHadPrivs . valueString
  , adaptPwd       .-> warnOrOp "Non-string value in adapt:pwd"   T.UAPWD . valueString
  , adaptGroup          .-> warnOrOp "Non-string value in adapt:group" T.UAGroup . valueString
  , adaptCommandLine    .-> warnOrOp "Non-string value in adapt:commandLine" T.UACommandLine . valueString
  , adaptSource         .-> warnOrOp "Non-string value in adapt:source"  T.UASource . valueString
  , adaptCWD            .-> warnOrOp "Non-string value in adapt:cwd" T.UACWD . valueString
  , adaptUID            .-> warnOrOp "Non-string value in adapt:uid" T.UAUID . valueString
  , adaptProgramName    .-> warnOrOp "Non-string value in adapt:programName" T.UAProgramName . valueString
  , provType       .-> ignore
  , provAtTime     .-> (\v -> return $ let mtime = case v of
                                                      ValString s -> parseUTC s
                                                      ValTime t   -> Just t
                                                      _           -> Nothing
                                       in fmap T.UAStarted mtime)
  , foafAccountName .-> warnOrOp "Non-string value in foaf:accountName" T.UAUser . valueString
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
  , provType         .-> ignore
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
  , adaptPath         .-> warnOrOp "Non-string value in adapt:path" T.ArtACoarseLoc . valueString
  , adaptDestinationAddress .-> warnOrOp "Non-string value in adapt:destinationAddress" T.ArtADestinationAddress . valueString
  , adaptDestinationPort    .-> warnOrOp "Non-string value in adapt:destinationPort" T.ArtADestinationPort . valueString
  , adaptSourceAddress .-> warnOrOp "Non-string value in adapt:sourceAddress" T.ArtASourceAddress . valueString
  , adaptSourcePort    .-> warnOrOp "Non-string value in adapt:sourcePort" T.ArtASourcePort . valueString
  , adaptHasVersion   .-> ignore
  , provType          .-> ignore
  , adaptEntityType   .-> ignore
  ]

attrOper :: Map Ident (Value -> a) -> a -> Ident -> Value -> a
attrOper m w i v =
  do let op = Map.lookup i m
     case Map.lookup i m of
      Just o  -> o v
      Nothing -> w

warnConst = const . warnN
warnN s   = warn s >> return Nothing

ignore = const (return Nothing)
warnOrOp :: Text -> (a -> b) -> Maybe a -> Tr (Maybe b)
warnOrOp w f m = maybe (warn w >> return Nothing) (return . Just . f) m

--------------------------------------------------------------------------------
--  Predicate Translation

-- N.B. We drop all values that start with an underscore, such as the entity
-- and activity that cause a data derivation, or the "plan" for an
-- association.

wasGeneratedBy :: Maybe Ident -> Ident -> Maybe Ident -> Maybe Time -> KVs -> Tr T.Predicate
wasGeneratedBy mI subj (orBlank -> obj) mTime kvs =
  let constr f = predicate subj obj T.WasGeneratedBy (f $ generationAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

used :: Maybe Ident -> Ident -> Maybe Ident -> Maybe Time -> KVs -> Tr T.Predicate
used mI subj (orBlank -> obj) mTime kvs =
  return $ predicate subj obj T.Used (maybeToList (T.AtTime <$> mTime) ++ usedAttr kvs)

wasStartedBy :: Maybe Ident -> Ident -> Maybe Ident -> d -> Maybe Time -> KVs -> Tr T.Predicate
wasStartedBy mI subj (orBlank -> obj) _mParentTrigger mTime kvs =
  let constr f = predicate subj obj T.WasStartedBy (f $ startedByAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

wasEndedBy :: Maybe Ident -> Ident -> Maybe Ident -> d -> Maybe Time -> KVs -> Tr T.Predicate
wasEndedBy mI subj  (orBlank -> obj) _mParentTrigger mTime kvs =
  let constr f = predicate subj obj T.WasEndedBy (f $ endedByAttr kvs)
  in return $ constr $ maybe id ((:) . T.AtTime) mTime

timeZero :: UTCTime
timeZero = UTCTime (fromGregorian 1900 1 1) 0

wasInformedBy :: a -> Ident -> Maybe Ident -> KVs -> Tr T.Predicate
wasInformedBy _i subj (orBlank -> obj) kvs                            = return $ predicate subj obj T.WasInformedBy (informedByAttr kvs)

wasAssociatedWith :: a -> Ident -> Maybe Ident -> d -> KVs -> Tr T.Predicate
wasAssociatedWith _i subj (orBlank -> obj) _mPlan kvs                 = return $ predicate subj obj T.WasAssociatedWith (associatedWithAttr kvs)

wasDerivedFrom  :: a -> Ident -> Ident -> d -> e -> f -> KVs -> Tr T.Predicate
wasDerivedFrom _i subj obj _mGeneratingEnt _mGeneratingAct _useId kvs = return $ predicate subj obj T.WasDerivedFrom (derivedFromAttr kvs)

wasAttributedTo :: a -> Ident -> Ident -> KVs -> Tr T.Predicate
wasAttributedTo _i subj obj kvs                                       = return $ predicate subj obj T.WasAttributedTo (attributedToAttr kvs)

-- Dublin Core
isPartOf :: Ident -> Ident -> Tr T.Predicate
isPartOf subj obj    = return $ predicate subj obj T.IsPartOf []

description :: Ident -> KVs -> Tr T.Predicate
description obj  kvs = return $ predicate blankNode obj T.Description (descriptionAttr kvs)

predicate :: Ident -> Ident -> T.PredicateType -> [T.PredicateAttr] -> T.Predicate
predicate s o ty attr = T.Predicate (textOfIdent s) (textOfIdent o) ty attr


--------------------------------------------------------------------------------
--  Attributes
--  XXX notice we do not warn when an attribute is dropped.  This might be
--  desirable in the future.

type PredAttrTrans = KVs -> [T.PredicateAttr]

generationAttr :: PredAttrTrans
generationAttr = catMaybes . map ge
 where
 ge (k,ValString v) | eqIdent adaptGenOp k   = Just $ T.GenOp v
                    | eqIdent nfoPermissions k = Just $ T.Permissions v
 ge _                                     = Nothing

usedAttr :: PredAttrTrans
usedAttr = catMaybes . map ua
 where
 ua (k,ValString v)  | eqIdent adaptArgs k      = Just $ T.Args v
                     | eqIdent adaptReturnVal k = Just $ T.ReturnVal v
                     | eqIdent nfoOperation k   = Just $ T.Operation v
                     | eqIdent adaptTime k      = T.AtTime <$> parseUTC v
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
  go (k,ValString v) | eqIdent provAtTime k = T.AtTime <$> parseUTC v
  go (k,ValTime   v) | eqIdent provAtTime k = Just $ T.AtTime v
  go (k,ValString v) | eqIdent adaptUseOp k = Just $ T.Operation v
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

descriptionAttr :: PredAttrTrans
descriptionAttr = catMaybes . map go
 where
 go (k,ValString v) = Just (T.Raw (textOfIdent k) v)
 go _               = Nothing

orBlank :: Maybe Ident -> Ident
orBlank = maybe blankNode id
