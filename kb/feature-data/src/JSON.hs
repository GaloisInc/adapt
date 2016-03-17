{-# LANGUAGE TypeOperators #-}
{-# LANGUAGE DefaultSignatures #-}
{-# LANGUAGE FlexibleContexts #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE OverloadedStrings #-}

module JSON (
    ToFields(..),
    toObject,
    dumpJSONList,

    -- * Re-exported
    Pair, (.=),
  ) where

import           Control.Monad (foldM_)
import qualified Data.ByteString.Lazy.Char8 as L
import           Data.Aeson
import           Data.Aeson.Types (Pair)
import           Data.Char (isUpper,toLower)
import           GHC.Generics


class ToFields a where
  toFields :: a -> [Pair]

  default toFields :: (GToFields (Rep a), Generic a) => a -> [Pair]
  toFields a = gtoFields (from a)


toObject :: ToFields a => a -> Value
toObject a = object (toFields a)

dumpJSONList :: ToFields a => [a] -> IO ()
dumpJSONList xs =
  do foldM_ dump '[' xs
     putStrLn "]"
  where
  dump c x =
    do putStr [c,' ']
       L.putStrLn (encode (toObject x))
       return ','

unCamelCase :: String -> String
unCamelCase [] = []
unCamelCase (x:xs)
  | isUpper x = toLower x : concatMap hyphenate xs
  | otherwise =         x : concatMap hyphenate xs
  where
  hyphenate c
    | isUpper c = [ '-', toLower c ]
    | otherwise = [ c ]


class GToFields f where
  gtoFields :: f a -> [Pair]

-- Datatypes define the feature name
instance (GToFields f, Datatype c) => GToFields (M1 D c f) where
  gtoFields m@(M1 a) = "feature" .= unCamelCase (datatypeName m)
                     : gtoFields a

-- constructors are just passed through
instance GToFields f => GToFields (M1 C c f) where
  gtoFields (M1 a) = gtoFields a

instance GToFields f => GToFields (M1 S c f) where
  gtoFields (M1 a) = gtoFields a

instance (GToFields f, GToFields g) => GToFields (f :*: g) where
  gtoFields (l :*: r) = gtoFields l ++ gtoFields r

instance ToFields a => GToFields (K1 i a) where
  gtoFields (K1 a) = toFields a
