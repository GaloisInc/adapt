{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE OverloadedStrings #-}
module CommonDataModel.FromProv
  ( readFileCDM
  , translateTextCDM
  , translate
  , module CDM
  , T.Error(..), Warning(..)
  ) where

import qualified Data.Map as Map
import           Data.Map (Map)
import           Data.DList (DList, toList, singleton)
import           Data.Monoid
import qualified Data.Text.Lazy.IO as Text
import qualified Data.Text.Lazy as Text
import qualified Data.Text      as TextStrict
import           Data.Text.Lazy (Text)
import           Text.Read (readMaybe)
import           MonadLib as ML
import qualified Control.Exception as X

import           CommonDataModel as CDM
import qualified Types as T
import qualified ParserCore as T
import           Parser (parseProvN)
import           Types (TranslateError(..),Range(..))
import           ParserCore
import           Namespaces

readFileCDM ::  FilePath -> IO ([CDM.Node], [CDM.Edge], [Warning])
readFileCDM fp =
  (either X.throw id . translateTextCDM) <$> Text.readFile fp

translateTextCDM ::  Text -> Either T.Error ([CDM.Node], [CDM.Edge], [Warning])
translateTextCDM t = runId $ runExceptionT $ do
  p             <- eX T.PE  (parseProvN t)
  ((ns,es),ws)  <- eX T.TRE (translate p)
  return  (ns,es,ws)
 where
  eX :: ExceptionM m i => (e -> i) -> Either e a -> m a
  eX f = either (raise . f) return


translate :: Prov -> Either TranslateError (([CDM.Node], [CDM.Edge]), [Warning])
translate (Prov _prefix es) = runTr $
  do (as,bs) <- unzip <$> mapM translateProvExpr es
     return (concat as, concat bs)

translateProvExpr :: T.Expr -> Tr ([CDM.Node], [CDM.Edge])
translateProvExpr e@(RawEntity {..})
  | Just f <- Map.lookup exprOper operHandler = f e
  | otherwise = do warn $ "No CDM interpretation for operation: " <> textOfIdent exprOper
                   noResults
  where
  operHandler :: Map Ident (T.Expr -> Tr ([CDM.Node], [CDM.Edge]))
  operHandler =
    Map.fromList [ (provEntity            , translateProvEntity)
                 , (provAgent             , translateProvAgent)
                 , (provActivity          , translateProvActivity)
                 , (provWasAssociatedWith , translateProvWasAssociatedWith)
                 , (provUsed              , translateProvUsed)
                 , (provWasStartedBy      , translateProvWasStartedBy)
                 , (provWasGeneratedBy    , translateProvWasGeneratedBy)
                 , (provWasEndedBy        , translateProvWasEndedBy)
                 , (provWasInformedBy     , translateProvWasInformedBy)
                 , (provWasAttributedTo   , translateProvWasAttributedTo)
                 , (provWasDerivedFrom    , translateProvWasDerivedFrom)
                 , (provActedOnBehalfOf   , translateProvActedOnBehalfOf)
                 , (provWasInvalidatedBy  , translateProvWasInvalidatedBy)
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
    node (NodeEntity $ NetFlow src uid info (strict srcIP) (strict dstIP) srcPort dstPort)
  translateProvFile    = do
    uid <- uidOfMay exprIdent
    url <- strict <$> kvStringReq adaptPath exprAttrs -- TODO old prov uses 'filePath'
    let info = noInfo
        ver  = -1
        sz   = Nothing
    node (NodeEntity $ File src uid info url ver sz)
  translateProvDevice  = do
    uid  <- uidOfMay exprIdent
    node (NodeResource $ Resource src uid noInfo)

translateProvAgent (RawEntity {..}) =
  -- warn "We just throw Prov Agents away when generating CDM."
  noResults

translateProvActivity (RawEntity {..}) =
  do let subjectSource = SourceLinuxAuditTrace
     subjectUID <- uidOfMay exprIdent
     let subjectType = SubjectProcess
         err = raise $ TranslateError "Prov Activity (CDM Subject) has no start time."
     subjectStartTime   <- maybe err return =<< kvTime adaptTime exprAttrs
     subjectSequence    <- nextSequenceNumber
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
     node $ NodeSubject $ Subject {..}

translateProvWasAssociatedWith (RawEntity {..}) =
  warn "No known CDM for 'wasAssociatedWith'" >> noResults
translateProvUsed (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId),_time] ->
        do operation <- kvString adaptOperation exprAttrs
           edgeSource <- uidOf edgeSourceId
           edgeDestination <- uidOf edgeDestinationId
           edgeRelationship <-
                Used <$> case operation of
                         Just "open"          -> return $ Just UseOpen
                         Just "bind"          -> return $ Just UseBind
                         Just "connect"       -> return $ Just UseConnect
                         Just "accept"        -> return $ Just UseAccept
                         Just "read"          -> return $ Just UseRead
                         Just "mmap"          -> return $ Just UseMmap
                         Just "mprotect"      -> return $ Just UseMprotect
                         Just "close"         -> return $ Just UseClose
                         Just "link"          -> return $ Just UseLink
                         Just "modAttributes" -> return $ Just UseModAttributes
                         Just "execute"       -> return $ Just UseExecute
                         Just "asinput"       -> return $ Just UseAsInput
                         Just other           -> -- 'recvfrom' etc
                          do warn $ "Unknown use operation: " <> other
                             return Nothing
                         Nothing              -> return Nothing
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasGeneratedBy (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId),_time] ->
        do operation <- kvString adaptOperation exprAttrs
           edgeSource <- uidOf edgeSourceId
           edgeDestination <- uidOf edgeDestinationId
           edgeRelationship <-
            fmap WasGeneratedBy <$> case operation of
                                     Just "send"     -> return $ Just GenSend
                                     Just "connect"  -> return $ Just GenConnect
                                     Just "truncate" -> return $ Just GenTruncate
                                     Just "chmod"    -> return $ Just GenChmod
                                     Just "touch"    -> return $ Just GenTouch
                                     Just "create"   -> return $ Just GenCreate
                                     Just other ->
                                        do warn $ "Unknown GenOperation: " <> other
                                           return Nothing
                                     _ -> return Nothing
           maybe noResults edge (Edge edgeSource edgeDestination <$> edgeRelationship)
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasStartedBy (RawEntity {..}) =
  -- XXX modify subject's 'subjectStartTime' field
  noResults
translateProvWasEndedBy (RawEntity {..}) =
  -- XXX modify subject's 'subjectEndTime' field
  noResults
translateProvWasInformedBy (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do edgeSource       <- uidOf edgeSourceId
           edgeDestination  <- uidOf edgeDestinationId
           edgeRelationship <- pure $ WasInformedBy Nothing
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasAttributedTo (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do edgeSource       <- uidOf edgeSourceId
           edgeDestination  <- uidOf edgeDestinationId
           edgeRelationship <- pure WasAttributedTo
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'used' relation." >> noResults
translateProvWasDerivedFrom (RawEntity {..}) =
  case exprArgs of
    [Just (Left edgeSourceId),Just (Left edgeDestinationId)] ->
        do -- Prov 'wasaDerviedFrom' is inverse to 'isPartOf'
           -- Thus we reverse the src and dst.
           edgeDestination  <- uidOf edgeSourceId
           edgeSource       <- uidOf edgeDestinationId
           edgeRelationship <- pure IsPartOf
           edge $ Edge edgeSource edgeDestination edgeRelationship
    _ -> warn "Unrecognized 'used' relation." >> noResults
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
warn = put . Warn

newtype Tr a = Tr { unTr :: ReaderT Range (WriterT (DList Warning) (ExceptionT TranslateError Id)) a }
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

runTr :: Tr a -> Either TranslateError (a,[Warning])
runTr = either Left (Right . (\(a,b) -> (a,toList b))) . runId . runExceptionT . runWriterT . runReaderT NoLoc . unTr

uidOfMay :: Maybe Ident -> Tr UID
uidOfMay = undefined

uidOf :: Ident -> Tr UID
uidOf = undefined
  -- XXX get a mapping from Ident to UID, lookup uid or insert / return a fresh UID

nextSequenceNumber :: Tr Sequence
nextSequenceNumber = undefined -- XXX pull from a Word64 supply

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
