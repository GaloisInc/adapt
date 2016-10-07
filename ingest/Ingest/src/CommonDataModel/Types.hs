{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE ScopedTypeVariables #-}
module CommonDataModel.Types where

import           Prelude as P
import           Data.Avro (FromAvro(..), (.:))
import qualified Data.Avro.Schema as Avro
import qualified Data.Avro.Types as Ty
import           Data.ByteString (ByteString)
import qualified Data.ByteString as B
import           Data.ByteString.Base64 as B64
import qualified Data.ByteString.Char8 as BC
import           Data.Int
import           Data.Map
import           Data.Monoid
import           Data.Text (Text)
import qualified Data.Text as T
import           Data.Word (Word16)


-- | A two byte value we keep as 16 bits in little endian.
newtype Short = Short { unShort :: Word16 }
  deriving (Eq,Ord,Show,Read)

-- | UUIDs are 128 bit (16 byte) values in CDM13 (our base)
newtype UUID = UUID ByteString
  deriving (Eq,Ord)

instance Show UUID where
  show (UUID b) = BC.unpack (B64.encode b)
instance Read UUID where
  readsPrec _ b = case B64.decode $ BC.pack b of
                    Right res -> [(UUID res,"")]
                    Left _    -> []

data SubjectType = Process | Thread | Unit | BasicBlock
  deriving (Eq, Ord, Show, Enum, Bounded)

data SrcSinkType
        = SOURCE_ACCELEROMETER
        | SOURCE_TEMPERATURE
        | SOURCE_GYROSCOPE
        | SOURCE_MAGNETIC_FIELD
        | SOURCE_HEART_RATE
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
        | SOURCE_ENV_VARIABLE
        -- IPC should only be used for internal IPC instead of network flows
        -- ClearScope might be using this in the interim for flows
        -- Can be a source or a sink
        | SOURCE_SINK_IPC
        | SOURCE_UNKNOWN -- ideally, this should never be used
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data InstrumentationSource
        = SOURCE_LINUX_AUDIT_TRACE
        | SOURCE_LINUX_PROC_TRACE
        | SOURCE_LINUX_BEEP_TRACE
        | SOURCE_FREEBSD_OPENBSM_TRACE
        | SOURCE_ANDROID_JAVA_CLEARSCOPE
        | SOURCE_ANDROID_NATIVE_CLEARSCOPE
        | SOURCE_FREEBSD_DTRACE_CADETS
        | SOURCE_FREEBSD_TESLA_CADETS
        | SOURCE_FREEBSD_LOOM_CADETS
        | SOURCE_FREEBSD_MACIF_CADETS
        | SOURCE_WINDOWS_DIFT_FAROS
        | SOURCE_LINUX_THEIA
        | SOURCE_WINDOWS_FIVEDIRECTIONS
      deriving (Eq, Ord, Show, Read, Enum, Bounded)

data PrincipalType = PRINCIPAL_LOCAL | PRINCIPAL_REMOTE
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data EventType
        = EVENT_ACCEPT
        | EVENT_BIND
        | EVENT_CHANGE_PRINCIPAL
        | EVENT_CHECK_FILE_ATTRIBUTES
        | EVENT_CLONE
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
        | EVENT_RECVFROM
        | EVENT_RECVMSG
        | EVENT_RENAME
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
        | EVENT_UNIT
        | EVENT_UPDATE
        | EVENT_SENDTO
        | EVENT_SENDMSG
        | EVENT_SHM
        | EVENT_EXIT
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data TagEntity =
  TagEntity { teUUID       :: UUID
            , tePTN        :: ProvenanceTagNode
            , teTimestamp  :: Maybe Int64
            , teProperties :: Maybe Properties
            }
         deriving (Eq,Ord,Show)

data EdgeType
        = EDGE_EVENT_AFFECTS_MEMORY
        | EDGE_EVENT_AFFECTS_FILE
        | EDGE_EVENT_AFFECTS_NETFLOW
        | EDGE_EVENT_AFFECTS_SUBJECT
        | EDGE_EVENT_AFFECTS_SRCSINK
        | EDGE_EVENT_HASPARENT_EVENT
        | EDGE_EVENT_CAUSES_EVENT
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
        | EDGE_FILE_HAS_TAG
        | EDGE_NETFLOW_HAS_TAG
        | EDGE_MEMORY_HAS_TAG
        | EDGE_SRCSINK_HAS_TAG
        | EDGE_SUBJECT_HAS_TAG
        | EDGE_EVENT_HAS_TAG
        | EDGE_EVENT_AFFECTS_REGISTRYKEY
        | EDGE_REGISTRYKEY_AFFECTS_EVENT
        | EDGE_REGISTRYKEY_HAS_TAG
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data LocalAuthType
        = LOCALAUTH_NONE
        | LOCALAUTH_PASSWORD
        | LOCALAUTH_PUBLIC_KEY
        | LOCALAUTH_ONE_TIME_PASSWORD
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data TagOpCode
        = TAG_OP_SEQUENCE
        | TAG_OP_UNION
        | TAG_OP_ENCODE
        | TAG_OP_STRONG
        | TAG_OP_MEDIUM
        | TAG_OP_WEAK
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data IntegrityTag
        = INTEGRITY_UNTRUSTED
        | INTEGRITY_BENIGN
        | INTEGRITY_INVULNERABLE
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data ConfidentialityTag
        = CONFIDENTIALITY_SECRET
        | CONFIDENTIALITY_SENSITIVE
        | CONFIDENTIALITY_PRIVATE
        | CONFIDENTIALITY_PUBLIC
  deriving (Eq, Ord, Enum, Bounded, Show, Read)

data ProvenanceTagNode
    = PTN { ptnValue       :: PTValue
          , ptnChildren    :: Maybe [ProvenanceTagNode]
          , ptnId          :: Maybe TagId
          , ptnProperties  :: Maybe Properties
          }
     deriving (Eq,Ord,Show)

type Properties = Map Text Text -- XXX map to dynamic?

type TagId = Int32

data PTValue = PTVInt Int64
             | PTVUUID UUID
             | PTVTagOpCode  TagOpCode
             | PTVIntegrityTag IntegrityTag
             | PTVConfidentialityTag ConfidentialityTag
     deriving (Eq,Ord,Show)

data Value = Value { valSize       :: Int32
                   , valType       :: ValueType
                   , valDataType   :: ValueDataType
                   , valBytes      :: Maybe ByteString
                   , valTags       :: Maybe [Int32] -- XXX Run Length and ID pairs
                   , valComponents :: Maybe [Value]
                   }
     deriving (Eq,Ord,Show)

data ValueType = TypeIn | TypeOut | TypeInOut
  deriving (Eq,Ord,Show,Enum,Bounded)

data ValueDataType
        = VALUE_DATA_TYPE_BYTE     -- 8 bit
        | VALUE_DATA_TYPE_BOOL     -- 8 bit TRUE=1 FALSE=0
        | VALUE_DATA_TYPE_CHAR     -- 16 bit unicode char
        | VALUE_DATA_TYPE_SHORT    -- 16 bit signed value
        | VALUE_DATA_TYPE_INT      -- 32 bit signed value
        | VALUE_DATA_TYPE_FLOAT    -- 32 bit floating point value
        | VALUE_DATA_TYPE_LONG     -- 64 bit signed value
        | VALUE_DATA_TYPE_DOUBLE   -- 64 bit double value
        | VALUE_DATA_TYPE_COMPLEX  -- everything else that is not a primitive data type
  deriving (Eq,Ord,Show,Read,Enum,Bounded)

data Subject =
  Subject { subjUUID                 :: UUID
          , subjType                 :: SubjectType
          , subjPID                  :: Int32
          , subjPPID                 :: Int32
          , subjSource               :: InstrumentationSource
          , subjStartTimestampMicros :: Maybe Int64 -- Unix Epoch
          , subjUnitId               :: Maybe Int32
          , subjEndTimestampMicros   :: Maybe Int64
          , subjCmdLine              :: Maybe Text
          , subjImportedLibraries    :: Maybe [Text]
          , subjExportedLibraries    :: Maybe [Text]
          , subjPInfo                :: Maybe Text
          , subjProperties           :: Maybe Properties
          }
     deriving (Eq,Ord,Show)

data Event =
  Event { evtUUID               :: UUID
        , evtSequence           :: Int64
        , evtType               :: EventType
        , evtThreadId           :: Int32
        , evtSource             :: InstrumentationSource
        , evtTimestampMicros    :: Maybe Int64
        , evtName               :: Maybe Text
        , evtParameters         :: Maybe [Value]
        , evtLocation           :: Maybe Int64
        , evtSize               :: Maybe Int64
        , evtProgramPoint       :: Maybe Text
        , evtProperties         :: Maybe Properties
        }
     deriving (Eq,Ord,Show)

data AbstractObject =
  AbstractObject { aoSource              :: InstrumentationSource
                 , aoPermission          :: Maybe Short
                 , aoLastTimestampMicros :: Maybe Int64
                 , aoProperties          :: Maybe Properties
                 }
     deriving (Eq,Ord,Show)

data FileObject =
  FileObject  { foUUID       :: UUID
              , foBaseObject :: AbstractObject
              , foURL        :: Text
              , foIsPipe     :: Bool
              , foVersion    :: Int32
              , foSize       :: Maybe Int64
              }
     deriving (Eq,Ord,Show)

data NetFlowObject =
  NetFlowObject { nfUUID        :: UUID
                , nfBaseObject  :: AbstractObject
                , nfSrcAddress  :: Text
                , nfSrcPort     :: Int32
                , nfDstAddress  :: Text
                , nfDstPort     :: Int32
                , nfIpProtocol  :: Maybe Int32
                }
     deriving (Eq,Ord,Show)

data MemoryObject =
  MemoryObject { moUUID          :: UUID
               , moBaseObject    :: AbstractObject
               , moPageNumber    :: Maybe Int64
               , moMemoryAddress :: Int64
               }
     deriving (Eq,Ord,Show)

data SrcSinkObject =
  SrcSinkObject { ssUUID        :: UUID
                , ssBaseObject  :: AbstractObject
                , ssType        :: SrcSinkType
                }
     deriving (Eq,Ord,Show)

data Principal =
  Principal { pUUID     :: UUID
            , pType     :: PrincipalType
            , pUserId   :: Text
            , pGroupIds :: [Text]
            , pSource   :: InstrumentationSource
            , pProperties :: Maybe Properties
            }
     deriving (Eq,Ord,Show)

data SimpleEdge =
  SimpleEdge { fromUUID         :: UUID
             , toUUID           :: UUID
             , edgeType         :: EdgeType
             , timestamp        :: Int64
             , edgeProperties   :: Maybe Properties
             }
     deriving (Eq,Ord,Show)

data RegistryKeyObject =
  RegistryKeyObject { regUUID       :: UUID
                     , regBaseObject :: AbstractObject
                     , regKey        :: Text
                     , regVersion    :: Int32
                     , regSize       :: Maybe Int64
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
        | DatumTag TagEntity
        | DatumSim SimpleEdge
        | DatumReg RegistryKeyObject
      deriving (Eq,Ord,Show)

instance FromAvro TCCDMDatum where
  fromAvro (Ty.Record _ rec) = do
    val <- rec .: "datum"
    case val of
      Ty.Union _ t v ->
        case Avro.typeName t of
          "ProvenanceTagNode"  -> DatumPTN <$> fromAvro v
          "Subject"            -> DatumSub <$> fromAvro v
          "Event"              -> DatumEve <$> fromAvro v
          "NetFlowObject"      -> DatumNet <$> fromAvro v
          "FileObject"         -> DatumFil <$> fromAvro v
          "SrcSinkObject"      -> DatumSrc <$> fromAvro v
          "MemoryObject"       -> DatumMem <$> fromAvro v
          "Principal"          -> DatumPri <$> fromAvro v
          "TagEntity"          -> DatumTag <$> fromAvro v
          "SimpleEdge"         -> DatumSim <$> fromAvro v
          "RegistryKeyObject" -> DatumReg <$> fromAvro v
          x                   -> fail $ "Unrecognized TCCDMDatum option in union: " <> show x
      v                   -> fail $ "TCCDMDatum 'datum' should be a union, found: " <> show v
  fromAvro v = fail $ "Invalid type for TCCDMDatum, should be a record but found: " <> show v
instance FromAvro ProvenanceTagNode where
  fromAvro (Ty.Record _ obj) =
      PTN <$> obj .: "value"
          <*> obj .: "children"
          <*> obj .: "tagId"
          <*> obj .: "properties"
  fromAvro _ = fail "Invalid value for ProvenanceTagNode"
instance FromAvro PTValue where
  fromAvro (Ty.Union _ t v) =
     case Avro.typeName t of
      "int"                -> PTVInt <$> fromAvro v
      "UUID"               -> PTVUUID <$> fromAvro v
      "TagOpCode"          -> PTVTagOpCode <$> fromAvro v
      "IntegrityTag"       -> PTVIntegrityTag <$> fromAvro v
      "ConfidentialityTag" -> PTVConfidentialityTag <$> fromAvro v
      _                    -> fail "Unrecognized tag in ProvTagNode value union"
  fromAvro _ = fail "Non-union value in PTValue"
instance FromAvro TagOpCode where
  fromAvro = fromAvroEnum "TagOpCode"
instance FromAvro Event where
  fromAvro (Ty.Record _ obj) =
    Event <$> obj .: "uuid"
          <*> obj .: "sequence"
          <*> obj .: "type"
          <*> obj .: "threadId"
          <*> obj .: "source"
          <*> obj .: "timestampMicros"
          <*> obj .: "name"
          <*> obj .: "parameters"
          <*> obj .: "location"
          <*> obj .: "size"
          <*> obj .: "programPoint"
          <*> obj .: "properties"
  fromAvro _ = fail "Invalid value for Event"
instance FromAvro EventType where
  fromAvro = fromAvroEnum "EventType"
instance FromAvro InstrumentationSource where
  fromAvro = fromAvroEnum "InstrumentationSource"
instance FromAvro Value where
  fromAvro (Ty.Record _ obj) =
    Value <$> obj .: "size"
          <*> obj .: "type"
          <*> obj .: "valueDataType"
          -- "isNull"
          -- "name"
          -- "runtimeDataType"
          <*> obj .: "valueBytes"
          <*> obj .: "tag"
          <*> obj .: "components"
  fromAvro _ = fail "Non-record for Value."

instance FromAvro ValueType where
  fromAvro = fromAvroEnum "ValueType"
instance FromAvro ValueDataType where
  fromAvro = fromAvroEnum "ValueDataType"
instance FromAvro NetFlowObject where
  fromAvro (Ty.Record _ obj)  =
    NetFlowObject <$> obj .: "uuid"
                  <*> obj .: "baseObject"
                  <*> obj .: "srcAddress"
                  <*> obj .: "srcPort"
                  <*> obj .: "destAddress"
                  <*> obj .: "destPort"
                  <*> obj .: "ipProtocol"
  fromAvro _ = fail "Non-record type for NetFlowObject."
instance FromAvro AbstractObject where
  fromAvro (Ty.Record _ obj) =
    AbstractObject <$> obj .: "source"
                   <*> obj .: "permission"
                   <*> obj .: "lastTimestampMicros"
                   <*> obj .: "properties"
  fromAvro _ = fail "Non-record for AbstractObject"
instance FromAvro Short where
  fromAvro (Ty.Fixed bs)
    | [b,a] <- B.unpack bs = pure $ Short $ fromIntegral a * 256 + fromIntegral b
  fromAvro _ = fail "Invalid value for Short - requires 16 bit fixed."
instance FromAvro FileObject where
  fromAvro (Ty.Record _ obj) =
    FileObject  <$> obj .: "uuid"
                <*> obj .: "baseObject"
                <*> obj .: "url"
                <*> obj .: "isPipe"
                <*> obj .: "version"
                <*> obj .: "size"
  fromAvro _ = fail "Non-record for FileObject"
instance FromAvro SrcSinkObject where
  fromAvro (Ty.Record _ obj) =
    SrcSinkObject <$> obj .: "uuid"
                  <*> obj .: "baseObject"
                  <*> obj .: "type"
  fromAvro _ = fail "Non-record for SrcSinkObject"
instance FromAvro SrcSinkType where
  fromAvro = fromAvroEnum "SrcSinkType"
instance FromAvro MemoryObject where
  fromAvro (Ty.Record _ obj) =
    MemoryObject <$> obj .: "uuid"
                 <*> obj .: "baseObject"
                 <*> obj .: "pageNumber"
                 <*> obj .: "memoryAddress"
  fromAvro _ = fail "Non-record for MemoryObject"
instance FromAvro Principal where
  fromAvro (Ty.Record _ obj) =
    Principal <$> obj .: "uuid"
              <*> obj .: "type"
              <*> (textOf <$> obj .: "userId")
              <*> (P.map textOf <$> obj .: "groupIds")
              <*> obj .: "source"
              <*> obj .: "properties"
  fromAvro _ = fail "Invalid value for Principal"
data TextOrInt = TOI {textOf :: Text}
instance FromAvro TextOrInt where
  fromAvro (Ty.String s) = pure $ TOI s
  fromAvro (Ty.Int i) = pure (TOI $ T.pack (show i))
  fromAvro (Ty.Long i) = pure (TOI $ T.pack (show i))
  fromAvro _ = fail "Invalid value for TextOrInt used by Principal"
instance FromAvro PrincipalType where
  fromAvro = fromAvroEnum "PrincipalType"
instance FromAvro TagEntity where
  fromAvro (Ty.Record _ obj) =
    TagEntity <$> obj .: "uuid"
              <*> obj .: "tag"
              <*> obj .: "timestampMicros"
              <*> obj .: "properties"
  fromAvro _ = fail "Invalid value for TagEntity"
instance FromAvro Subject where
  fromAvro (Ty.Record _ obj) =
    Subject <$> obj .: "uuid"
            <*> obj .: "type"
            <*> obj .: "pid"
            <*> obj .: "ppid"
            <*> obj .: "source"
            <*> obj .: "startTimestampMicros"
            <*> obj .: "unitId"
            <*> obj .: "endTimestampMicros"
            <*> obj .: "cmdLine"
            <*> obj .: "importedLibraries"
            <*> obj .: "exportedLibraries"
            <*> obj .: "pInfo"
            <*> obj .: "properties"
  fromAvro _ = fail "Invalid value for Subject"

instance FromAvro SubjectType where
  fromAvro = fromAvroEnum "SubjectType"

instance FromAvro ConfidentialityTag where
  fromAvro = fromAvroEnum "ConfidentialityTag"

instance FromAvro IntegrityTag where
  fromAvro = fromAvroEnum "IntegrityTag"

instance FromAvro SimpleEdge where
  fromAvro (Ty.Record _ fields) =
    SimpleEdge <$> fields .: "fromUuid"
               <*> fields .: "toUuid"
               <*> fields .: "type"
               <*> fields .: "timestamp"
               <*> fields .: "properties"
  fromAvro _ = fail "Invalid Avro type for SimpleEdge"

instance FromAvro UUID where
  fromAvro (Ty.Fixed bs) = pure $ UUID bs
  fromAvro _ = fail "Invalid value for UUID"

instance FromAvro EdgeType where
  fromAvro = fromAvroEnum "EdgeType"

instance FromAvro RegistryKeyObject where
  fromAvro (Ty.Record _ obj) =
    RegistryKeyObject <$> obj .: "uuid"
                      <*> obj .: "baseObject"
                      <*> obj .: "key"
                      <*> obj .: "version"
                      <*> obj .: "size"
  fromAvro _ = fail "Invalid value for RegistryKeyObject"

-- XXX seriously...
fromAvroEnum :: forall a. (Bounded a, Enum a) => String -> Ty.Value Avro.Type -> Avro.Result a
fromAvroEnum ty (Ty.Enum _ idx txt)
  | idx >= fromEnum (minBound :: a) && idx <= fromEnum (maxBound :: a) = pure $ toEnum idx
  | otherwise = fail $ "Unrecognized enum for '" <> ty <> "': " <> T.unpack txt
fromAvroEnum ty v = fail $ "Invalid value for '" <> ty <> "': " <> show v
