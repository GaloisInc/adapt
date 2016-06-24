{-# LANGUAGE TupleSections #-}
{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}
module CommonDataModel where

import qualified Data.Avro as Avro
import qualified Data.Avro.Schema as Avro
import qualified Data.Aeson as Aeson
import qualified Data.Binary.Get as G
import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as BL
import           Data.Int
import qualified Data.Map as Map
import           Data.Maybe (fromMaybe)
import           Data.Text (Text)
import           Data.Time
import           Data.Time.Clock.POSIX (posixSecondsToUTCTime)
import           MonadLib

import           CommonDataModel.Types
import qualified Schema as S
import Paths_Ingest

getAvroSchema :: IO Avro.Schema
getAvroSchema =
  do fp <- getDataFileName "data/TCCDMDatum13.avsc"
     schM <- Aeson.eitherDecode <$> BL.readFile fp
     case schM of
      Left e    -> error e
      Right sch -> return sch

readContainer :: BL.ByteString -> IO [[TCCDMDatum]]
readContainer bs =
  do sch <- getAvroSchema
     return $ Avro.decodeContainer sch bs

-- | toSchema is the top-level operation for translating TC CDM data into
-- the Adapt schema.
toSchema :: [TCCDMDatum] -> ([S.Node], [S.Edge])
toSchema [] = ([],[])
toSchema (x:xs) =
  let (Result _ ns es) = execTr $ translate x -- XXX get warnings
      (nss,ess) = toSchema xs
  in (ns ++ nss, es ++ ess)


--------------------------------------------------------------------------------
--  Translation Monad

type Translate a = StateT State Id a

data Result = Result { warnings :: [Warning]
                     , nodes    :: [S.Node]
                     , edges    :: [S.Edge]
                     } deriving (Eq, Ord, Show)

data State = State { warningsS :: [Warning] -> [Warning]
                   , nodesS    :: [S.Node] -> [S.Node]
                   , edgesS    :: [S.Edge] -> [S.Edge]
                   }

tellNode :: S.Node -> Translate ()
tellNode n =
  do s <- get
     set s { nodesS = \ns -> nodesS s (n :ns) }

tellEdge :: S.Edge -> Translate ()
tellEdge e =
  do s <- get
     set s { edgesS = \es -> edgesS s (e :es)  }

warn :: Warning -> Translate ()
warn w =
  do s <- get
     set s { warningsS = \ws -> warningsS s (w : ws) }

execTr :: Translate a -> Result
execTr op =
  let (_,State wF nF eF) = runId (runStateT (State id id id) op)
  in Result (wF []) (nF []) (eF [])

warnNoTranslation :: Text -> Translate ()
warnNoTranslation = warn . WarnNoTranslation
data Warning = WarnNoTranslation Text
                | WarnOther Text
      deriving (Eq, Ord, Show)


--------------------------------------------------------------------------------
--  Operations

translate :: TCCDMDatum -> Translate ()
translate datum =
  case datum of
   DatumPTN provenanceTagNode -> translatePTN           provenanceTagNode
   DatumSub subj              -> translateSubject       subj
   DatumEve event             -> translateEvent         event
   DatumNet netFlowObject     -> translateNetFlowObject netFlowObject
   DatumFil fileObject        -> translateFileObject    fileObject
   DatumSrc srcSinkObject     -> translateSrcSinkObject srcSinkObject
   DatumMem memoryObject      -> translateMemoryObject  memoryObject
   DatumPri principal         -> translatePrincipal     principal
   DatumTag tagObject         -> translateTagObject     tagObject
   DatumSim simpleEdge        -> translateSimpleEdge    simpleEdge
   DatumReg rko               -> translateRegistryKeyObject rko

translatePTN :: ProvenanceTagNode -> Translate ()
translatePTN           (PTN {..}) =
  warnNoTranslation "No Adapt Schema for Provenance Tag Nodes."
{-# INLINE translatePTN #-}

translateSubject :: Subject -> Translate ()
translateSubject       (Subject {..}) =
  let s = S.Subject
                  { S.subjectSource = translateSource subjSource
                  , S.subjectUID    = translateUUID subjUUID
                  , S.subjectType   = translateSubjectType subjType
                  , S.subjectStartTime   = translateTime <$> subjStartTimestampMicros
                  , S.subjectEndTime     = Nothing
                  , S.subjectPID         = Just subjPID
                  , S.subjectPPID        = Just subjPPID
                  , S.subjectUnitID      = subjUnitId
                  , S.subjectCommandLine = subjCmdLine
                  , S.subjectImportLibs  = subjImportedLibraries
                  , S.subjectExportLibs  = subjExportedLibraries
                  , S.subjectProcessInfo = subjPInfo
                  , S.subjectOtherProperties = fromMaybe Map.empty subjProperties
                  -- XXX the below are probably not supposed to be fields
                  -- of subject, yes?
                  , S.subjectLocation = Nothing
                  , S.subjectSize = Nothing
                  , S.subjectPpt = Nothing
                  , S.subjectEnv = Nothing
                  , S.subjectArgs = Nothing
                  , S.subjectEventType = Nothing
                  , S.subjectEventSequence = Nothing
                  }
  in tellNode (S.NodeSubject s)
{-# INLINE translateSubject #-}

translateUUID :: UUID -> S.UID
translateUUID (UUID bs) =
  let [a,b,c,d] = G.runGet (replicateM 4 G.getWord32le) (BL.fromStrict bs)
  in (a,b,c,d)
{-# INLINE translateUUID #-}

translateSubjectType :: SubjectType -> S.SubjectType
translateSubjectType s =
 case s of
  Process -> S.SubjectProcess
  Thread  -> S.SubjectThread
  Unit    -> S.SubjectUnit
  BasicBlock -> S.SubjectBlock
{-# INLINE translateSubjectType #-}

translateEventType :: EventType -> S.EventType
translateEventType e = toEnum (fromEnum e)
{-# INLINE translateEventType #-}

-- Notice we drop the tag information when extracting parameter values.
translateParameters :: [Value] -> S.Args
translateParameters = map (fromMaybe BS.empty . valBytes)
{-# INLINE translateParameters #-}

translateEvent :: Event -> Translate ()
translateEvent         (Event {..}) =
  let s = S.Subject { S.subjectSource = translateSource evtSource
                    , S.subjectUID    = translateUUID evtUUID
                    , S.subjectType   = S.SubjectEvent
                    , S.subjectEventType = Just (translateEventType evtType)
                    , S.subjectEventSequence = Just evtSequence
                    , S.subjectStartTime = fmap translateTime evtTimestampMicros
                    , S.subjectEndTime   = Nothing
                    , S.subjectPID    = Nothing
                    , S.subjectPPID   = Nothing
                    , S.subjectUnitID = Nothing
                    , S.subjectCommandLine = Nothing
                    , S.subjectImportLibs = Nothing
                    , S.subjectExportLibs = Nothing
                    , S.subjectProcessInfo = Nothing
                    , S.subjectOtherProperties = fromMaybe Map.empty evtProperties
                    -- XXX the below are probably not supposed to be fields
                    -- of subject, yes?
                    , S.subjectLocation = evtLocation
                    , S.subjectSize = evtSize
                    , S.subjectPpt = evtProgramPoint
                    , S.subjectEnv = Nothing
                    , S.subjectArgs = fmap translateParameters evtParameters
                    }
  in tellNode (S.NodeSubject s)
{-# INLINE translateEvent #-}

translateTrust :: ProvenanceTagNode -> Translate (Maybe S.IntegrityTag)
translateTrust t =
  case ptnValue t of
    PTVIntegrityTag i -> case i of
                          INTEGRITY_UNTRUSTED -> return $ Just S.IntegrityUntrusted
                          INTEGRITY_BENIGN    -> return $ Just S.IntegrityBenign
                          INTEGRITY_INVULNERABLE -> return $ Just S.IntegrityInvulnerable
    _ -> return Nothing
{-# INLINE translateTrust #-}

translateSensitivity :: ProvenanceTagNode -> Translate (Maybe S.ConfidentialityTag)
translateSensitivity t =
  case ptnValue t of
    PTVConfidentialityTag i -> case i of
                          CONFIDENTIALITY_SECRET    -> return $ Just S.ConfidentialitySecret
                          CONFIDENTIALITY_SENSITIVE -> return $ Just S.ConfidentialitySensitive
                          CONFIDENTIALITY_PRIVATE   -> return $ Just S.ConfidentialityPrivate
                          CONFIDENTIALITY_PUBLIC    -> return $ Just S.ConfidentialityPublic
    _ -> return Nothing
{-# INLINE translateSensitivity #-}

translateNetFlowObject ::NetFlowObject -> Translate () 
translateNetFlowObject (NetFlowObject {..}) = do
  let AbstractObject {..} = nfBaseObject
  let e = S.NetFlow
                { S.entitySource       = translateSource aoSource
                , S.entityUID          = translateUUID nfUUID
                , S.entityInfo         = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                              , S.infoPermissions     = fmap unShort aoPermission
                                              , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                              }
                -- Partial fields
                , S.entitySrcAddress   = nfSrcAddress
                , S.entityDstAddress   = nfDstAddress
                , S.entitySrcPort      = nfSrcPort
                , S.entityDstPort      = nfDstPort
                }
  tellNode (S.NodeEntity e)
{-# INLINE translateNetFlowObject #-}

translateFileObject :: FileObject -> Translate ()
translateFileObject    (FileObject {..}) = do
  let AbstractObject {..} = foBaseObject
  let e = S.File
             { S.entitySource       = translateSource aoSource
             , S.entityUID          = translateUUID foUUID
             , S.entityInfo         = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                           , S.infoPermissions     = fmap unShort aoPermission
                                           , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                           }
             -- Partial fields
             , S.entityURL          = foURL
             , S.entityFileVersion  = fromIntegral foVersion
             , S.entityFileSize     = foSize
             }
  tellNode (S.NodeEntity e)
{-# INLINE translateFileObject #-}

translateSrcSinkType :: SrcSinkType -> S.SourceType
translateSrcSinkType = toEnum . fromEnum
{-# INLINE translateSrcSinkType #-}

translateSrcSinkObject ::SrcSinkObject -> Translate () 
translateSrcSinkObject (SrcSinkObject {..}) = do
  let AbstractObject {..} = ssBaseObject
  let r = S.Resource
               { S.resourceSource = translateSource aoSource
               , S.resourceType   = translateSrcSinkType ssType
               , S.resourceUID    = translateUUID ssUUID
               , S.resourceInfo   = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                         , S.infoPermissions     = fmap unShort aoPermission
                                         , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                         }
               }
  tellNode (S.NodeResource r)
{-# INLINE translateSrcSinkObject #-}

translateMemoryObject :: MemoryObject -> Translate ()
translateMemoryObject  (MemoryObject {..}) = do
  let AbstractObject {..} = moBaseObject
  let m = S.Memory
               { S.entitySource       = translateSource aoSource
               , S.entityUID          = translateUUID moUUID
               , S.entityInfo         = S.Info
                                        { S.infoTime = translateTime <$> aoLastTimestampMicros
                                        , S.infoPermissions     = fmap unShort aoPermission
                                        , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                        }
               -- Partial fields
               , S.entityPageNumber   = moPageNumber
               , S.entityAddress      = moMemoryAddress
               }
  tellNode (S.NodeEntity m)
{-# INLINE translateMemoryObject #-}

translateTime :: Int64 -> UTCTime
translateTime = posixSecondsToUTCTime . fromIntegral
{-# INLINE translateTime #-}

translatePrincipalType :: PrincipalType -> S.PrincipalType
translatePrincipalType p =
  case p of 
    PRINCIPAL_LOCAL  -> S.PrincipalLocal
    PRINCIPAL_REMOTE -> S.PrincipalRemote
{-# INLINE translatePrincipalType #-}

translatePrincipal :: Principal -> Translate ()
translatePrincipal     (Principal {..}) =
  let a = S.Agent
            { S.agentUID          = translateUUID pUUID
            , S.agentUserID       = pUserId
            , S.agentGID          = Just pGroupIds
            , S.agentType         = Just $ translatePrincipalType pType
            , S.agentSource       = Just $ translateSource pSource
            , S.agentProperties   = fromMaybe Map.empty pProperties
            }
  in tellNode (S.NodeAgent a)
{-# INLINE translatePrincipal #-}

translateSource :: InstrumentationSource -> S.InstrumentationSource
translateSource = toEnum . fromEnum
{-# INLINE translateSource #-}

-- XXX Figure out delta to make Adapt Schema work with tag objects.
translateTagObject :: TagEntity -> Translate ()
translateTagObject (TagEntity {..}) =
  warn (WarnOther "Ignoring tag object, a new field in CDM10")
{-# INLINE translateTagObject #-}

translateSimpleEdge :: SimpleEdge -> Translate ()
translateSimpleEdge (SimpleEdge {..}) =
      let e = S.Edge { S.edgeSource       = translateUUID fromUUID
                     , S.edgeDestination  = translateUUID toUUID
                     , S.edgeRelationship = translateRelationship edgeType
                     }
      in warn (WarnOther "Ignoring edge time and properties") >> tellEdge e
{-# INLINE translateSimpleEdge #-}

translateRegistryKeyObject :: RegistryKeyObject -> Translate ()
translateRegistryKeyObject _ = warn (WarnOther "Ignoring a registry key object.")

translateRelationship :: EdgeType -> S.Relationship
translateRelationship e = toEnum (fromEnum e)
{-# INLINE translateRelationship #-}
