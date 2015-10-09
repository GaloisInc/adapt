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
               -- , aptExfiltration       :: Exfiltration
               } deriving (Show)


-- Initial Compromise ----------------------------------------------------------

data InitialCompromise = DirectAttack DirectAttack
                       | IndirectAttack IndirectAttack
                       | ICLoc (Located InitialCompromise)
                         deriving (Show)

data DirectAttack = ZeroDay
                  | ExploitVulnerability
                  | DALoc (Located DirectAttack)
                    deriving (Show)

data IndirectAttack = IA
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


-- Location Management ---------------------------------------------------------

instance HasRange Persistence where
  getRange (Persistence a b) = M.mappend (getRange a) (getRange b)

instance HasRange InitialCompromise where
  getRange (ICLoc lic) = getRange lic
  getRange _           = M.mempty

instance HasRange DirectAttack where
  getRange (DALoc ld) = getRange ld
  getRange _          = M.mempty

instance HasRange IndirectAttack where
  getRange _ = M.mempty

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


-- Pretty-printing -------------------------------------------------------------

instance PP Decl where
  ppPrec _ Decl { .. } = hang (pp dName <+> char '=') 2 (pp dAPT)

  ppList ds = vcat (map pp ds)

instance PP APT where
  ppPrec p APT { .. } =
    fsep $ commas
         $ concat [ [ ppPrec p aptInitialCompromise ]
                  , [ ppPrec p ex | ex <- aptExpansion ]
                  ]

instance PP InitialCompromise where
  ppPrec p (DirectAttack   da)  = ppPrec p da
  ppPrec p (IndirectAttack ia)  = ppPrec p ia
  ppPrec p (ICLoc          lic) = ppPrec p lic

instance PP DirectAttack where
  ppPrec _ ZeroDay              = text "Zero_day_activity"
  ppPrec _ ExploitVulnerability = text "Exploit_vulnerability_activity"
  ppPrec p (DALoc lda)          = ppPrec p lda

instance PP IndirectAttack where
  ppPrec _ _ = text "..."

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
