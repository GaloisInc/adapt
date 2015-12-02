{-# LANGUAGE DeriveFoldable    #-}
{-# LANGUAGE DeriveTraversable #-}
{-# LANGUAGE DeriveFunctor     #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE DeriveDataTypeable #-}

module Position where

import           PP

import           Data.Function (on)
import qualified Data.Foldable as F
import qualified Data.Monoid as M
import qualified Data.Traversable as T
import qualified Data.Text.Lazy as L
import           Data.Data


-- Positions -------------------------------------------------------------------

data Position = Position { posRow, posCol, posOff :: {-# UNPACK #-} !Int
                         } deriving (Show, Data, Typeable)

instance Eq Position where
  (==) = (==) `on` posOff
  (/=) = (/=) `on` posOff

instance Ord Position where
  compare = compare `on` posOff

instance PP Position where
  ppPrec _ Position { .. } = pp posRow <> char ':' <> pp posCol

-- | Advance a position by one character.
advance :: Char -> Position -> Position
advance '\t' Position { .. } = Position { posCol = posCol + 8
                                        , posOff = posOff + 8
                                        , .. }

advance '\n' Position { .. } = Position { posRow = posRow + 1
                                        , posCol = 1
                                        , posOff = posOff + 1 }

advance _    Position { .. } = Position { posCol = posCol + 1
                                        , posOff = posOff + 1
                                        , .. }


-- Ranges ----------------------------------------------------------------------

data Range = NoLoc
           | Range !Position !Position FilePath
             deriving (Eq,Ord,Show,Data,Typeable)

instance M.Monoid Range where
  mempty = NoLoc

  mappend (Range s1 e1 l) (Range s2 e2 _) = Range (min s1 s2) (max e1 e2) l
  mappend NoLoc b                         = b
  mappend a NoLoc                         = a

instance PP Range where
  ppPrec _ NoLoc               = text "<no location>"
  ppPrec _ (Range start end _) = parens (pp start <> char '-' <> pp end)


-- | Select the lines included in the range given, with some amount of context
-- lines around it.
getLines :: L.Text -> Int -> Range -> L.Text
getLines _     _   NoLoc         = L.empty
getLines input cxt (Range s _ _) = L.unlines
                                 $ take len
                                 $ drop start
                                 $ L.lines input
  where

  (start,len)
    | cxt >= posRow s = (0,posRow s)
    | otherwise       = (posRow s - 1 - cxt,cxt + 1)


-- Located Things --------------------------------------------------------------

-- | A value paired with a source location.
data Located a = Located { locRange :: !Range
                         , locValue :: a
                         } deriving (Eq,Ord,Show,Functor,F.Foldable,T.Traversable,Data,Typeable)

-- by default, print with no location information
instance PP a => PP (Located a) where
  ppPrec p Located { .. } = text "\"" PP.<> ppPrec p locValue PP.<> text "\"" PP.<> text " at " PP.<> pp locRange


class HasRange a where
  getRange :: a -> Range

instance (HasRange a, HasRange b) => HasRange (a,b) where
  getRange (a,b) = getRange a `M.mappend` getRange b

instance HasRange a => HasRange [a] where
  getRange = F.foldMap getRange
  {-# INLINE getRange #-}

instance HasRange a => HasRange (Maybe a) where
  getRange = F.foldMap getRange
  {-# INLINE getRange #-}

instance HasRange (Located a) where
  getRange = locRange
  {-# INLINE getRange #-}

instance HasRange Range where
  getRange = id
  {-# INLINE getRange #-}


-- | Convenience function for creating located things.
at :: HasRange range => a -> range -> Located a
at locValue range = Located { locRange = getRange range, .. }
