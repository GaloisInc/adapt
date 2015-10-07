module Adapt.Parser.PP (
    PP(..),
    pp,
    pretty,
    module Text.PrettyPrint.HughesPJ
  ) where

import qualified Data.Text.Lazy as L
import           Text.PrettyPrint.HughesPJ


pretty :: PP a => a -> String
pretty a = show (pp a)

pp :: PP a => a -> Doc
pp  = ppPrec 0

commas :: [Doc] -> [Doc]
commas  = punctuate comma

class PP a where
  ppPrec :: Int -> a -> Doc

  ppList :: [a] -> Doc
  ppList as = fsep (commas (map pp as))

instance PP a => PP [a] where
  ppPrec _ as = ppList as

instance PP Int where
  ppPrec _ = int

instance PP Char where
  ppPrec _ = char
  ppList   = text

instance PP L.Text where
  ppPrec _ t = text (L.unpack t)
