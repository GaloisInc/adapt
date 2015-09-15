
module Ingest
    ( -- * High-level interface
      readTriples
    , Error(..), ParseError(..), TypeError(..)
    , TypeAnnotatedTriple(..)
    , Object(..), Entity, Verb
    , PP(..)
    ) where

import Types
import Typecheck
import Compile
import Parser

readTriples :: Text -> Either Error [TypeAnnotatedTriple]
readTriples raw =
  case parseStrict raw of
    Left e -> Left (PE e)
    Right ts -> case typecheck ts of
                  Left e   -> Left (TE e)
                  Right xs -> Right (translate xs)
