{-# LANGUAGE OverloadedStrings #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import Ingest
import SimpleGetOpt
import Types as T
import qualified Graph as G

import Control.Monad (when)
import Data.Monoid ((<>))
import Data.Maybe (catMaybes)
import Data.List (intersperse)
import Data.Graph
import qualified Data.Map as Map
import           Data.Map (Map)
import MonadLib
import MonadLib.Monads
import qualified Data.Text.Lazy as L
import qualified Data.Text.Lazy.IO as L
import System.FilePath ((<.>))

data Config = Config { lintOnly   :: Bool
                     , graph      :: Bool
                     , stats      :: Bool
                     , files      :: [FilePath]
                     }

defaultConfig :: Config
defaultConfig = Config False False False []

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
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
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
         when (stats c) (printStats res)
         -- XXX
         -- let doc = T.unlines $ map ppStmt res
         -- when (not lint) (writeFile (fp <.> "trint") doc)
         return ()

printWarnings :: [Warning] -> IO ()
printWarnings ws =
  do let doc = L.unlines $ intersperse "\n" $ map ppWarning ws
     L.putStrLn doc

printStats :: [Stmt] -> IO ()
printStats ss =
  do let g  = mkGraph ss
         vs = vertices g
         mn = length (take 1 ss) -- one node suggests the minimum subgraph is one.
         sz = maximum (mn : map (length . reachable g) vs) -- XXX O(n^2) algorithm!
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show (length vs)
     putStrLn $ "\tPredicates:       " ++ show (length ss - length vs)
     putStrLn $ "\tTotal statements: " ++ show (length ss)

data NodeInfo = Node Int | Edge (Int,Int)

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: [Stmt] -> Graph
mkGraph ss =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge ss
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Stmt -> State (Map Text Vertex,Vertex) (Maybe Edge)
mkEdge (StmtPredicate (Predicate s o _ _)) =
  do nS <- nodeOf s
     nO <- nodeOf o
     return $ Just (nS,nO)
mkEdge _ = return Nothing

-- Memoizing node numbering
nodeOf :: Text -> State (Map Text Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n
