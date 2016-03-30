{-# LANGUAGE DeriveGeneric #-}
module CommonDataModel.Types where

import Data.ByteString (ByteString)
import Data.Int
import Data.Text (Text)
import Data.Word
import Data.Map
import GHC.Generics

type Short = Word16

data SubjectType = Process | Thread | Unit
  deriving (Eq, Ord, Show, Enum)

data SrcSinkType
        = SOURCE_ACCELEROMETER
        | SOURCE_TEMPERATURE
        | SOURCE_GYROSCOPE
        | SOURCE_MAGNETIC_FIELD
        | SOURCE_HEAR_RATE
        | SOURCE_LIGHT
        | SOURCE_PROXIMITY
        | SOURCE_PRESSURE
        | SOURCE_RELATIVE_HUMIDITY
        -- composite sensors, sources
        | SOURCE_LINEAR_ACCELERATION
        | SOURCE_MOTION
        | SOURCE_STEP_DETECTOR
        | SOURCE_STEP_COUNTER
        | SOURCE_TILT_DETECTOR
        | SOURCE_ROTATION_VECTOR
        | SOURCE_GRAVITY
        | SOURCE_GEOMAGNETIC_ROTATION_VECTOR
        -- camera and GPS sources, temporary
        | SOURCE_CAMERA
        | SOURCE_GPS
        | SOURCE_AUDIO
        -- Environment variables
        | SOURCE_SYSTEM_PROPERTY
        -- IPC should only be used for internal IPC instead of network flows
        -- ClearScope might be using this in the interim for flows
        -- Can be a source or a sink
        | SOURCE_SINK_IPC
        | SOURCE_UNKNOWN -- ideally, this should never be used
  deriving (Eq, Ord, Enum, Show)

data InstrumentationSource
        = SOURCE_LINUX_AUDIT_TRACE
        | SOURCE_LINUX_PROC_TRACE
        | SOURCE_FREEBSD_OPENBSM_TRACE
        | SOURCE_ANDROID_JAVA_CLEARSCOPE
        | SOURCE_ANDROID_NATIVE_CLEARSCOPE
        | SOURCE_FREEBSD_DTRACE_CADETS
        | SOURCE_FREEBSD_TESLA_CADETS
        | SOURCE_FREEBSD_LOOM_CADETS
        | SOURCE_FREEBSD_MACIF_CADETS
        | SOURCE_WINDOWS_DIFT_FAROS
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data PrincipalType = PRINCIPAL_LOCAL | PRINCIPAL_REMOTE
  deriving (Eq, Ord, Enum, Show)

data EventType
        = EVENT_ACCEPT
        | EVENT_BIND
        | EVENT_CHANGE_PRINCIPAL
        | EVENT_CHECK_FILE_ATTRIBUTES
        | EVENT_CLOSE
        | EVENT_CONNECT
        | EVENT_CREATE_OBJECT
        | EVENT_CREATE_THREAD
        | EVENT_EXECUTE
        | EVENT_FORK
        | EVENT_LINK
        | EVENT_UNLINK
        | EVENT_MMAP
        | EVENT_MODIFY_FILE_ATTRIBUTES
        | EVENT_MPROTECT
        | EVENT_OPEN
        | EVENT_READ
        | EVENT_WRITE
        | EVENT_SIGNAL
        | EVENT_TRUNCATE
        | EVENT_WAIT
        | EVENT_OS_UNKNOWN
        | EVENT_KERNEL_UNKNOWN
        | EVENT_APP_UNKNOWN
        | EVENT_UI_UNKNOWN
        | EVENT_UNKNOWN
        | EVENT_BLIND
  deriving (Eq, Ord, Enum, Show)

data EdgeType
        = EDGE_EVENT_AFFECTS_MEMORY
        | EDGE_EVENT_AFFECTS_FILE
        | EDGE_EVENT_AFFECTS_NETFLOW
        | EDGE_EVENT_AFFECTS_SUBJECT
        | EDGE_EVENT_AFFECTS_SRCSINK
        | EDGE_EVENT_HASPARENT_EVENT
        | EDGE_EVENT_ISGENERATEDBY_SUBJECT
        | EDGE_SUBJECT_AFFECTS_EVENT
        | EDGE_SUBJECT_HASPARENT_SUBJECT
        | EDGE_SUBJECT_HASLOCALPRINCIPAL
        | EDGE_SUBJECT_RUNSON
        | EDGE_FILE_AFFECTS_EVENT
        | EDGE_NETFLOW_AFFECTS_EVENT
        | EDGE_MEMORY_AFFECTS_EVENT
        | EDGE_SRCSINK_AFFECTS_EVENT
        | EDGE_OBJECT_PREV_VERSION
  deriving (Eq, Ord, Enum, Show)

data LocalAuthType
        = LOCALAUTH_NONE
        | LOCALAUTH_PASSWORD
        | LOCALAUTH_PUBLIC_KEY
        | LOCALAUTH_ONE_TIME_PASSWORD
  deriving (Eq, Ord, Enum, Show)

data TagOpCode
        = TAG_OP_SEQUENCE
        | TAG_OP_UNION
        | TAG_OP_ENCODE
        | TAG_OP_STRONG
        | TAG_OP_MEDIUM
        | TAG_OP_WEAK
  deriving (Eq, Ord, Enum, Show)

data IntegrityTag
        = INTEGRITY_UNTRUSTED
        | INTEGRITY_BENIGN
        | INTEGRITY_INVULNERABLE
  deriving (Eq, Ord, Enum, Show)

data ConfidentialityTag
        = CONFIDENTIALITY_SECRET
        | CONFIDENTIALITY_SENSITIVE
        | CONFIDENTIALITY_PRIVATE
        | CONFIDENTIALITY_PUBLIC
  deriving (Eq, Ord, Enum, Show)

data ProvenanceTagNode
    = PTN { ptnValue    :: PTValue
          , ptnChildren :: [ProvenanceTagNode]
          , ptnId       :: Maybe TagId
          , ptnProperties  :: Properties
          }
     deriving (Eq,Ord,Show)

type Properties = Map Text Text -- XXX map to dynamic?

type TagId = Int32

data PTValue = PTVInt Int64
             | PTVTagOpCode  TagOpCode
             | PTVIntegrityTag IntegrityTag
             | PTVConfidentialityTag ConfidentialityTag
     deriving (Eq,Ord,Show)

data Value = Value { valType  :: Text
                   , valBytes :: ByteString
                   , valTags  :: [(Int,TagId)] -- Run Length and ID pairs
                   }
     deriving (Eq,Ord,Show)

data Subject =
  Subject { subjUUID                 :: Int64
          , subjType                 :: SubjectType
          , subjPID                  :: Int32
          , subjPPID                 :: Int32
          , subjSource               :: InstrumentationSource
          , subjStartTimestampMicros :: Int64 -- Unix Epoch
          , subjUnitId               :: Maybe Int
          , subjEndTimestampMicros   :: Maybe Int64
          , subjCmdLine              :: Maybe Text
          , subjImportedLibraries    :: [Text]
          , subjExportedLibraries    :: [Text]
          , subjPInfo                :: Maybe Text
          , subjProperties           :: Properties
          }
     deriving (Eq,Ord,Show)

data Event =
  Event { evtUUID               :: Int64
        , evtTimestampMicros    :: Int64
        , evtSequence           :: Int64
        , evtType               :: EventType
        , evtThreadId           :: Int32
        , evtSource             :: InstrumentationSource
        , evtName               :: Maybe Text
        , evtParameters         :: Maybe [Value]
        , evtLocation           :: Maybe Int64
        , evtSize               :: Maybe Int64
        , evtProgramPoint       :: Maybe Text
        , evtProperties         :: Properties
        }
     deriving (Eq,Ord,Show)

data AbstractObject =
  AbstractObject { aoSource     :: InstrumentationSource
                 , aoPermission :: Maybe Short
                 , aoLastTimestampMicros :: Maybe Int64
                 , aoProvenanceTagNode  :: Maybe ProvenanceTagNode
                 , aoProperties         :: Properties
                 }
     deriving (Eq,Ord,Show)

data FileObject =
  FileObject  { foUUID       :: Int64
              , foBaseObject :: AbstractObject
              , foURL        :: Text
              , foVersion    :: Int32
              , foSize       :: Maybe Int64
              }
     deriving (Eq,Ord,Show)

data NetFlowObject =
  NetFlowObject { nfUUID        :: Int64
                , nfBaseObject  :: AbstractObject
                , nfSrcAddress  :: Text
                , nfSrcPort     :: Int32
                , nfDstAddress  :: Text
                , nfDstPort     :: Int32
                }
     deriving (Eq,Ord,Show)

data MemoryObject =
  MemoryObject { moUUID         :: Int64
               , moBaseObject   :: AbstractObject
               , moPageNumber   :: Int64
               , moMemoryAddress :: Int64
               }
     deriving (Eq,Ord,Show)

data SrcSinkObject =
  SrcSinkObject { ssUUID        :: Int64
                , ssBaseObject  :: AbstractObject
                , ssType        :: SrcSinkType
                }
     deriving (Eq,Ord,Show)

data Principal =
  Principal { pUUID     :: Int64
            , pType     :: PrincipalType
            , pUserId   :: Int32
            , pGroupIds :: [Int32]
            , pSource   :: InstrumentationSource
            , pProperties :: Properties
            }
     deriving (Eq,Ord,Show)

data SimpleEdge =
  SimpleEdge { fromUUID         :: Int64
             , toUUID           :: Int64
             , edgeType         :: EdgeType
             , timestamp        :: Int64
             , edgeProperties   :: Properties
             }
     deriving (Eq,Ord,Show)

data TCCDMDatum
        = DatumPTN ProvenanceTagNode
        | DatumSub Subject
        | DatumEve Event
        | DatumNet NetFlowObject
        | DatumFil FileObject
        | DatumSrc SrcSinkObject
        | DatumMem MemoryObject
        | DatumPri Principal
        | DatumSim SimpleEdge
      deriving (Eq,Ord,Show)
