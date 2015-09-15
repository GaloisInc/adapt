{-# LANGUAGE DeriveDataTypeable #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE MultiParamTypeClasses #-}

-- | Usage:
--
-- @
-- translate . either error id . typecheck . parseTriples :: Text -> [TypeAnnotatedTriple]
-- @
module Types
    ( -- * Types
      Error(..)
    , ParseError(..)
    , TypeError(..)
      -- * Key types
    , Triple(..)
    , Type(..)
    , Object(..), Entity, Verb
      -- * Monomorphic wrappers around 'Triple'
    , RawTA1(..), TypeAnnotatedTriple(..)
      -- * Classes
    , PP(..), render
      -- * Re-exports
    , Text
    ) where

import           Data.Monoid
import           Data.Data
import           Data.Text (Text)
import qualified Data.Text as Text
import qualified Control.Exception as X

data Error = PE ParseError | TE TypeError deriving (Eq, Ord, Show, Data, Typeable)

data Triple a =
        Triple { triSubject  :: Entity a
               , triVerb     :: Verb a
               , triObject   :: Entity a }
            deriving (Eq, Ord, Show, Read)

data Object a
        = -- IRIs are in the form 'http://some/location', 'cid://computation/identifier', etc.
          IRI { namespace              :: Text  -- URI form such as 'http' 'cid' etc.
              , theObject              :: Text  -- Everything after '://'
              , objectTag              :: a
              }
        | Lit { litValue               :: Text
              , objectTag              :: a
              }
            deriving (Eq, Ord, Show, Read)

type Entity a = Object a
type Verb a   = Object a

newtype RawTA1 = RawTA1 { unRTA :: Triple () }
            deriving (Eq, Ord, Show, Read)

newtype TypeAnnotatedTriple = TypeAnnotatedTriple { unTAT :: Triple Type }
            deriving (Eq, Ord, Show, Read)

data ParseError = ParseFailure String | PartialParse
            deriving (Eq, Ord, Show, Data, Typeable)

--------------------------------------------------------------------------------
--  PrettyPrinting

render :: PP a => [a] -> Text
render = Text.concat . map pp

class PP a where
    pp :: a -> Text

instance PP (Triple ()) where
    pp (Triple s p o) =
        Text.unwords [ pp s , pp p , pp o , " .\n"]

instance PP (Triple Type) where
    pp (Triple s p o) =
        Text.unwords [ pp s , pp p , pp o , " : "
                     , ty, " .\n" ]
     where ty = Text.unwords [pp (objectTag s), "->", pp (objectTag o)]

instance PP (Object a) where
    pp (IRI ns o _) = ns <> "://" <> o
    pp (Lit t    _) = t

instance PP Type where
    pp (TyArrow a b) = pp a <> " -> " <> pp b
    pp ty            = Text.pack $ show ty


instance PP TypeAnnotatedTriple where
  pp (TypeAnnotatedTriple t) = pp t


data Type = EntityClass | ActorClass | ResourceClass | DataClass | Agent
          | UnitOfExecution | Host | Socket | File | Packet | Memory
          | TyString | TyArrow Type Type
        deriving (Data, Typeable, Eq, Ord, Show, Read)

data TypeError = TypeError Type Type | CanNotInferType Text
        deriving (Data, Typeable, Eq, Ord, Show, Read)

instance X.Exception TypeError
instance X.Exception ParseError
instance X.Exception Error
