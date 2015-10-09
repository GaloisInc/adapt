module Main where

import           Adapt.Parser

import qualified Data.Text.Lazy.IO as L
import           System.Environment (getArgs)

main :: IO ()
main  =
  do [input] <- getArgs
     bytes   <- L.readFile input
     case parseDecls input bytes of
       Right decls -> print decls
       Left err    -> print (ppError (Just bytes) err)
