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
    , Object(..), Entity, Predicate
    , Verb(..), IRI(..)
    , Resource(..)
      -- * Monomorphic wrappers around 'Triple'
    , RawTA1(..), TypeAnnotatedTriple(..)
      -- * Classes
    , PP(..), render
      -- * Re-exports
    , Text
    ) where

import           Data.Monoid
import           Data.Data
import           Data.Char (toLower)
import           Data.Text (Text)
import qualified Data.Text as Text
import qualified Control.Exception as X

data Error = PE ParseError | TE TypeError deriving (Eq, Ord, Show, Data, Typeable)

data Triple a =
        Triple { triSubject  :: Entity a
               , triVerb     :: Predicate a
               , triObject   :: Entity a }
            deriving (Eq, Ord, Show, Read)

data Object container tag =
          Obj { theObject              :: container -- IRI: after '://'.  
              , objectTag              :: tag
              }
            deriving (Eq, Ord, Show, Read)

-- IRIs are in the form 'http://some/location', 'cid://computation/identifier', etc.
data IRI = IRI { namespace :: Text
               , iriBody   :: Text
               }
            deriving (Eq, Ord, Show, Read)

type Entity    a = Object IRI a
type Predicate a = Object Verb a

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

instance PP c => PP (Object c a) where
    pp (Obj cont _) = pp cont

instance PP IRI where
   pp (IRI a b) = a <> "://" <> b

instance PP Type where
    pp (TyArrow a b) = pp a <> " -> " <> pp b
    pp ty            = Text.pack $ show ty

instance PP Verb where
    pp v = case show v of
            (l:ls) -> Text.pack (toLower l : ls)
            []     -> ""

instance PP TypeAnnotatedTriple where
    pp (TypeAnnotatedTriple t) = pp t

data Type = EntityClass | ActorClass | Resource | Agent
          | UnitOfExecution | Host
          | TyString | TyArrow Type Type
        deriving (Data, Typeable, Eq, Ord, Show, Read)

data Resource = File | Socket | Semaphore | Memory | Packet
        deriving (Data, Typeable, Eq, Ord, Show, Read)

data TypeError = TypeError Type Type | CanNotInferType Text
        deriving (Data, Typeable, Eq, Ord, Show, Read)

instance X.Exception TypeError
instance X.Exception ParseError
instance X.Exception Error

data Verb
  = WasDerivedFrom
  | SpawnedBy
  | WasInformedBy
  | ActedOnBehalfOf
  | WasKilledBy
  | WasAttributedTo
  | Modified
  | Generated
  | Destroyed
  | Read
  | Write | WrittenBy
  | Is
  | A
            deriving (Eq, Ord, Show, Read)
