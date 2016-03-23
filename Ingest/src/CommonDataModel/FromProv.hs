{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE RecordWildCards            #-}
{-# LANGUAGE TupleSections              #-}
module CommonDataModel.FromProv
  ( readFileCDM
  , translateTextCDM
  , translate
  , module CDM
  , T.Error(..), Warning(..)
  ) where

import qualified Control.Exception as X
import           Control.Monad.CryptoRandom
import           Crypto.Random (SystemRandom)
import           Data.DList (DList, toList, singleton)
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Monoid
import qualified Data.Text      as TextStrict
import           Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import           Data.Time (UTCTime(..))
import           MonadLib as ML
import           Text.Read (readMaybe)

import           CommonDataModel as CDM
import           Namespaces
import           Parser (parseProvN)
import           ParserCore
import qualified ParserCore as T
import           PP (pretty)
import           Types (TranslateError(..),Range(..))
import qualified Types as T


readFileCDM ::  FilePath -> IO ([CDM.Node], [CDM.Edge], [Warning])
readFileCDM fp =
  either X.throw return =<< translateTextCDM =<< Text.readFile fp

translateTextCDM ::  Text -> IO (Either T.Error ([CDM.Node], [CDM.Edge], [Warning]))
translateTextCDM t = do
  res  <- pure (parseProvN t)
  res2 <- either (pure . Left . T.PE) (fmap (either (Left . T.TRE) Right) . translate) res
  case res2 of
    Right ((ns,es), ws) -> return $ Right (ns,es,ws)
    Left err            -> return $ Left err

translate :: Prov -> IO (Either TranslateError (([CDM.Node], [CDM.Edge]), [Warning]))
translate (Prov _prefix es) = runTr $
  do (as,bs) <- unzip <$> mapM (\e -> ML.local (exprLocation e) (translateProvExpr e)) es
     return (concat as, concat bs)

translateProvExpr :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvExpr e@(RawEntity {..})
  | Just f <- Map.lookup (Namespaces.local exprOper) operHandler = f e
  | otherwise = do warn $ "No CDM interpretation for operation: " <> textOfIdent exprOper
                   noResults
  where
  operHandler :: Map Text (T.Expr -> Tr ([CDM.Node], [CDM.Edge]))
  operHandler =
    Map.fromList [ ("entity"            , translateProvEntity)
                 , ("agent"             , translateProvAgent)
                 , ("activity"          , translateProvActivity)
                 , ("wasAssociatedWith" , translateProvWasAssociatedWith)
                 , ("used"              , translateProvUsed)
                 , ("wasStartedBy"      , translateProvWasStartedBy)
                 , ("wasGeneratedBy"    , translateProvWasGeneratedBy)
                 , ("wasEndedBy"        , translateProvWasEndedBy)
                 , ("wasInformedBy"     , translateProvWasInformedBy)
                 , ("wasAttributedTo"   , translateProvWasAttributedTo)
                 , ("wasDerivedFrom"    , translateProvWasDerivedFrom)
                 , ("actedOnBehalfOf"   , translateProvActedOnBehalfOf)
                 , ("wasInvalidatedBy"  , translateProvWasInvalidatedBy)
                 ]

translateProvEntity, translateProvAgent :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvActivity, translateProvWasAssociatedWith :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvUsed, translateProvWasStartedBy :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvWasGeneratedBy, translateProvWasEndedBy :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvWasInformedBy, translateProvWasAttributedTo :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvWasDerivedFrom, translateProvActedOnBehalfOf :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvWasInvalidatedBy :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])

translateProvEntity (RawEntity {..}) =
  case artifact of
    "file"          -> translateProvFile
    "network"       -> translateProvNetwork
    ""              -> translateProvDevice
    "registryEntry" -> noResults -- CDM does not handle registry entries.
    _               -> do warn $ "Invalid artifact type for entity: " <> artifact
                          noResults
 where
  src      = SourceLinuxAuditTrace
  artifact = maybe "" id $ valueString =<< lookup adaptEntityType exprAttrs
  translateProvNetwork = do
    uid <- uidOfMay exprIdent
    let info = noInfo
    srcIP   <- kvStringReq adaptSourceAddress exprAttrs
    dstIP   <- kvStringReq adaptDestinationAddress exprAttrs
    srcPort <- kvIntegralReq adaptSourcePort exprAttrs
    dstPort <- kvIntegralReq adaptDestinationPort exprAttrs
    let net = (NodeEntity $ NetFlow src uid info (strict srcIP) (strict dstIP) srcPort dstPort)
    residesOn net =<< kvString adaptMachineID exprAttrs
  translateProvFile    = do
    uid <- uidOfMay exprIdent
    url <- strict <$> kvStringReq adaptPath exprAttrs -- TODO old prov uses 'filePath'
    let info = noInfo
        ver  = -1
        sz   = Nothing
    let file = NodeEntity $ File src uid info url ver sz
    residesOn file =<< kvString adaptMachineID exprAttrs
  translateProvDevice  = do
    -- From the machine ID, create a new node 'Host', and edge 'residesOn'.
    uid     <- uidOfMay exprIdent
    devType <- kvGetDevice adaptDeviceID exprAttrs
    let res = NodeResource $ Resource src devType uid noInfo
    residesOn res =<< kvString adaptMachineID exprAttrs

residesOn :: Node -> Maybe Text -> Tr ([Node], [Edge])
residesOn ent machine =
    case machine of
      Just mid -> do
           hostUID <- uidOfMachine mid
           let host   = NodeHost $ Host hostUID Nothing Nothing
               reside = Edge (nodeUID ent) hostUID ResidesOn
           return ([ent, host], [reside])
      Nothing -> node ent

translateProvAgent (RawEntity {..}) =
  -- warn "We just throw Prov Agents away when generating CDM."
  noResults

translateProvActivity (RawEntity {..}) =
  do let subjectSource = SourceLinuxAuditTrace
     subjectUID <- uidOfMay exprIdent
     let subjectType = SubjectProcess
         fakeDay = UTCTime (toEnum 0) 0
         err = warn "Prov Activity (CDM Subject) has no start time." >> return fakeDay
     subjectStartTime   <- maybe err return =<< kvTime adaptTime exprAttrs
     subjectPID         <- kvStringToIntegral adaptPid exprAttrs
     subjectPPID        <- kvStringToIntegral adaptPPid exprAttrs
     subjectUnitID      <- return Nothing
     subjectEndTime     <- return Nothing
     subjectCommandLine <- fmap strict <$> kvString adaptCommandLine exprAttrs
     let subjectImportLibs      = Nothing
     let subjectExportLibs      = Nothing
     let subjectProcessInfo     = Nothing
     let subjectOtherProperties = Map.empty
     let subjectLocation        = Nothing
     let subjectSize            = Nothing
     let subjectPpt             = Nothing
     let subjectEnv             = Nothing
     let subjectArgs            = Nothing
     let nd = NodeSubject Subject {..}
     residesOn nd =<< kvString adaptMachineID exprAttrs

translateProvWasAssociatedWith (RawEntity {..}) =
  warn "No known CDM for 'wasAssociatedWith'" >> noResults
translateProvUsed (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId),Just (Right time)] ->
        do operation <- kvString adaptOperation exprAttrs
           edgeSource <- uidOf edgeSourceId
           edgeDestination <- uidOf edgeDestinationId
           (edgeRelationship,eTy) <-
                (Used,) <$> case operation of
                         Just "open"          -> return $ EventOpen
                         Just "bind"          -> return $ EventBind
                         Just "connect"       -> return $ EventConnect
                         Just "accept"        -> return $ EventAccept
                         Just "read"          -> return $ EventRead
                         Just "mmap"          -> return $ EventMmap
                         Just "mprotect"      -> return $ EventMprotect
                         Just "close"         -> return $ EventClose
                         Just "link"          -> return $ EventLink
                         Just "execute"       -> return $ EventExecute
                         Just other           -> -- 'recvfrom' etc
                            die $ "Unknown use operation: " <> other
                         Nothing              ->
                            die "No specific 'use' operation."
           -- Generate an event and two edges for new data model
           evtUID <- randomUID
           let evt = NodeSubject $ mkEvent src evtUID eTy Nothing time
               src = SourceLinuxAuditTrace
               wib = Edge edgeSource evtUID WasInformedBy
               use = Edge evtUID edgeDestination edgeRelationship
           return ([evt], [wib,use])
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasGeneratedBy (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId),Just (Right time)] ->
        do operation <- kvString adaptOperation exprAttrs
           edgeSource <- uidOf edgeSourceId
           edgeDestination <- uidOf edgeDestinationId
           (edgeRelationship,eTy) <-
            (WasGeneratedBy,) <$> case operation of
                                     Just "send"     -> return EventSend
                                     Just "connect"  -> return EventConnect
                                     Just "truncate" -> return EventTruncate
                                     Just "chmod"    -> return EventChmod
                                     Just "create"   -> return EventCreate
                                     Just other ->
                                        die $ "WasGeneratedBy: Unknown operation: " <> other
                                     Nothing ->
                                        die "WasGeneratedBy: No provided operation"
           evtUID <- randomUID
           let evt = NodeSubject $ mkEvent src evtUID eTy Nothing time
               src = SourceLinuxAuditTrace
               wib = Edge edgeSource evtUID WasInformedBy
               wgb = Edge evtUID edgeDestination edgeRelationship
           return ([evt], [wib,wgb])
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasStartedBy (RawEntity {..}) =
  case exprArgs of
    Just (Left edgeSourceId) : Just (Left edgeDestinationId) : _ ->
      do let err = die "No time found in 'wasStartedBy'"
         time <- maybe err return =<< kvTime adaptTime exprAttrs
         edgeSource <- uidOf edgeSourceId
         edgeDestination <- uidOf edgeDestinationId
         evtUID <- randomUID
         let evt = NodeSubject $ mkEvent src evtUID EventFork Nothing time
             src = SourceLinuxAuditTrace
             wsb = Edge edgeSource evtUID WasInformedBy
             wib = Edge evtUID edgeDestination WasInformedBy
         return ([evt], [wsb,wib])
    _ -> warn "Unrecognized 'wasStartedBy' relation." >> noResults
translateProvWasEndedBy (RawEntity {..}) =
  case exprArgs of
    Just (Left edgeSourceId) : Just (Left edgeDestinationId) : _ ->
      do let err = die "No time found in 'wasEndedBy'"
         time <- maybe err return =<< kvTime adaptTime exprAttrs
         edgeSource <- uidOf edgeSourceId
         edgeDestination <- uidOf edgeDestinationId
         evtUID <- randomUID
         let evt = NodeSubject $ mkEvent src evtUID EventStop Nothing time
             src = SourceLinuxAuditTrace
             web = Edge edgeSource evtUID WasInformedBy
             wib = Edge evtUID edgeDestination WasInformedBy
         return ([evt], [web,wib])
    _ -> warn "Unrecognized 'wasEndedBy' relation." >> noResults
translateProvWasInformedBy (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do edgeSource       <- uidOf edgeSourceId
           edgeDestination  <- uidOf edgeDestinationId
           edgeRelationship <- pure WasInformedBy
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'wasInformedBy' relation." >> noResults
translateProvWasAttributedTo (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do edgeSource       <- uidOf edgeSourceId
           edgeDestination  <- uidOf edgeDestinationId
           edgeRelationship <- pure WasAttributedTo
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'wasAttributedTo' relation." >> noResults
translateProvWasDerivedFrom (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do edgeDestination  <- uidOf edgeSourceId
           edgeSource       <- uidOf edgeDestinationId
           let rel = WasDerivedFrom UnknownStrength UnknownDerivation
           edge $ Edge edgeSource edgeDestination rel
    _ -> warn "Unrecognized 'wasDerivedFrom' relation." >> noResults
translateProvActedOnBehalfOf (RawEntity {..}) =
  do warn "Unimplemented: actedOnBehalfOf"
     noResults
translateProvWasInvalidatedBy (RawEntity {..}) =
  do warn "Unimplemented: wasInvalidatedBy"
     noResults

--------------------------------------------------------------------------------
--  Translate Monad

data Warning = Warn Text
  deriving (Eq, Ord, Show)

warn :: Text -> Tr ()
warn msg =
  do loc <- ask
     put (Warn (pretty loc <> msg))

die :: Text -> Tr a
die msg =
  do loc <- ask
     raise (TranslateError (pretty loc <> msg))

data TrState = TrState { trRandoms :: [Word64]
                       , trUIDs    :: Map Ident UID
                       , _trSequenceNumber :: Sequence
                       }

newtype Tr a = Tr { unTr :: StateT TrState (ReaderT Range (WriterT (DList Warning) (ExceptionT TranslateError Id))) a }
  deriving (Monad, Applicative, Functor)

instance ExceptionM Tr TranslateError where
  raise = Tr . raise

instance RunExceptionM Tr TranslateError where
  try = Tr . try . unTr

instance WriterM Tr Warning where
  put w = Tr (put $ singleton w)

instance ReaderM Tr Range where
  ask = Tr ask

instance RunReaderM Tr Range where
  local i (Tr m) = Tr (ML.local i m)

instance StateM Tr TrState where
  get   = Tr get
  set s = Tr (set s)

runTr :: Tr a -> IO (Either TranslateError (a,[Warning]))
runTr tr =
  do g <- newGenIO :: IO SystemRandom
     let trs = TrState (crandoms g) Map.empty 0
     pure (runTrWith trs tr)

runTrWith :: TrState -> Tr a -> Either TranslateError (a,[Warning])
runTrWith trs tr = warningsToList $ runStack tr
 where
 warningsToList = either Left (Right . (\(a,b) -> (a,toList b)))
 runStack :: Tr a -> Either TranslateError (a,DList Warning)
 runStack = runId . runExceptionT . runWriterT . runReaderT NoLoc . evalStateT trs . unTr
 evalStateT s m = fst <$> runStateT s m

randomUID :: Tr UID
randomUID = do
  st <- get
  case trRandoms st of
    (a:b:c:d:rest) -> do
      set st { trRandoms = rest }
      return (a,b,c,d)
    _ -> die "Impossible: Ran out of entropy generating randomUID."

insertUID :: Ident -> UID -> Tr ()
insertUID i u =
 do st <- get
    let mp = Map.insert i u (trUIDs st)
    mp `seq` set st { trUIDs = mp }

lookupUID :: Ident -> Tr (Maybe UID)
lookupUID i = Map.lookup i . trUIDs <$> get

uidOfMay :: Maybe Ident -> Tr UID
uidOfMay = maybe randomUID uidOf

uidOf :: Ident -> Tr UID
uidOf ident =
  do um <- lookupUID ident
     case um of
      Nothing -> do val <- randomUID
                    insertUID ident val
                    return val
      Just val -> return val

uidOfMachine :: Text -> Tr UID
uidOfMachine t = uidOf (adapt .: ("machine://" <> t))

_nextSequenceNumber :: Tr Sequence
_nextSequenceNumber =
 do s <- get
    let sqn = _trSequenceNumber s
    set s { _trSequenceNumber = sqn + 1 }
    return sqn

node :: Node -> Tr ([Node],[Edge])
node n = return ([n],[])

edge :: Edge -> Tr ([Node],[Edge])
edge e = return ([],[e])

noResults :: Tr ([Node],[Edge])
noResults = return ([],[])

kvStringToIntegral :: (Read a, Num a, Integral a) => Ident -> KVs -> Tr (Maybe a)
kvStringToIntegral i m = (join . fmap (readMaybe . Text.unpack)) <$> kvString i m

kvString :: Ident -> KVs -> Tr (Maybe Text)
kvString i m = return $ lookup i m >>= valueString

kvTime :: Ident -> KVs -> Tr (Maybe CDM.Time)
kvTime i m = return $ lookup i m >>= valueTime

kvStringReq :: Ident -> KVs -> Tr Text
kvStringReq i m =
 do str <- kvString i m
    case str of
      Just t  -> return t
      Nothing ->
        do warn $ "Required field is missing (" <> textOfIdent i <> ") filling in ''."
           return ""

kvGetDevice :: Ident -> KVs -> Tr SourceType
kvGetDevice i m =
 do md <- kvString i m
    case md of
     Just "Accelerometer"             -> return SourceAccelerometer
     Just "Temperature"               -> return SourceTemperature
     Just "Gyroscope"                 -> return SourceGyroscope
     Just "MagneticField"             -> return SourceMagneticField
     Just "HearRate"                  -> return SourceHearRate
     Just "Light"                     -> return SourceLight
     Just "Proximity"                 -> return SourceProximity
     Just "Pressure"                  -> return SourcePressure
     Just "RelativeHumidity"          -> return SourceRelativeHumidity
     Just "LinearAcceleration"        -> return SourceLinearAcceleration
     Just "Motion"                    -> return SourceMotion
     Just "StepDetector"              -> return SourceStepDetector
     Just "StepCounter"               -> return SourceStepCounter
     Just "TiltDetector"              -> return SourceTiltDetector
     Just "RotationVector"            -> return SourceRotationVector
     Just "Gravity"                   -> return SourceGravity
     Just "GeomagneticRotationVector" -> return SourceGeomagneticRotationVector
     Just "Camera"                    -> return SourceCamera
     Just "Gps"                       -> return SourceGps
     Just s                           -> die $ "Unrecognized source: " <> s
     Nothing                          -> die "No device source provided."

kvIntegralReq :: Integral a => Ident -> KVs -> Tr a
kvIntegralReq i m =
  do let v = lookup i m
     case join (valueNum <$> v) of
      Just int -> return (fromIntegral int)
      Nothing  -> do warn $ "Required field is missing (" <> textOfIdent i <>") using 0."
                     return 0

--------------------------------------------------------------------------------
--  Utils
strict :: Text.Text -> TextStrict.Text
strict = Text.toStrict
