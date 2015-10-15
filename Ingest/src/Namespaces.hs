{-# LANGUAGE DeriveDataTypeable #-}
{-# LANGUAGE OverloadedStrings  #-}
module Namespaces
  ( prov,dc,adapt,foaf,nfo,URI
  , Ident(..), mkIdent, (.:), textOfIdent
  , adaptUnitOfExecution, adaptPid, adaptRegistryKey, adaptDevType, adaptDeviceID, adaptArtifact, adaptArtifactType, adaptCmdLine, adaptCmdString, adaptMachineID
  , foafName, foafAccountName
  , provAtTime, provType
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
  deriving (Eq,Ord,Show,Data)

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

adaptUnitOfExecution, adaptPid, adaptDevType, adaptDeviceID, adaptArtifactType, adaptCmdLine, adaptCmdString, adaptMachineID, foafName, foafAccountName, provAtTime, provType :: Ident
adaptUnitOfExecution = adapt .: "unitOfExecution"
adaptPid             = adapt .: "pid"
adaptRegistryKey     = adapt .: "registryKey"
adaptDevType         = adapt .: "devType"
adaptDeviceID        = adapt .: "devID"
adaptArtifact        = adapt .: "artifact"
adaptArtifactType    = adapt .: "artifactType"
adaptCmdLine         = adapt .: "cmdLine"
adaptCmdString       = adapt .: "cmdString"
adaptMachineID       = adapt .: "machineID"
foafName             = foaf  .: "name"
foafAccountName      = foaf  .: "accountName"
provAtTime           = prov  .: "atTime"
provType             = prov  .: "type"


mkIdent :: URI -> Text -> Ident
mkIdent d l = Qualified (L.pack (show d)) l

(.:) :: URI -> Text -> Ident
(.:) = mkIdent

--------------------------------------------------------------------------------
--  Utils

perr :: String -> URI
perr x = maybe (error $ "Impossible: failed to parse build-in namespace of " ++ x) id $ parseURI x

