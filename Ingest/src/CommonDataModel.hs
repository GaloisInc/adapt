{-# LANGUAGE TupleSections #-}
{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}
module CommonDataModel where

import qualified Data.ByteString as BS
import Data.Maybe (fromMaybe)
import Data.Text (Text)
import Data.Time
import Data.Time.Clock.POSIX (posixSecondsToUTCTime)
import Data.Int
import MonadLib
import qualified Data.Map as Map

import CommonDataModel.Types
import qualified Schema as S

-- toSchema is the top-level operation for translating TC CDM data into
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
   DatumSim simpleEdge        -> translateSimpleEdge    simpleEdge

translatePTN :: ProvenanceTagNode -> Translate ()
translatePTN           (PTN {..}) =
  warnNoTranslation "No Adapt Schema for Provenance Tag Nodes."

translateSubject :: Subject -> Translate ()
translateSubject       (Subject {..}) =
  let s = S.Subject
                  { S.subjectSource = translateSource subjSource
                  , S.subjectUID    = translateUUID subjUUID
                  , S.subjectType   = translateSubjectType subjType
                  , S.subjectStartTime = translateTime subjStartTimestampMicros
                  , S.subjectEndTime   = Nothing
                  , S.subjectPID    = Just subjPID
                  , S.subjectPPID   = Just subjPPID
                  , S.subjectUnitID = subjUnitId
                  , S.subjectCommandLine = subjCmdLine
                  , S.subjectImportLibs = subjImportedLibraries
                  , S.subjectExportLibs = subjExportedLibraries
                  , S.subjectProcessInfo = subjPInfo
                  , S.subjectOtherProperties = fromMaybe Map.empty subjProperties
                  -- XXX the below are probably not supposed to be fields
                  -- of subject, yes?
                  , S.subjectLocation = Nothing
                  , S.subjectSize = Nothing
                  , S.subjectPpt = Nothing
                  , S.subjectEnv = Nothing
                  , S.subjectArgs = Nothing
                  }
  in tellNode (S.NodeSubject s)

translateUUID :: Int64 -> S.UID
translateUUID = (0,0,0,) . fromIntegral

translateSubjectType :: SubjectType -> S.SubjectType
translateSubjectType s =
 case s of
  Process -> S.SubjectProcess
  Thread  -> S.SubjectThread
  Unit    -> S.SubjectUnit

translateEventType :: EventType -> S.EventType
translateEventType e =
  case e of
    EVENT_ACCEPT                 -> S.EventAccept
    EVENT_BIND                   -> S.EventBind
    EVENT_CHANGE_PRINCIPAL       -> S.EventChangePrincipal
    EVENT_CHECK_FILE_ATTRIBUTES  -> S.EventCheckFileAttributes
    EVENT_CLOSE                  -> S.EventClose
    EVENT_CONNECT                -> S.EventConnect
    EVENT_CREATE_OBJECT          -> S.EventCreateObject
    EVENT_CREATE_THREAD          -> S.EventCreateThread
    EVENT_EXECUTE                -> S.EventExecute
    EVENT_FORK                   -> S.EventFork
    EVENT_LINK                   -> S.EventLink
    EVENT_UNLINK                 -> S.EventUnlink
    EVENT_MMAP                   -> S.EventMmap
    EVENT_MODIFY_FILE_ATTRIBUTES -> S.EventModifyFileAttributes
    EVENT_MPROTECT               -> S.EventMprotect
    EVENT_OPEN                   -> S.EventOpen
    EVENT_READ                   -> S.EventRead
    EVENT_WRITE                  -> S.EventWrite
    EVENT_SIGNAL                 -> S.EventSignal
    EVENT_TRUNCATE               -> S.EventTruncate
    EVENT_WAIT                   -> S.EventWait
    EVENT_OS_UNKNOWN             -> S.EventOSUnknown
    EVENT_KERNEL_UNKNOWN         -> S.EventKernelUnknown
    EVENT_APP_UNKNOWN            -> S.EventAppUnknown
    EVENT_UI_UNKNOWN             -> S.EventUIUnknown
    EVENT_UNKNOWN                -> S.EventUnknown
    EVENT_BLIND                  -> S.EventBlind

-- Notice we drop the tag information when extracting parameter values.
translateParameters :: [Value] -> S.Args
translateParameters = map (fromMaybe BS.empty . valBytes)

translateEvent :: Event -> Translate ()
translateEvent         (Event {..}) =
  let s = S.Subject { S.subjectSource = translateSource evtSource
                    , S.subjectUID    = translateUUID evtUUID
                    , S.subjectType   = S.SubjectEvent (translateEventType evtType)
                                                       (Just evtSequence)
                    , S.subjectStartTime = translateTime evtTimestampMicros
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

translateTrust :: ProvenanceTagNode -> Translate (Maybe S.IntegrityTag)
translateTrust t =
  case ptnValue t of
    PTVIntegrityTag i -> case i of
                          INTEGRITY_UNTRUSTED -> return $ Just S.IntegrityUntrusted
                          INTEGRITY_BENIGN    -> return $ Just S.IntegrityBenign
                          INTEGRITY_INVULNERABLE -> return $ Just S.IntegrityInvulnerable
    _ -> return Nothing

translateSensitivity :: ProvenanceTagNode -> Translate (Maybe S.ConfidentialityTag)
translateSensitivity t =
  case ptnValue t of
    PTVConfidentialityTag i -> case i of
                          CONFIDENTIALITY_SECRET    -> return $ Just S.ConfidentialitySecret
                          CONFIDENTIALITY_SENSITIVE -> return $ Just S.ConfidentialitySensitive
                          CONFIDENTIALITY_PRIVATE   -> return $ Just S.ConfidentialityPrivate
                          CONFIDENTIALITY_PUBLIC    -> return $ Just S.ConfidentialityPublic
    _ -> return Nothing

translateNetFlowObject ::NetFlowObject -> Translate () 
translateNetFlowObject (NetFlowObject {..}) = do
  let AbstractObject {..} = nfBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let e = S.NetFlow
                { S.entitySource       = translateSource aoSource
                , S.entityUID          = translateUUID nfUUID
                , S.entityInfo         = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                              , S.infoPermissions     = aoPermission
                                              , S.infoTrustworthiness = mt
                                              , S.infoSensitivity     = ms
                                              , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                              }
                -- Partial fields
                , S.entitySrcAddress   = nfSrcAddress
                , S.entityDstAddress   = nfDstAddress
                , S.entitySrcPort      = nfSrcPort
                , S.entityDstPort      = nfDstPort
                }
  tellNode (S.NodeEntity e)

translateFileObject :: FileObject -> Translate ()
translateFileObject    (FileObject {..}) = do
  let AbstractObject {..} = foBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let e = S.File
             { S.entitySource       = translateSource aoSource
             , S.entityUID          = translateUUID foUUID
             , S.entityInfo         = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                           , S.infoPermissions     = aoPermission
                                           , S.infoTrustworthiness = mt
                                           , S.infoSensitivity     = ms
                                           , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                           }
             -- Partial fields
             , S.entityURL          = foURL
             , S.entityFileVersion  = fromIntegral foVersion
             , S.entityFileSize     = foSize
             }
  tellNode (S.NodeEntity e)

translateSrcSinkType :: SrcSinkType -> S.SourceType
translateSrcSinkType = toEnum . fromEnum

translateSrcSinkObject ::SrcSinkObject -> Translate () 
translateSrcSinkObject (SrcSinkObject {..}) = do
  let AbstractObject {..} = ssBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let r = S.Resource
               { S.resourceSource = translateSource aoSource
               , S.resourceType   = translateSrcSinkType ssType
               , S.resourceUID    = translateUUID ssUUID
               , S.resourceInfo   = S.Info { S.infoTime = translateTime <$> aoLastTimestampMicros
                                         , S.infoPermissions     = aoPermission
                                         , S.infoTrustworthiness = mt
                                         , S.infoSensitivity     = ms
                                         , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                         }
               }
  tellNode (S.NodeResource r)

translateMemoryObject :: MemoryObject -> Translate ()
translateMemoryObject  (MemoryObject {..}) = do
  let AbstractObject {..} = moBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let m = S.Memory
               { S.entitySource       = translateSource aoSource
               , S.entityUID          = translateUUID moUUID
               , S.entityInfo         = S.Info
                                        { S.infoTime = translateTime <$> aoLastTimestampMicros
                                        , S.infoPermissions     = aoPermission
                                        , S.infoTrustworthiness = mt
                                        , S.infoSensitivity     = ms
                                        , S.infoOtherProperties = fromMaybe Map.empty aoProperties
                                        }
               -- Partial fields
               , S.entityPageNumber   = moPageNumber
               , S.entityAddress      = moMemoryAddress
               }
  tellNode (S.NodeEntity m)

translateTime :: Int64 -> UTCTime
translateTime = posixSecondsToUTCTime . fromIntegral

translatePrincipalType :: PrincipalType -> S.PrincipalType
translatePrincipalType p =
  case p of 
    PRINCIPAL_LOCAL  -> S.PrincipalLocal
    PRINCIPAL_REMOTE -> S.PrincipalRemote

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

translateSource :: InstrumentationSource -> S.InstrumentationSource
translateSource = toEnum . fromEnum

translateSimpleEdge :: SimpleEdge -> Translate ()
translateSimpleEdge (SimpleEdge {..}) =
      let e = S.Edge { S.edgeSource       = translateUUID fromUUID
                     , S.edgeDestination  = translateUUID toUUID
                     , S.edgeRelationship = translateRelationship edgeType
                     }
      in warn (WarnOther "Ignoring edge time and properties") >> tellEdge e

translateRelationship :: EdgeType -> S.Relationship
translateRelationship e =
  case e of
    EDGE_EVENT_AFFECTS_MEMORY        -> S.WasGeneratedBy -- XXX or WasInvalidatedBy depending...
    EDGE_EVENT_AFFECTS_FILE          -> S.WasGeneratedBy
    EDGE_EVENT_AFFECTS_NETFLOW       -> S.WasGeneratedBy
    EDGE_EVENT_AFFECTS_SUBJECT       -> S.WasGeneratedBy
    EDGE_EVENT_AFFECTS_SRCSINK       -> S.WasGeneratedBy
    EDGE_EVENT_HASPARENT_EVENT       -> error "EDGE_EVENT_HASPARENT_EVENT"
    EDGE_EVENT_ISGENERATEDBY_SUBJECT -> error "EDGE_EVENT_ISGENERATEDBY_SUBJECT"
    EDGE_SUBJECT_AFFECTS_EVENT       -> error "EDGE_SUBJECT_AFFECTS_EVENT"
    EDGE_SUBJECT_HASPARENT_SUBJECT   -> error "EDGE_SUBJECT_HASPARENT_SUBJECT"
    EDGE_SUBJECT_HASLOCALPRINCIPAL   -> error "EDGE_SUBJECT_HASLOCALPRINCIPAL"
    EDGE_SUBJECT_RUNSON              -> error "EDGE_SUBJECT_RUNSON"
    EDGE_FILE_AFFECTS_EVENT          -> error "EDGE_FILE_AFFECTS_EVENT"
    EDGE_NETFLOW_AFFECTS_EVENT       -> error "EDGE_NETFLOW_AFFECTS_EVENT"
    EDGE_MEMORY_AFFECTS_EVENT        -> error "EDGE_MEMORY_AFFECTS_EVENT"
    EDGE_SRCSINK_AFFECTS_EVENT       -> error "EDGE_SRCSINK_AFFECTS_EVENT"
    EDGE_OBJECT_PREV_VERSION         -> error "EDGE_OBJECT_PREV_VERSION"
