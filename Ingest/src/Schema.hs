{-# LANGUAGE DeriveGeneric #-}
module Schema
  ( module Schema
  , Word64
  ) where

import           Data.ByteString (ByteString)
import           Data.Int
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Text (Text)
import           Data.Time (UTCTime)
import           Data.Word
import           GHC.Generics

data Node
      = NodeEntity Entity
      | NodeResource Resource
      | NodeSubject Subject
      | NodeHost Host
      | NodeAgent Agent
      deriving (Eq, Ord, Show, Read, Generic)

nodeUID :: Node -> UID
nodeUID n =
  case n of
    NodeEntity e   -> entityUID e
    NodeResource r -> resourceUID r
    NodeSubject s  -> subjectUID s
    NodeHost h     -> hostUID h
    NodeAgent a    -> agentUID a

data Edge
      = Edge { edgeSource, edgeDestination :: UID
             , edgeRelationship :: Relationship
             }
      deriving (Eq, Ord, Show, Read, Generic)

data Entity
      = File { entitySource       :: InstrumentationSource
             , entityUID          :: UID
             , entityInfo         :: OptionalInfo
             -- Partial fields
             , entityURL          :: URL
             , entityFileVersion  :: FileVersion
             , entityFileSize     :: Maybe Size
             }
      | NetFlow { entitySource       :: InstrumentationSource
                , entityUID          :: UID
                , entityInfo         :: OptionalInfo
                -- Partial fields
                , entitySrcAddress   :: SrcAddress
                , entityDstAddress   :: DstAddress
                , entitySrcPort      :: SrcPort
                , entityDstPort      :: DstPort
                }
      | Memory { entitySource       :: InstrumentationSource
               , entityUID          :: UID
               , entityInfo         :: OptionalInfo
               -- Partial fields
               , entityPageNumber   :: Maybe PageNumber -- partial
               , entityAddress      :: Address          -- partial
               }
      deriving (Eq, Ord, Show, Read, Generic)

data Resource
    = Resource { resourceSource :: InstrumentationSource
               , resourceType   :: SourceType
               , resourceUID    :: UID
               , resourceInfo   :: OptionalInfo
               }
      deriving (Eq, Ord, Show, Read, Generic)

data Subject
    = Subject { subjectSource          :: InstrumentationSource
              , subjectUID             :: UID
              , subjectType            :: SubjectType
              , subjectStartTime       :: Maybe Time
              -- Optional attributes
              , subjectPID             :: Maybe PID
              , subjectPPID            :: Maybe PPID
              , subjectUnitID          :: Maybe UnitID
              , subjectEndTime         :: Maybe Time
              , subjectCommandLine     :: Maybe CommandLine
              , subjectImportLibs      :: Maybe ImportLibs
              , subjectExportLibs      :: Maybe ExportLibs
              , subjectProcessInfo     :: Maybe PInfo
              , subjectOtherProperties :: Properties
              , subjectLocation        :: Maybe Location
              , subjectSize            :: Maybe Size
              , subjectPpt             :: Maybe Ppt
              , subjectEnv             :: Maybe Env
              , subjectArgs            :: Maybe Args
              , subjectEventType       :: Maybe EventType
              , subjectEventSequence   :: Maybe Sequence
              }
      deriving (Eq, Ord, Show, Read, Generic)

mkEvent :: InstrumentationSource -> UID -> EventType -> Maybe Sequence -> Time -> Subject
mkEvent src uid eTy eSeq start =
  Subject src uid SubjectEvent (Just start) Nothing Nothing Nothing Nothing Nothing Nothing Nothing Nothing Map.empty Nothing Nothing Nothing Nothing Nothing (Just eTy) eSeq

data Host =
      Host { hostUID    :: UID
           , hostIP     :: Maybe IPAddress
           , hostSource :: Maybe InstrumentationSource
           }
      deriving (Eq, Ord, Show, Read, Generic)

data Agent =
      Agent { agentUID          :: UID
            , agentUserID       :: UserID
            , agentGID          :: Maybe GID
            , agentType         :: Maybe PrincipalType
            , agentSource       :: Maybe InstrumentationSource
            , agentProperties   :: Properties
            }
      deriving (Eq, Ord, Show, Read, Generic)

data Relationship
        = EdgeEventAffectsMemory
        | EdgeEventAffectsFile
        | EdgeEventAffectsNetflow
        | EdgeEventAffectsSubject
        | EdgeEventAffectsSrcsink
        | EdgeEventHasparentEvent
        | EdgeEventCausesEvent
        | EdgeEventIsgeneratedbySubject
        | EdgeSubjectAffectsEvent
        | EdgeSubjectHasparentSubject
        | EdgeSubjectHaslocalprincipal
        | EdgeSubjectRunson
        | EdgeFileAffectsEvent
        | EdgeNetflowAffectsEvent
        | EdgeMemoryAffectsEvent
        | EdgeSrcsinkAffectsEvent
        | EdgeObjectPrevVersion
        | EdgeFileHasTag
        | EdgeNetflowHasTag
        | EdgeMemoryHasTag
        | EdgeSrcsinkHasTag
        | EdgeSubjectHasTag
        | EdgeEventHasTag
        | PartOfPattern
        | PartOfActivity
        | PartOfPhase
        | PartOfAPT
        | PartOfSegement
        -- Not yet part of the CDM standard but perhaps someday
        | EdgeSubjectResidesOn
        | WasDerivedFrom
        -- Still used in prov translations for now, drop someday
        | WasAttributedTo
      deriving (Eq, Ord, Show, Read, Enum, Generic)

data OptionalInfo
        = Info { infoTime            :: Maybe Time
               , infoPermissions     :: Maybe Permissions
               , infoOtherProperties :: Properties
               }
      deriving (Eq, Ord, Show, Read, Generic)

noInfo :: OptionalInfo
noInfo = Info Nothing Nothing Map.empty

data InstrumentationSource
      = SourceLinuxAuditTrace
      | SourceLinuxProcTrace
      | SourceLinuxBeepTrace
      | SourceFreeBsdOpenBsmTrace
      | SourceAndroidJavaClearscope
      | SourceAndoirdNativeClearscope
      | SourceFreeBSDDtraceCadets
      | SourceFreeBSDTeslaCadets
      | SourceFreeBSDLoomCadets
      | SourceFreeBSDMacifCadets
      | SourceWindowsDiftFaros
      | SourceLinuxTheia
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data PrincipalType
      = PrincipalLocal
      | PrincipalRemote
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data EventType
        = EventAccept
        | EventBind
        | EventChangePrincipal
        | EventCheckFileAttributes
        | EventClone
        | EventClose
        | EventConnect
        | EventCreateObject
        | EventCreateThread
        | EventExecute
        | EventFork
        | EventLink
        | EventUnlink
        | EventMmap
        | EventModifyFileAttributes
        | EventMprotect
        | EventOpen
        | EventRead
        | EventRecvFrom
        | EventRecvMsg
        | EventRename
        | EventWrite
        | EventSignal
        | EventTruncate
        | EventWait
        | EventOsUnknown
        | EventKernelUnknown
        | EventAppUnknown
        | EventUiUnknown
        | EventUnknown
        | EventBlind
        | EventUnit
        | EventUpdate
        | EventSendTo
        | EventSentMsg
        | EventShm
        | EventExit
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data SourceType
      = SourceAccelerometer
      | SourceTemperature
      | SourceGyroscope
      | SourceMagneticField
      | SourceHeartRate
      | SourceLight
      | SourceProximity
      | SourcePressure
      | SourceRelativeHumidity
      -- composite sensors, sources
      | SourceLinearAcceleration
      | SourceMotion
      | SourceStepDetector
      | SourceStepCounter
      | SourceTiltDetector
      | SourceRotationVector
      | SourceGravity
      | SourceGeomagneticRotationVector
      -- camera and gps sources, temporary
      | SourceCamera
      | SourceGps
      | SourceAudio
      -- environment variables
      | SourceSystemProperty
      | SourceEnvVariable
      -- ipc should only be used for internal ipc instead of network flows
      -- clearscope might be using this in the interim for flows
      -- can be a source or a sink
      | SourceSinkIpc
      | SourceUnknown -- ideally, this should never be used
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data IntegrityTag
      = IntegrityUntrusted
      | IntegrityBenign
      | IntegrityInvulnerable
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data ConfidentialityTag
      = ConfidentialitySecret
      | ConfidentialitySensitive
      | ConfidentialityPrivate
      | ConfidentialityPublic
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data SubjectType
      = SubjectProcess
      | SubjectThread
      | SubjectUnit
      | SubjectBlock
      | SubjectEvent
      deriving (Eq, Ord, Show, Read, Generic, Enum)

data Strength = UnknownStrength | Weak | Medium | Strong
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)


data Derivation = UnknownDerivation | Copy | Encode | Compile | Encrypt | Other
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

-- "Other primitive types used in our model"
type Properties    = Map Text Text
type UID           = (Word64, Word64, Word64, Word64)
type UserID        = Text
type URL           = Text
type FileVersion   = Int64
type Size          = Int64
type Permissions   = Word16 -- 2 bytes, decoded in little endian to match much of Avro
type Sequence      = Int64
type Time          = UTCTime -- ZuluTime
type StartedAtTime = UTCTime
type EndedAtTime   = UTCTime
type IPAddress     = Text
type SrcAddress    = Text
type SrcPort       = Int32
type DstAddress    = Text
type DstPort       = Int32
type PageNumber    = Int64
type Address       = Int64
type PID           = Int32
type PPID          = Int32
type UnitID        = Int32
type CommandLine   = Text
type ImportLibs    = [Text]
type ExportLibs    = [Text]
type Env           = Map Text Text
type PInfo         = Text
type Location      = Int64
type Ppt           = Text
type Args          = [ByteString]
type GID           = [Text]
