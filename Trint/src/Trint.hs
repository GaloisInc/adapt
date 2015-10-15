{-# LANGUAGE OverloadedStrings #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import Ingest
import SimpleGetOpt
import Types as T
import qualified Graph as G

import Control.Monad (when)
import Data.Monoid ((<>))
import Data.List (intersperse)
import qualified Data.Text.Lazy as L
import qualified Data.Text.Lazy.IO as L
import System.FilePath ((<.>))

data Config = Config { lintOnly   :: Bool
                     , graph      :: Bool
                     , files      :: [FilePath]
                     }

defaultConfig :: Config
defaultConfig = Config False False []

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files to be scanned.")]
               , progParams    = \p s -> Right s { files = p : files s }
               , progOptions   =
                  [ Option ['l'] ["lint"]
                    "Check the given file for syntactic and type issues."
                    $ NoArg $ \s -> Right s { lintOnly = True }
                  , Option ['g'] ["graph"]
                    "Produce a dot file representing a graph of the conceptual model."
                    $ NoArg $ \s -> Right s { graph = True }
                  ]
               }

main :: IO ()
main =
  do c <- getOpts opts
     mapM_ (trint c) (files c)

trint :: Config -> FilePath -> IO ()
trint c fp =
  do eres <- ingestText <$> L.readFile fp
     case eres of
      Left e            -> L.putStrLn $ "Error: " <> (L.pack $ show e)
      Right (res,ws)    ->
          do printWarnings ws
             doRest c fp res

doRest :: Config -> FilePath -> [Stmt] -> IO ()
doRest c fp res
  | lintOnly c = return ()
  | otherwise =
      do when (graph c) (writeFile (fp <.> "dot") (G.graph res))
         -- XXX
         -- let doc = T.unlines $ map ppStmt res
         -- when (not lint) (writeFile (fp <.> "trint") doc)
         return ()

printWarnings :: [Warning] -> IO ()
printWarnings ws =
  do let doc = L.unlines $ intersperse "\n" $ map ppWarning ws
     L.putStrLn doc
