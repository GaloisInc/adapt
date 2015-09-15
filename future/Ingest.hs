
module Ingest
    ( -- * High-level interface
      readTriples, readTriplesFromFile
    , Error(..), ParseError(..), TypeError(..)
    , TypeAnnotatedTriple(..)
    , Object(..), Entity, Verb
    , PP(..)
    ) where

import Types
import Typecheck
import Compile
import Parser
import qualified Control.Exception as X
import qualified Data.Text.IO as Text
import Graph

readTriplesFromFile ::  FilePath -> IO [TypeAnnotatedTriple]
readTriplesFromFile fp = (either X.throw id . readTriples) <$> Text.readFile  fp

readTriples :: Text -> Either Error [TypeAnnotatedTriple]
readTriples raw =
  case parseStrict raw of
    Left e -> Left (PE e)
    Right ts -> case typecheck ts of
                  Left e   -> Left (TE e)
                  Right xs -> Right (translate xs)
