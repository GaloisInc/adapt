{-# LANGUAGE TupleSections #-}
module FromCDM where

import Data.ByteString (ByteString)
import Data.Text (Text)

import CommonDataModel.Types
import qualified Schema as S

-- toSchema is the top-level operation for translating TC CDM data into
-- the Adapt schema.
toSchema :: [TCCDMDatum] -> ([Node], [Edge])
toSchema [] = ([],[])
toSchema (x:xs) =
  let (ns,es) = runTr $ translate x
      (nss,ess) = toSchema xs
  in (ns ++ nss, es ++ ess)


--------------------------------------------------------------------------------
--  Translation Monad

type Translate a = StateT State Identity a

data Result = Result { warnings :: [Warning]
                     , nodes    :: [Node]
                     , edges    :: [Edge]
                     } deriving (Eq, Ord, Show)

data State = State { warningsS :: [Warning] -> [Warning]
                   , nodesS    :: [Node] -> [Node]
                   , edgesS    :: [Edge] -> [Edge]
                   }

tellNode :: Node -> Translate ()
tellNode n =
  do s <- get
     put s { nodesS = \ns -> nodesS s (n :ns) }

tellEdge :: Edge -> Translate ()
tellEdge e =
  do s <- get
     put s { edgesS = \es -> edgesS s (e :es)  }

warn :: Warning -> Translate ()
warn w =
  do s <- get
     put s { warningsS = \ws -> warningsS s (w : ws) }

execTr :: Translate a -> Result
execTr op =
  let (State wF nF eF,_) = runIdentity (runStateT (State id id id) op)
  in Result (wF []) (nF []) (eF [])

warnNoTranslation :: Text -> Translate ()
warnNoTranslation = warn . WarnNoTranslation
data Warning = WarnNoTranslation Text
                | WarnOther Text
      deriving (Eq, Ord, Show)


--------------------------------------------------------------------------------
--  Operations

translate :: TCCDMDatum -> Result
translate datum =
  case datum of
   PTN provenanceTagNode -> translatePTN           provenanceTagNode
   Sub subj              -> translateSubject       subj
   Eve event             -> translateEvent         event
   Net netFlowObject     -> translateNetFlowObject netFlowObject
   Fil fileObject        -> translateFileObject    fileObject
   Src srcSinkObject     -> translateSrcSinkObject srcSinkObject
   Mem memoryObject      -> translateMemoryObject  memoryObject
   Pri principal         -> translatePrincipal     principal
   Sim simpleEdge        -> translateSimpleEdge    simpleEdge

translatePTN :: ProvenanceTagNode -> Translate ()
translatePTN           (ProvenanceTagNode {..}) = 
  warnNoTranslation "No Adapt Schema for Provenance Tag Nodes."

translateSubject :: Subject -> Translate ()
translateSubject       (Subject {..}) =
  let s = Subject { subjectSource = subjSource
                  , subjectUID    = translateUUID subjUUID
                  , subjectType   = translateSubjectType subjType
                  , subjectStartTime = translateTime subjStartTimestampMicros
                  , subjectPID    = Just subjPID
                  , subjectPPID   = Just subjPPID
                  , subjectUnitID = Just subjUnitId
                  , subjectCommandLine = subjCmdLine
                  , subjectImportLibs = Just subjImportedLibraries
                  , subjectExportLibs = Just subjExportedLibraries
                  , subjectProcessInfo = subjPInfo
                  , subjectOtherProperties = subjProperties
                  -- XXX the below are probably not supposed to be fields
                  -- of subject, yes?
                  , subjectLocation = Nothing
                  , subjectSize = Nothing
                  , subjectPpt = Nothing
                  , subjectEnv = Nothing
                  , subjectArgs = Nothing
                  }
  in tellNode (NodeSubject s)

translateUUID :: Word64 -> S.UUID
translateUUID = (0,0,0,)

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
    EVENT_CLOSE                  -> S.EventCloase
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
translateParameters = map valBytes

translateEvent :: Event -> Translate ()
translateEvent         (Event {..}) =
  let s = Subject { subjectSource = evtSource
                  , subjectUID    = translateUUID evtUUID
                  , subjectType   = S.SubjectEvent (translateEventType evtType)
                                                   (Just evtSequence)
                  , subjectStartTime = translateTime evtTimestampMicros
                  , subjectPID    = Nothing
                  , subjectPPID   = Nothing
                  , subjectUnitID = Nothing
                  , subjectCommandLine = Nothing
                  , subjectImportLibs = Nothing
                  , subjectExportLibs = Nothing
                  , subjectProcessInfo = Nothing
                  , subjectOtherProperties = evtProperties
                  -- XXX the below are probably not supposed to be fields
                  -- of subject, yes?
                  , subjectLocation = evtLocation
                  , subjectSize = evtSize
                  , subjectPpt = evtProgramPoint
                  , subjectEnv = Nothing
                  , subjectArgs = fmap translateParameters evtParameters
                  }
  in tellNode (NodeSubject s)

translateTrust :: ProvenanceTagNode -> Translate IntegrityTag
translateTrust t =
  case ptnValue t of
    PTVIntegrityTag i -> case i of
                          INTEGRITY_UNTRUSTED -> return $ Just S.IntegrityUntrusted
                          INTEGRITY_BENIGN    -> return $ Just S.IntegrityBenign
                          INTEGRITY_INVULNERABLE -> return $ Just S.IntegrityInvulnerable
    _ -> return Nothing

translateSensitivity :: ProvenanceTagNode -> Translate ConfidentialityTag
translateSensitivity =
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
  let e = NetFlow
                { entitySource       = aoSource
                , entityUID          = translateUUID nfUUID
                , entityInfo         = Info { infoTime = translateTime <$> aoLastTimestampMicros
                                            , infoPermissions     = aoPermission
                                            , infoTrustworthiness = mt
                                            , infoSensitivity     = ms
                                            , infoOtherProperties = aoProperties
                                            }
                -- Partial fields
                , entitySrcAddress   = nfSrcAddress
                , entityDstAddress   = nfDstAddress
                , entitySrcPort      = nfSrcPort
                , entityDstPort      = nfDstPort
                }
  tellNode (NodeEntity e)

translateFileObject :: FileObject -> Translate ()
translateFileObject    (FileObject {..}) = do
  let AbstractObject {..} = foBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let e = File
             { entitySource       = aoSource
             , entityUID          = translateUUID foUUID
             , entityInfo         = Info { infoTime = translateTime <$> aoLastTimestampMicros
                                         , infoPermissions     = aoPermission
                                         , infoTrustworthiness = mt
                                         , infoSensitivity     = ms
                                         , infoOtherProperties = aoProperties
                                         }
             -- Partial fields
             , entityURL          = foURL
             , entityFileVersion  = fromIntegral foVersion
             , entityFileSize     = foSize
             }
  tellNode (NodeEntity e)

translateSrcSinkType :: SourceSinkType -> SourceType
translateSrcSinkType = undefined

translateSrcSinkObject ::SrcSinkObject -> Translate () 
translateSrcSinkObject (SrcSinkObject {..}) = do
  let AbstractObject {..} = ssBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let r = Resource
               { resourceSource = aoSource
               , resourceType   = translateSrcSinkType ssType
               , resourceUID    = translateUUID ssUUID
               , resourceInfo   = Info { infoTime = translateTime <$> aoLastTimestampMicros
                                        , infoPermissions     = aoPermission
                                        , infoTrustworthiness = mt
                                        , infoSensitivity     = ms
                                        , infoOtherProperties = aoProperties
                                        }
               }
  tellNode (NodeResource r)

translateMemoryObject :: MemoryObject -> Translate ()
translateMemoryObject  (MemoryObject {..}) = do
  let AbstractObject {..} = moBaseObject
  mt <- maybe (return Nothing) translateTrust aoProvenanceTagNode
  ms <- maybe (return Nothing) translateSensitivity aoProvenanceTagNode
  let m = Memory
               { entitySource       = aoSource
               , entityUID          = translateUUID moUUID
               , entityInfo         = Info
                                        { infoTime = translateTime <$> aoLastTimestampMicros
                                        , infoPermissions     = aoPermission
                                        , infoTrustworthiness = mt
                                        , infoSensitivity     = ms
                                        , infoOtherProperties = aoProperties
                                        }
               -- Partial fields
               , entityPageNumber   = moPageNumber
               , entityAddress      = moMemoryAddress
               }
  tellNode (NodeEntity m)

translatePrincipleType :: PrincipleType -> S.PrincipleType
translatePrincipleType p =
  case p of 
    PRINCIPAL_LOCAL  -> PrincipalLocal
    PRINCIPAL_REMOTE -> PrincipalRemote

translatePrincipal :: Principal -> Translate ()
translatePrincipal     (Principal {..}) =
  let a = Agent
            { agentUID          = translateUUID pUUID
            , agentUserID       = pUserId
            , agentGID          = pGroupIds
            , agentType         = Just $ translatePrincipleType pType
            , agentSource       = translateSource pSource
            , agentProperties   = pProperties
            }
  in tellNode (NodeAgent m)

translateSimpleEdge :: SimpleEdge -> Translate ()
translateSimpleEdge    (SimpleEdge {..}) =
      let e = Edge { edgeSource       = translateUUID fromUUID
                   , edgeDestination  = translateUUID toUUID
                   , edgeRelationship = translateRelationship edgeType
                   }
      in warn (WarnOther "Ignoring edge time and properties") >> tellEdge e

translateRelationship :: EdgeType -> S.Relationship
translationRelationship e =
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
