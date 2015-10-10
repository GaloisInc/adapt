{
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE OverloadedStrings #-}
module Adapt.Parser (

    -- * Parser
    parseDecls,

    module Exports,
    ppError

  ) where

import           Adapt.Parser.AST as Exports
import           Adapt.Parser.Lexeme
import           Adapt.Parser.Lexer
import           Adapt.Parser.PP as Exports (PP(..), pp, pretty)
import           Adapt.Parser.Position as Exports

import qualified Control.Applicative as A
import qualified Data.Monoid as M
import qualified Data.Text.Lazy as L
import           MonadLib (runM,StateT,get,set,ExceptionT,raise,Id)

}

%token
  IDENT    { $$ @ (Located _ (Ident _   ) ) }

  ';'      { Located $$ (Symbol SymSep)    }
  '='      { Located $$ (Symbol SymDef)    }

  'Zero_day_activity'              { Located $$ (Activity "Zero_day_activity") }
  'Exploit_vulnerability_activity' { Located $$ (Activity "Exploit_vulnerability_activity") }

  'Account_enum_activity'          { Located $$ (Activity "Account_enum_activity")      }
  'File_system_enum_activity'      { Located $$ (Activity "File_system_enum_activity")  }
  'Group_permissions_activity'     { Located $$ (Activity "Group_permissions_activity") }
  'Process_enum_activity'          { Located $$ (Activity "Process_enum_activity")      }

  'Credential_dumping_activity'    { Located $$ (Activity "Credential_dumping_activity") }
  'Credential_in_file_activity'    { Located $$ (Activity "Credential_in_file_activity") }
  'Network_sniffing_activity'      { Located $$ (Activity "Network_sniffing_activity")   }
  'Keyboard_logging_activity'      { Located $$ (Activity "Keyboard_logging_activity")   }

  'New_machine_access_activity'    { Located $$ (Activity "New_machine_access_activity") }
  'Infect_new_machine_activity'    { Located $$ (Activity "Infect_new_machine_activity") }

  'Download_backdoor_activity'     { Located $$ (Activity "Download_backdoor_activity") }
  'Initiate_command_control_activity'{ Located $$ (Activity "Initiate_command_control_activity") }
  'Listen_cc_activity'             { Located $$ (Activity "Listen_cc_activity")         }
  'Send_cc_activity'               { Located $$ (Activity "Send_cc_activity")           }
  'Cover_tracks_activity'          { Located $$ (Activity "Cover_tracks_activity")      }

  'Bios_activity'                  { Located $$ (Activity "Bios_activity") }
  'Dll_activity'                   { Located $$ (Activity "Dll_activity") }
  'Registry_activity'              { Located $$ (Activity "Registry_activity") }

  'Compress_activity'              { Located $$ (Activity "Compress_activity") }
  'Encrypt_activity'               { Located $$ (Activity "Encrypt_activity") }
  'Establish_exfil_points_activity'{ Located $$ (Activity "Establish_exfil_points_activity") }
  'Exfil_physical_medium_activity' { Located $$ (Activity "Exfil_physical_medium_activity") }

  'Http_post_activity'             { Located $$ (Activity "Http_post_activity") }
  'Dns_activity'                   { Located $$ (Activity "Dns_activity") }
  'Https_activity'                 { Located $$ (Activity "Https_activity") }
  'Ftp_activity'                   { Located $$ (Activity "Ftp_activity") }
  'Ssh_activity'                   { Located $$ (Activity "Ssh_activity") }
  'UDP_activity'                   { Located $$ (Activity "UDP_activity") }
  'TCP_activity'                   { Located $$ (Activity "TCP_activity") }

  'User_open_file_activity'             { Located $$ (Activity "User_open_file_activity") }
  'Application_compromise_activity'     { Located $$ (Activity "Application_compromise_activity") }
  'Initial_malware_download_activity'   { Located $$ (Activity "Initial_malware_download_activity") }
  'User_click_link_activity'            { Located $$ (Activity "User_click_link_activity") }
  'Browser_exploit_download_activity'   { Located $$ (Activity "Browser_exploit_download_activity") }
  'User_visits_tainted_URL_activity'    { Located $$ (Activity "User_visits_tainted_URL_activity") }
  'User_insert_usb_activity'            { Located $$ (Activity "User_insert_usb_activity") }
  'Usb_autorun_activity'                { Located $$ (Activity "Usb_autorun_activity") }
  'User_click_rogue_link_file_activity' { Located $$ (Activity "User_click_rogue_link_file_activity") }


%tokentype { Lexeme }
%monad     { ParseM }
%lexer     { lexer  } { Located _ EOF }

%name decls decls

%%

-- Utilities -------------------------------------------------------------------

ident :: { Located L.Text }
  : IDENT { let Located r (Ident n) = $1
            in n `at` r }

-- Declarations ----------------------------------------------------------------

 -- REVERSED
decls :: { [Decl] }
  : {- empty -}    { []      }
  | decl           { [$1]    }
  | decls ';' decl { $3 : $1 }

decl :: { Decl }
  : ident '=' apt { Decl { dName = $1, dAPT = $3 } }


-- APTs ------------------------------------------------------------------------

apt :: { Located APT }
  : initial_compromise
      persistence
      expansion_list
      exfiltration

    { APT { aptInitialCompromise = $1
          , aptPersistence       = $2
          , aptExpansion         = reverse $3
          , aptExfiltration      = $4
          } `at` ($1,$4) }


-- Initial Compromise ----------------------------------------------------------

initial_compromise :: { InitialCompromise }
  : direct_attack   { DirectAttack   $1 }
  | indirect_attack { IndirectAttack $1 }

direct_attack :: { DirectAttack }
  : 'Zero_day_activity'              { DALoc (ZeroDay              `at` $1) }
  | 'Exploit_vulnerability_activity' { DALoc (ExploitVulnerability `at` $1) }

indirect_attack :: { IndirectAttack }
  : malicious_attachment { MaliciousAttachment $1 }
  | drive_by_download    { DriveByDownload     $1 }
  | waterholing          { Waterholing         $1 }
  | usb_infection        { UsbInfection        $1 }

malicious_attachment :: { MaliciousAttachment }
  : 'User_open_file_activity'
        'Application_compromise_activity'
        'Initial_malware_download_activity'
    { MALoc (UserOpenFile `at` ($1,$3)) }

drive_by_download :: { DriveByDownload }
  : 'User_click_link_activity'
        'Browser_exploit_download_activity'
    { DDLoc (UserClickLink `at` ($1,$2)) }

waterholing :: { Waterholing }
  : 'User_visits_tainted_URL_activity'
        'Browser_exploit_download_activity'
    { WHLoc (UserVisitsTaintedURL `at` ($1,$2)) }

usb_infection :: { UsbInfection }
  : 'User_insert_usb_activity'     usb_exploit_start
    { UILoc (UserInsertUsb $2 `at` ($1,$2)) }

usb_exploit_start :: { UsbExploitStart }
  : 'Usb_autorun_activity'                { UELoc (UsbAutorun             `at` $1) }
  | 'User_click_rogue_link_file_activity' { UELoc (UserClickRogueLinkFile `at` $1) }


-- Persistence -----------------------------------------------------------------

persistence :: { Persistence }
  : maintain_access evade_defenses { Persistence $1 $2 }

maintain_access :: { MaintainAccess }
  : command_control
        'Download_backdoor_activity'
        opt_modify_target
    { MaintainAccess $1 $3 }

opt_modify_target :: { Maybe ModifyTarget }
  : {- empty -}   { Nothing }
  | modify_target { Just $1 }

modify_target :: { ModifyTarget }
  : 'Bios_activity'     { MTLoc (ModifyBios     `at` $1) }
  | 'Dll_activity'      { MTLoc (ModifyDll      `at` $1) }
  | 'Registry_activity' { MTLoc (ModifyRegistry `at` $1) }

command_control :: { CommandControl }
  : 'Initiate_command_control_activity' maintain_command_control_list
    { CCLoc (InitialCommandControl (reverse $2) `at` ($1,$2)) }

-- REVERSED
maintain_command_control_list :: { [MaintainCommandControl] }
  : {- empty -}                                                { []      }
  | maintain_command_control_list     maintain_command_control { $2 : $1 }

maintain_command_control :: { MaintainCommandControl }
  : 'Listen_cc_activity' { MCLoc (ListenCC `at` $1) }
  | 'Send_cc_activity'   { MCLoc (SendCC   `at` $1) }

evade_defenses :: { EvadeDefenses }
  : 'Cover_tracks_activity' { EDLoc (CoverTracks `at` $1) }


-- Expansion -------------------------------------------------------------------

-- REVERSED
expansion_list :: { [Expansion] }
  : {- empty -}                  { []      }
  | expansion_list     expansion { $2 : $1 }

expansion :: { Expansion }
  : host_enumeration   { ELoc (HostEnumeration   $1 `at` $1) }
  | access_credentials { ELoc (AccessCredentials $1 `at` $1) }
  | move_laterally     { ELoc (MoveLaterally     $1 `at` $1) }

host_enumeration :: { HostEnumeration }
  : 'Account_enum_activity'      { HELoc (AccountEnum      `at` $1) }
  | 'File_system_enum_activity'  { HELoc (FileSystemEnum   `at` $1) }
  | 'Group_permissions_activity' { HELoc (GroupPermissions `at` $1) }
  | 'Process_enum_activity'      { HELoc (ProcessEnum      `at` $1) }

access_credentials :: { AccessCredentials }
  : 'Credential_dumping_activity' { ACLoc (CredentialDumping `at` $1) }
  | 'Credential_in_file_activity' { ACLoc (CredentialInFile  `at` $1) }
  | 'Network_sniffing_activity'   { ACLoc (NetworkSniffing   `at` $1) }
  | 'Keyboard_logging_activity'   { ACLoc (KeyboardLogging   `at` $1) }

move_laterally :: { MoveLaterally }
  : 'New_machine_access_activity' opt_infect
    { let (infect,end) = case $2 of
                           Just loc -> (True,loc)
                           Nothing  -> (False,M.mempty)
      in MLLoc (NewMachineAccess infect `at` ($1,end)) }

opt_infect :: { Maybe Range }
  :     'Infect_new_machine_activity' { Just $1 }
  | {- empty -}                       { Nothing }


-- Exfiltration ----------------------------------------------------------------

exfiltration :: { Exfiltration }
  : exfil_staging     exfil_execution { Exfiltration (Just $1) $2 }
  | exfil_execution                   { Exfiltration Nothing $1 }

exfil_staging :: { ExfilStaging }
  : exfil_format     exfil_infrastructure
    { ExfilStaging $1 $2 }

exfil_format :: { ExfilFormat }
  : 'Compress_activity' opt_encrypt
    { let (encrypt,end) = case $2 of
                            Just end -> (True,end)
                            Nothing  -> (False,M.mempty)
      in EFLoc (Compress encrypt `at` ($1,end)) }

opt_encrypt :: { Maybe Range }
  : {- empty -}            { Nothing }
  |     'Encrypt_activity' { Just $1 }

exfil_infrastructure :: { ExfilInfrastructure }
  : 'Establish_exfil_points_activity'
    { EILoc (EstablishExfilPoints `at` $1) }

exfil_execution :: { ExfilExecution }
  : exfil_channel     exfil_socket { ExfilExecution $1 $2 }

exfil_channel :: { ExfilChannel }
  : 'Exfil_physical_medium_activity' { ECLoc (ExfilPhysicalMedium `at` $1) }
  | 'Http_post_activity'             { ECLoc (HttpPost            `at` $1) }
  | 'Dns_activity'                   { ECLoc (Dns                 `at` $1) }
  | 'Https_activity'                 { ECLoc (Https               `at` $1) }
  | 'Ftp_activity'                   { ECLoc (Ftp                 `at` $1) }
  | 'Ssh_activity'                   { ECLoc (Ssh                 `at` $1) }

exfil_socket :: { ExfilSocket }
  : 'UDP_activity' { ESLoc (UDP `at` $1) }
  | 'TCP_activity' { ESLoc (TCP `at` $1) }

{

-- External Interface-----------------------------------------------------------

parseDecls :: String -> L.Text -> Either Error [Decl]
parseDecls source bytes = runParseM (primLexer source bytes) decls


-- Parser Internals ------------------------------------------------------------

newtype ParseM a = ParseM { unParseM :: StateT [Lexeme] (ExceptionT Error Id) a
                          } deriving (A.Applicative,Functor,Monad)

runParseM :: [Lexeme] -> ParseM a -> Either Error a
runParseM toks m =
  case runM (unParseM m) toks of
    Right (a,_) -> Right a
    Left err    -> Left err

lexer :: (Lexeme -> ParseM a) -> ParseM a
lexer k = ParseM $
  do toks <- get
     case toks of

       Located _ (Error err) : _ ->
            raise err

       t : ts ->
         do set ts
            unParseM (k t)

       [] ->
            raise UnexpectedEOF

-- | Raise a general happy error, but try to give some useful information about
-- where the error came from.
happyError :: ParseM a
happyError  = ParseM $
  do toks <- get
     case toks of
       tok : _ -> raise (HappyError (getRange tok))
       _       -> raise (HappyError M.mempty)

-- vim: ft=haskell
}
