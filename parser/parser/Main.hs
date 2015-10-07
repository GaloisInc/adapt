module Main where

import           Adapt.Parser

import qualified Data.Text.Lazy.IO as L
import           System.Environment (getArgs)

main :: IO ()
main  =
  do [input] <- getArgs
     bytes   <- L.readFile input
     putStrLn (either show pretty (parseDecls input bytes))
