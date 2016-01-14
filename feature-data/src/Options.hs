{-# LANGUAGE RecordWildCards #-}

module Options (
    Options(..),
    parseArgs,
    showHelp,
  ) where

import System.Environment (getArgs,getProgName)
import System.Exit (exitFailure)


-- Options ---------------------------------------------------------------------

data Options = Options { optFeature :: String
                       , optRaw     :: Bool
                       } deriving (Show)


parseArgs :: IO Options
parseArgs  =
  do args <- getArgs
     case args of
       [optFeature,"raw"]    -> return Options { optRaw = True,  .. }
       [optFeature,"vector"] -> return Options { optRaw = False, .. }
       _                     -> showHelp >> exitFailure


showHelp :: IO ()
showHelp  =
  do prog <- getProgName
     putStrLn ("Usage: " ++ prog ++ " <feature> <raw|vector>")
