module Namespaces
  ( dc,adapt,foaf,nfo,URI
  ) where

import Network.URI

dc,adapt,foaf,nfo :: URI
dc    = perr "http://purl.org/dc/elements/1.1/"
adapt = perr "http://adapt.galois.com/"
foaf  = perr "http://xmlns.com/foaf/0.1/"
nfo   = perr "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo/v1.2/"

perr :: String -> URI
perr x = maybe (error $ "Impossible: failed to parse build-in namespace of " ++ x) id $ parseURI x
