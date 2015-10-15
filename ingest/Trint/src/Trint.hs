{-# LANGUAGE OverloadedStrings #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import Ingest
import SimpleGetOpt
import Types as T

import Data.Monoid ((<>))
import Data.List (intersperse)
import qualified Data.Text.Lazy as L
import qualified Data.Text.Lazy.IO as L

data Config = Config { lintOnly   :: Bool
                     , files      :: [FilePath]
                     }

defaultConfig = Config False []

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files to be scanned.")]
               , progParams    = \p s -> Right s { files = p : files s }
               , progOptions   =
                  [ Option ['l'] ["lint"]
                    "Check the given file for syntactic and type issues."
                    $ NoArg $ \s -> Right s { lintOnly = True }
                  ]
               }

main :: IO ()
main =
  do c <- getOpts opts
     mapM_ (trint (lintOnly c)) (files c)

trint :: Bool -> FilePath -> IO ()
trint lint fp =
  do eres <- ingestText <$> L.readFile fp
     case eres of
      Left e            -> L.putStrLn $ "Error: " <> (L.pack $ show e)
      Right (res,ws)    ->
          do printWarnings ws
             -- XXX
             -- let doc = T.unlines $ map ppStmt res
             -- when (not lint) (writeFile (fp <.> "trint") doc)
             return ()

printWarnings :: [Warning] -> IO ()
printWarnings ws =
  do let doc = L.unlines $ intersperse "\n" $ map ppWarning ws
     L.putStrLn doc
