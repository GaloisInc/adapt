{-# LANGUAGE DeriveDataTypeable #-}
{-# LANGUAGE OverloadedStrings  #-}
module Namespaces
  ( prov,dc,adapt,foaf,nfo,URI
  , Ident(..), domain, local, mkIdent, (.:), textOfIdent
  , allIdent
  , adaptIdent
  , adaptUnitOfExecution, adaptPid, adaptPPid, adaptPrivs, adaptPwd, adaptRegistryKey, adaptDevType, adaptDeviceID
  , adaptArtifact, adaptArtifactType, adaptCmdLine, adaptCmdString, adaptMachineID
  , adaptAccept, adaptRecv, adaptArgs, adaptRead, adaptExecute, adaptReturnVal, adaptUseOp
  , adaptFilePath
  , foafIdent
  , foafName, foafAccountName
  , provIdent
  , provAtTime
  , provType
  , provActivity
  , provAgent
  , provWasAssociatedWith
  , provEntity
  , provUsed
  , provWasStartedBy
  , provWasGeneratedBy
  , provWasEndedBy
  , provWasInformedBy
  , provWasAttributedTo
  , provWasDerivedFrom
  , provActedOnBehalfOf
  , provWasInvalidatedBy
  , dcIdent
  , dcDescription, dcIsPartOf
  , blankNode
  ) where

import Network.URI
import qualified Data.Text.Lazy as L
import Data.Text.Lazy (Text)
import Data.Data
import Data.Monoid ((<>))

--------------------------------------------------------------------------------
--  Identifiers

data Ident = Qualified Text Text -- XXX the domain should be a URI.
           | Unqualified Text
  deriving (Eq,Ord,Show,Data,Typeable)

domain :: Ident -> Maybe Text
domain (Qualified t _ ) = Just t
domain _                = Nothing

local  :: Ident -> Text
local (Qualified _ t) = t
local (Unqualified t) = t

textOfIdent :: Ident -> Text
textOfIdent (Qualified a b) = a <> ":" <> b
textOfIdent (Unqualified b) = b

dc,adapt,foaf,nfo :: URI
dc    = perr "http://purl.org/dc/elements/1.1/"
adapt = perr "http://adapt.galois.com/"
foaf  = perr "http://xmlns.com/foaf/0.1/"
nfo   = perr "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo/v1.2/"
prov  = perr "http://www.w3.org/ns/prov#"

allIdent :: [Ident]
allIdent = adaptIdent ++ provIdent ++ foafIdent ++ dcIdent

adaptIdent :: [Ident]
adaptIdent =
  [ adaptUnitOfExecution, adaptPid, adaptPPid, adaptPrivs, adaptPwd, adaptRegistryKey, adaptDevType, adaptDeviceID
  , adaptArtifact, adaptArtifactType, adaptCmdLine, adaptCmdString, adaptMachineID
  , adaptAccept, adaptRecv, adaptArgs, adaptRead, adaptExecute, adaptReturnVal, adaptUseOp
  , adaptFilePath
  ]

adaptUnitOfExecution, adaptPid, adaptDevType, adaptDeviceID, adaptArtifactType, adaptCmdLine, adaptCmdString, adaptMachineID, adaptFilePath, adaptPPid, foafName, foafAccountName, provAtTime, provType :: Ident
adaptUnitOfExecution = adapt .: "unitOfExecution"
adaptPid             = adapt .: "pid"
adaptPPid            = adapt .: "ppid"
adaptRegistryKey     = adapt .: "registryKey"
adaptPrivs           = adapt .: "privs"
adaptPwd             = adapt .: "pwd"
adaptDevType         = adapt .: "devType"
adaptDeviceID        = adapt .: "devID"
adaptArtifact        = adapt .: "artifact"
adaptArtifactType    = adapt .: "artifactType"
adaptCmdLine         = adapt .: "cmdLine"
adaptCmdString       = adapt .: "cmdString"
adaptFilePath        = adapt .: "filePath"
adaptMachineID       = adapt .: "machineID"
adaptRead            = adapt .: "read"
adaptRecv            = adapt .: "recv"
adaptAccept          = adapt .: "accept"
adaptExecute         = adapt .: "execute"
adaptReturnVal       = adapt .: "returnVal"
adaptArgs            = adapt .: "args"
adaptUseOp           = adapt .: "useOp"


foafIdent :: [Ident]
foafIdent = [foafName, foafAccountName]

foafName              = foaf  .: "name"
foafAccountName       = foaf  .: "accountName"

provIdent :: [Ident]
provIdent =
  [ provAtTime , provType , provActivity , provAgent , provWasAssociatedWith , provEntity
  , provUsed , provWasStartedBy , provWasGeneratedBy , provWasEndedBy , provWasInformedBy
  , provWasAttributedTo , provWasDerivedFrom , provActedOnBehalfOf , provWasInvalidatedBy
  ]

provAtTime            = prov  .: "atTime"
provType              = prov  .: "type"
provActivity          = prov  .: "activity"
provAgent             = prov  .: "agent"
provWasAssociatedWith = prov  .: "wasAssociatedWith"
provEntity            = prov  .: "entity"
provUsed              = prov  .: "used"
provWasStartedBy      = prov  .: "wasStartedBy"
provWasGeneratedBy    = prov  .: "wasGeneratedBy"
provWasEndedBy        = prov  .: "wasEndedBy"
provWasInformedBy     = prov  .: "wasInformedBy"
provWasAttributedTo   = prov  .: "wasAttributedTo"
provWasDerivedFrom    = prov  .: "wasDerivedFrom"
provActedOnBehalfOf   = prov  .: "actedOnBehalfOf"
provWasInvalidatedBy  = prov  .: "wasInvalidatedBy"

dcIdent :: [Ident]
dcIdent = [dcDescription, dcIsPartOf]

dcDescription = dc .: "description"
dcIsPartOf    = dc .: "isPartOf"

blankNode :: Ident
blankNode = Unqualified "_"

mkIdent :: URI -> Text -> Ident
mkIdent d l = Qualified (L.pack (show d)) l

(.:) :: URI -> Text -> Ident
(.:) = mkIdent

--------------------------------------------------------------------------------
--  Utils

perr :: String -> URI
perr x = maybe (error $ "Impossible: failed to parse build-in namespace of " ++ x) id $ parseURI x

