module Util where

import qualified Data.Text.Lazy as L
import Numeric (readDec)
import Data.Time
import Data.Char (isDigit)

parseUTC :: L.Text -> Maybe UTCTime
parseUTC chunk =
  let str      = L.unpack chunk
      numbers  = map (\x -> if isDigit x then x else ' ') str
  in case words numbers of
      [y,m,d,hh,mm,ss] ->
        let day  = fromGregorian (readD y) (readD m) (readD d)
            ps   = psH * readD hh + psM * readD mm + floor (psS * (readD ss :: Double))
            diff = picosecondsToDiffTime ps
        in Just (UTCTime day diff)
      _ -> Nothing
  where
  psH = psM * 60
  psM = psS * 60
  psS :: Num a => a
  psS = 1000000000000

  readD x = let ((n,_):_) = readDec x in n


