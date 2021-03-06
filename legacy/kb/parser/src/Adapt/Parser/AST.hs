{-# LANGUAGE RecordWildCards #-}

module Adapt.Parser.AST where

import Adapt.Parser.PP
import Adapt.Parser.Position

import qualified Data.Monoid    as M
import qualified Data.Text.Lazy as L


data Decl = Decl { dName :: Located L.Text
                 , dAPT  :: Located APT
                 } deriving (Show)

data APT = APT { aptInitialCompromise  :: InitialCompromise
               , aptPersistence        :: Persistence
               , aptExpansion          :: [Expansion]
               , aptExfiltration       :: Exfiltration
               } deriving (Show)


-- Initial Compromise ----------------------------------------------------------

data InitialCompromise = DirectAttack DirectAttack
                       | IndirectAttack IndirectAttack
                         deriving (Show)

data DirectAttack = ZeroDay
                  | ExploitVulnerability
                  | DALoc (Located DirectAttack)
                    deriving (Show)

data IndirectAttack = MaliciousAttachment MaliciousAttachment
                    | DriveByDownload DriveByDownload
                    | Waterholing Waterholing
                    | UsbInfection UsbInfection
                      deriving (Show)

data MaliciousAttachment = UserOpenFile
                         | MALoc (Located MaliciousAttachment)
                           deriving (Show)

data DriveByDownload = UserClickLink
                     | DDLoc (Located DriveByDownload)
                       deriving (Show)

data Waterholing = UserVisitsTaintedURL
                 | WHLoc (Located Waterholing)
                   deriving (Show)

data UsbInfection = UserInsertUsb UsbExploitStart
                  | UILoc (Located UsbInfection)
                    deriving (Show)

data UsbExploitStart = UsbAutorun
                     | UserClickRogueLinkFile
                     | UELoc (Located UsbExploitStart)
                       deriving (Show)


-- Persistence -----------------------------------------------------------------

data Persistence = Persistence MaintainAccess EvadeDefenses
                   deriving (Show)

data MaintainAccess = MaintainAccess CommandControl (Maybe ModifyTarget)
                      deriving (Show)

data ModifyTarget = ModifyBios
                  | ModifyDll
                  | ModifyRegistry
                  | MTLoc (Located ModifyTarget)
                    deriving (Show)

data CommandControl = InitialCommandControl [MaintainCommandControl]
                    | CCLoc (Located CommandControl)
                      deriving (Show)

data MaintainCommandControl = ListenCC
                            | SendCC
                            | MCLoc (Located MaintainCommandControl)
                              deriving (Show)

data EvadeDefenses = CoverTracks
                   | EDLoc (Located EvadeDefenses)
                     deriving (Show)


-- Expansion -------------------------------------------------------------------


data Expansion = HostEnumeration HostEnumeration
               | AccessCredentials AccessCredentials
               | MoveLaterally MoveLaterally
               | ELoc (Located Expansion)
                 deriving (Show)

data HostEnumeration = AccountEnum
                     | FileSystemEnum
                     | GroupPermissions
                     | ProcessEnum
                     | HELoc (Located HostEnumeration)
                       deriving (Show)

data AccessCredentials = CredentialDumping
                       | CredentialInFile
                       | NetworkSniffing
                       | KeyboardLogging
                       | ACLoc (Located AccessCredentials)
                         deriving (Show)

data MoveLaterally = NewMachineAccess Bool
                   | MLLoc (Located MoveLaterally)
                     deriving (Show)


-- Exfiltration-----------------------------------------------------------------

data Exfiltration = Exfiltration (Maybe ExfilStaging) ExfilExecution
                    deriving (Show)

data ExfilStaging = ExfilStaging ExfilFormat ExfilInfrastructure
                    deriving (Show)

data ExfilFormat = Compress Bool
                 | EFLoc (Located ExfilFormat)
                   deriving (Show)

data ExfilInfrastructure = EstablishExfilPoints
                         | EILoc (Located ExfilInfrastructure)
                           deriving (Show)

data ExfilExecution = ExfilExecution ExfilChannel ExfilSocket
                      deriving (Show)

data ExfilChannel = ExfilPhysicalMedium
                  | HttpPost
                  | Dns
                  | Https
                  | Ftp
                  | Ssh
                  | ECLoc (Located ExfilChannel)
                    deriving (Show)

data ExfilSocket = UDP
                 | TCP
                 | ESLoc (Located ExfilSocket)
                   deriving (Show)


-- Location Management ---------------------------------------------------------

instance HasRange Persistence where
  getRange (Persistence a b) = M.mappend (getRange a) (getRange b)

instance HasRange InitialCompromise where
  getRange (DirectAttack da)   = getRange da
  getRange (IndirectAttack ia) = getRange ia

instance HasRange DirectAttack where
  getRange (DALoc ld) = getRange ld
  getRange _          = M.mempty

instance HasRange IndirectAttack where
  getRange (MaliciousAttachment a) = getRange a
  getRange (DriveByDownload     a) = getRange a
  getRange (Waterholing         a) = getRange a
  getRange (UsbInfection        a) = getRange a

instance HasRange MaliciousAttachment where
  getRange (MALoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange DriveByDownload where
  getRange (DDLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange Waterholing where
  getRange (WHLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange UsbInfection where
  getRange (UILoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange UsbExploitStart where
  getRange (UELoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange HostEnumeration where
  getRange (HELoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange AccessCredentials where
  getRange (ACLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange MoveLaterally where
  getRange (MLLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange MaintainAccess where
  getRange (MaintainAccess a b) = M.mappend (getRange a) (getRange b)

instance HasRange EvadeDefenses where
  getRange (EDLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange CommandControl where
  getRange (CCLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange ModifyTarget where
  getRange (MTLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange MaintainCommandControl where
  getRange (MCLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange Exfiltration where
  getRange (Exfiltration a b) = getRange (a,b)

instance HasRange ExfilStaging where
  getRange (ExfilStaging a b) = getRange (a,b)

instance HasRange ExfilFormat where
  getRange (EFLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange ExfilInfrastructure where
  getRange (EILoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange ExfilExecution where
  getRange (ExfilExecution a b) = getRange (a,b)

instance HasRange ExfilChannel where
  getRange (ECLoc loc) = getRange loc
  getRange _           = M.mempty

instance HasRange ExfilSocket where
  getRange (ESLoc loc) = getRange loc
  getRange _           = M.mempty


-- Pretty-printing -------------------------------------------------------------

instance PP Decl where
  ppPrec _ Decl { .. } = hang (pp dName <+> char '=') 2 (pp dAPT)

  ppList ds = vcat (map pp ds)

instance PP APT where
  ppPrec _ APT { .. } =
    fsep $ commas
         $ concat [ [ pp aptInitialCompromise ]
                  , [ pp aptPersistence ]
                  , [ pp ex | ex <- aptExpansion ]
                  , [ pp aptExfiltration ]
                  ]

instance PP InitialCompromise where
  ppPrec p (DirectAttack   da)  = ppPrec p da
  ppPrec p (IndirectAttack ia)  = ppPrec p ia

instance PP DirectAttack where
  ppPrec _ ZeroDay              = text "Zero_day_activity"
  ppPrec _ ExploitVulnerability = text "Exploit_vulnerability_activity"
  ppPrec p (DALoc lda)          = ppPrec p lda

instance PP IndirectAttack where
  ppPrec p (MaliciousAttachment ma) = ppPrec p ma
  ppPrec p (DriveByDownload d)      = ppPrec p d
  ppPrec p (Waterholing w)          = ppPrec p w
  ppPrec p (UsbInfection ui)        = ppPrec p ui

instance PP MaliciousAttachment where
  ppPrec _ UserOpenFile = text "User_open_file_activity"
  ppPrec p (MALoc loc)  = ppPrec p loc

instance PP DriveByDownload where
  ppPrec _ UserClickLink = text "User_click_link_activity"
  ppPrec p (DDLoc loc)   = ppPrec p loc

instance PP Waterholing where
  ppPrec _ UserVisitsTaintedURL = text "User_visits_tainted_URL_activity"
  ppPrec p (WHLoc loc)          = ppPrec p loc

instance PP UsbInfection where
  ppPrec _ (UserInsertUsb es) = fsep $ commas [ text "User_insert_usb_activity"
                                              , pp es ]
  ppPrec p (UILoc loc)        = ppPrec p loc

instance PP UsbExploitStart where
  ppPrec _ UsbAutorun             = text "Usb_autorun_activity"
  ppPrec _ UserClickRogueLinkFile = text "User_click_rogue_link_file_activity"
  ppPrec p (UELoc loc)            = ppPrec p loc

instance PP Expansion where
  ppPrec p (HostEnumeration he)   = ppPrec p he
  ppPrec p (AccessCredentials ac) = ppPrec p ac
  ppPrec p (MoveLaterally ml)     = ppPrec p ml
  ppPrec p (ELoc le)              = ppPrec p le

instance PP HostEnumeration where
  ppPrec _ AccountEnum      = text "Account_enum_activity"
  ppPrec _ FileSystemEnum   = text "File_system_enum_activity"
  ppPrec _ GroupPermissions = text "Group_permissions_activity"
  ppPrec _ ProcessEnum      = text "Process_enum_activity"
  ppPrec p (HELoc lhe)      = ppPrec p lhe

instance PP AccessCredentials where
  ppPrec _ CredentialDumping = text "Credential_dumping_activity"
  ppPrec _ CredentialInFile  = text "Credential_in_file_activity"
  ppPrec _ NetworkSniffing   = text "Network_sniffing_activity"
  ppPrec _ KeyboardLogging   = text "Keyboard_logging_activity"
  ppPrec p (ACLoc lac)       = ppPrec p lac

instance PP MoveLaterally where
  ppPrec _ (NewMachineAccess infect)
    | infect    = sep [ text "New_machine_access_activity" <> char ','
                      , text "Infect_new_machine_activity" ]
    | otherwise = text "New_machine_access_activity"

  ppPrec p (MLLoc loc) = ppPrec p loc

instance PP Persistence where
  ppPrec _ (Persistence a b) = sep [ pp a <> char ',', pp b ]

instance PP MaintainAccess where
  ppPrec _ (MaintainAccess cc mbModify) =
    sep $ commas $ [ pp cc, text "Download_backdoor_activity" ]
                ++ [ pp mt | Just mt <- [mbModify] ]

instance PP CommandControl where
  ppPrec _ (InitialCommandControl ms) =
    sep $ commas $ text "Initiate_command_control_activity"
                 : map pp ms

  ppPrec p (CCLoc loc) = ppPrec p loc


instance PP EvadeDefenses where
  ppPrec _ CoverTracks = text "Cover_tracks_activity"
  ppPrec p (EDLoc loc) = ppPrec p loc

instance PP ModifyTarget where
  ppPrec _ ModifyBios     = text "Bios_activity"
  ppPrec _ ModifyDll      = text "Dll_activity"
  ppPrec _ ModifyRegistry = text "Registry_activity"
  ppPrec p (MTLoc loc)    = ppPrec p loc

instance PP MaintainCommandControl where
  ppPrec _ ListenCC    = text "Listen_cc_activity"
  ppPrec _ SendCC      = text "Send_cc_activity"
  ppPrec p (MCLoc loc) = ppPrec p loc

instance PP Exfiltration where
  ppPrec _ (Exfiltration mb b) =
    case mb of
      Just a  -> fsep (commas [ pp a, pp b])
      Nothing -> pp b

instance PP ExfilStaging where
  ppPrec _ (ExfilStaging a b) =
    fsep (commas [ pp a, pp b])

instance PP ExfilFormat where
  ppPrec _ (Compress encrypt)
    | encrypt   = fsep (commas [ text "Compress_activity", text "Encrypt_activity" ])
    | otherwise =                text "Compress_activity"

  ppPrec p (EFLoc loc) = ppPrec p loc

instance PP ExfilInfrastructure where
  ppPrec _ EstablishExfilPoints = text "Establish_exfil_points_activity"
  ppPrec p (EILoc loc)          = ppPrec p loc

instance PP ExfilExecution where
  ppPrec _ (ExfilExecution c s) = fsep (commas [ pp c, pp s ])

instance PP ExfilChannel where
  ppPrec _ ExfilPhysicalMedium = text "Exfil_physical_medium_activity"
  ppPrec _ HttpPost            = text "Http_post_activity"
  ppPrec _ Dns                 = text "Dns_activity"
  ppPrec _ Https               = text "Https_activity"
  ppPrec _ Ftp                 = text "Ftp_activity"
  ppPrec _ Ssh                 = text "Ssh_activity"
  ppPrec p (ECLoc loc)         = ppPrec p loc

instance PP ExfilSocket where
  ppPrec _ UDP         = text "UDP_activity"
  ppPrec _ TCP         = text "Http_post_activity"
  ppPrec p (ESLoc loc) = ppPrec p loc
