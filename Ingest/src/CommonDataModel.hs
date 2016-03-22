{-# LANGUAGE DeriveGeneric #-}
module CommonDataModel
  ( module CommonDataModel
  , Word64
  ) where

import Data.Word
import Data.Int
import Data.Text (Text)
import Data.ByteString (ByteString)
import Data.Map (Map)
import qualified Data.Map as Map
import Data.Time (UTCTime)

import GHC.Generics

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
               , entityPageNumber   :: PageNumber -- partial
               , entityAddress      :: Address    -- partial
               }
      deriving (Eq, Ord, Show, Read, Generic)

data Resource
    = Resource { resourceSource :: InstrumentationSource
               , resourceUID    :: UID
               , resourceInfo   :: OptionalInfo
               }
      deriving (Eq, Ord, Show, Read, Generic)

data Subject
    = Subject { subjectSource          :: InstrumentationSource
              , subjectUID             :: UID
              , subjectType            :: SubjectType
              , subjectStartTime       :: Time
              , subjectSequence        :: Sequence
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
              }
      deriving (Eq, Ord, Show, Read, Generic)

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
      = WasGeneratedBy GenOperation
      | WasInvalidatedBy DelOperation
      | Used (Maybe UseOperation)
      | IsPartOf
      | WasInformedBy (Maybe InstrumentationSource)
      | RunsOn
      | ResidesOn
      | WasAttributedTo
      deriving (Eq, Ord, Show, Read, Generic)

data EdgeType = EdgeEventHasParentEvent
              | EdgeSubjectHasParentSubject
              | EdgeEventIsGeneratedBySubject
              | EdgeEventAffectsSubject
              | EdgeSubjectAffectsEvent
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data OptionalInfo
        = Info { infoTime            :: Maybe Time
               , infoPermissions     :: Maybe Permissions
               , infoTrustworthiness :: Maybe IntegrityTag
               , infoSensitivity     :: Maybe ConfidentialityTag
               , infoOtherProperties :: Properties
               }
      deriving (Eq, Ord, Show, Read, Generic)

noInfo :: OptionalInfo
noInfo = Info Nothing Nothing Nothing Nothing Map.empty

data InstrumentationSource
      = SourceLinuxAuditTrace
      | SourceLinuxProcTrace
      | SourceFreeBsdOpenBsmTrace
      | SourceAndroidJavaClearscope
      | SourceAndoirdNativeClearscope
      | SourceLinuxAuditCadets
      | SourceWindowsDiftFaros
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data PrincipalType
      = PrincipleLocal
      | PrincipleRemote
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data EventType
      = EventAccept
      | EventBind
      | EventChangePrincipal
      | EventCheckFileAttributes
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
      | EventWrite
      | EventSignal
      | EventTruncate
      | EventWait
      | EventBlind
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data SourceType
      = SourceAccelerometer
      | SourceTemperature
      | SourceGyroscope
      | SourceMagneticField
      | SourceHearRate
      | SourceLight
      | SourceProximity
      | SourcePressure
      | SourceRelativeHumidity
      | SourceLinearAcceleration
      | SourceMotion
      | SourceStepDetector
      | SourceStepCounter
      | SourceTiltDetector
      | SourceRotationVector
      | SourceGravity
      | SourceGeomagneticRotationVector
      | SourceCamera
      | SourceGps
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
      | SubjectEvent EventType
      deriving (Eq, Ord, Show, Read, Generic)

data GenOperation
      = GenSend
      | GenConnect
      | GenTruncate
      | GenChmod
      | GenTouch
      | GenCreate
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data DelOperation = Delete | Unlink
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data UseOperation
      = UseOpen
      | UseBind
      | UseConnect
      | UseAccept
      | UseRead
      | UseMmap
      | UseMprotect
      | UseClose
      | UseLink
      | UseModAttributes
      | UseExecute
      | UseAsInput
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

data Strength = Weak | Medium | Strong
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)


data Derivation = Copy | Encode | Compile | Encrypt | Other
      deriving (Eq, Ord, Show, Read, Enum, Bounded, Generic)

-- "Other primitive types used in our model"
type Properties    = Map Text Text
type UID           = (Word64, Word64, Word64, Word64)
type UserID        = Text
type URL           = Text
type FileVersion   = Int64
type Size          = Int64
type Permissions   = Int16
type Sequence      = Word64
type Time          = UTCTime -- ZuluTime
type StartedAtTime = UTCTime
type EndedAtTime   = UTCTime
type IPAddress     = Text
type SrcAddress    = Text
type SrcPort       = Word16
type DstAddress    = Text
type DstPort       = Word16
type PageNumber    = Word64 -- was 'int' note the signed difference
type Address       = Word64 --    again
type PID           = Word64 --    again
type PPID          = Word64 --    again
type UnitID        = Int
type CommandLine   = Text
type ImportLibs    = [Text]
type ExportLibs    = [Text]
type Env           = Map Text Text
type PInfo         = Text
type Location      = Int
type Ppt           = Text
type Args          = [ByteString]
type GID           = [Int]
