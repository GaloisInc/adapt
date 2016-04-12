{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE ParallelListComp  #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import           Control.Applicative ((<$>))
import           Control.Exception
import           Control.Monad (when)
import qualified Data.Binary.Get as G
import qualified Data.ByteString as BS
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Lazy as ByteString
import qualified Data.Foldable as F
import           Data.Graph hiding (Node, Edge)
import           Data.Int (Int32)
import           Data.List (partition,intersperse,scanl')
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (catMaybes,isNothing, mapMaybe)
import           Data.Monoid ((<>))
import           Data.Proxy (Proxy(..))
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.Encoding as Text
import qualified Data.Text.Lazy.IO as Text
import           Data.Time (UTCTime, addUTCTime)
import           FromProv
import           MonadLib        hiding (handle)
import           MonadLib.Monads hiding (handle)
import           Numeric (showHex)
import           PP (pretty, pp)
import           SimpleGetOpt
import           System.Exit (exitFailure)
import           System.FilePath ((<.>))
import           Text.Groom
import           Text.Read (readMaybe)

import qualified CommonDataModel.Avro  as Avro
import qualified CommonDataModel.Types as CDM

import Titan
import CompileSchema
import Network.HTTP.Types

import Network.Kafka as Kafka
import Network.Kafka.Producer as KProd

type VertsAndEdges = (Map Text (Maybe ResultId), Map Text (Maybe ResultId))

data Config = Config { lintOnly   :: Bool
                     , quiet      :: Bool
                     , stats      :: Bool
                     , ast        :: Bool
                     , verbose    :: Bool
                     , help       :: Bool
                     , upload     :: Maybe ServerInfo
                     , pushKafka  :: Maybe FilePath
                     , files      :: [FilePath]
                     } deriving (Show)

defaultConfig :: Config
defaultConfig = Config
  { lintOnly  = False
  , quiet     = False
  , stats     = False
  , ast       = False
  , verbose   = False
  , help      = False
  , upload    = Nothing
  , pushKafka = Nothing
  , files     = []
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files to be scanned.")]
               , progParams    = \p s -> Right s { files = p : files s }
               , progOptions   =
                  [ Option ['l'] ["lint"]
                    "Check the given file for syntactic and type issues."
                    $ NoArg $ \s -> Right s { lintOnly = True }
                  , Option ['q'] ["quiet"]
                    "Quiet linter warnings"
                    $ NoArg $ \s -> Right s { quiet = True }
                  , Option ['v'] ["verbose"]
                    "Verbose debugging messages"
                    $ NoArg $ \s -> Right s { verbose = True }
                  , Option ['a'] ["ast"]
                    "Produce a pretty-printed internal CDM AST"
                    $ NoArg $ \s -> Right s { ast = True }
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
                  , Option ['p'] ["push-to-kafka"]
                    "Send TA-1 CDM statements to the ingest daemon via it's kafka queue."
                    $ ReqArg "FILE" $
                        \file s -> Right s { pushKafka = Just file }
                  , Option ['u'] ["upload"]
                    "Uploads the data by inserting it into a Titan database using gremlin."
                    $ OptArg "Database host" $
                        \str s -> let svr = uncurry ServerInfo <$> (parseHostPort =<< str)
                                  in case (str,svr) of
                                    (Nothing,_)  -> Right s { upload = Just defaultServer }
                                    (_,Just res) -> Right s { upload = Just res }
                                    (_,Nothing) -> Left "Could not parse host:port string."
                  , Option ['h'] ["help"]

                    "Prints this help message."
                    $ NoArg $ \s -> Right s { help = True }
                  ]
               }

parseHostPort :: String -> Maybe (String,Int)
parseHostPort str =
  let (h,p) = break (== ':') str
  in case (h,readMaybe p) of
      ("",_)         -> Nothing
      (_,Nothing)    -> Nothing
      (hst,Just pt) -> Just (hst,pt)

main :: IO ()
main =
  do c <- getOpts opts
     if help c
      then dumpUsage opts
      else mapM_ (trint c) (files c)

trint :: Config -> FilePath -> IO ()
trint c fp = do
  eres <- ingest
  case eres of
    Left e  -> do
      putStrLn $ "Error ingesting " ++ fp ++ ":"
      print (pp e)
      return ()
    Right (ns,es,ws) -> do
      unless (quiet c) $ printWarnings ws
      processStmts c fp (ns,es)

  where
  ingest = do
    t <- handle onError (Text.readFile fp)
    translateTextCDM t
  onError :: IOException -> IO a
  onError e = do putStrLn ("Error reading " ++ fp ++ ":")
                 print e
                 exitFailure

processStmts :: Config -> FilePath -> ([Node],[Edge]) -> IO ()
processStmts c fp res@(ns,es)
  | lintOnly c = return ()
  | otherwise = do
      when (stats  c) $ do
        printStats res

      when (ast c) $ do
        let astfile = fp <.> "trint"
        dbg ("Writing ast to " ++ astfile)
        output astfile $ Text.unlines $ map (Text.pack . groom) ns ++ map (Text.pack . groom) es
      case pushKafka c of
        Just file ->
          do stmts <- readCDMStatements file
             let ms = map (TopicAndMessage "ta2" . makeMessage) stmts
             runKafka (mkKafkaState "adapt-trint-ta1-from-file" ("localhost", 9092))
                      (produceMessages ms)
             return ()
        Nothing -> return ()
      case upload c of
        Just r ->
          do
            vses <- doUpload c res r
            runKafka (mkKafkaState "adapt-trint-db-nodes" ("localhost", 9092))
                     (pushDataToKafka vses)
            return ()
        Nothing ->
          return ()
 where
  dbg s = when (verbose c) (putStrLn s)
  output f t = handle (onError f) $ Text.writeFile f t
  onError :: String -> IOException -> IO ()
  onError f e = do putStrLn ("Error writing " ++ f ++ ":")
                   print e

readCDMStatements :: FilePath -> IO [BS.ByteString]
readCDMStatements fp =
 do cont <- ByteString.readFile fp
    return (map ByteString.toStrict $ segment cont)
 where
 segment bs =
  let offsets = G.runGet (Avro.getArrayBytes (Proxy :: Proxy CDM.TCCDMDatum)) bs
      acc     = scanl' (+) 0 offsets
  in [ByteString.take o (ByteString.drop a bs) | o <- offsets | a <- acc]

pushDataToKafka :: VertsAndEdges -> Kafka ()
pushDataToKafka (vs, es) = do produceMessages $ map convertResultId rids
                              return ()
  where convertResultId :: ResultId -> TopicAndMessage
        convertResultId rid = TopicAndMessage "pattern" (ridToMessage rid)
        rids = catMaybes $ Map.elems vs ++ Map.elems es
        ridToMessage (ResultId r) = makeMessage (T.encodeUtf8 r)

printWarnings :: [Warning] -> IO ()
printWarnings ws = Text.putStrLn doc
  where doc = Text.unlines $ intersperse "\n" $ map (Text.pack . show . unW) ws
        unW (Warn w) = w

--------------------------------------------------------------------------------
--  Database Upload

removeBadEdges :: ([Node],[Edge]) -> ([Node],[Edge])
removeBadEdges (ns,es) =
  let exists x = x `Set.member` is
      is = Set.fromList (map nodeUID ns)
  in (ns, filter (\e -> exists (edgeSource e) && exists (edgeDestination e)) es)

-- We should probably do this in a transaction or something to tell when vertex insertion errors out
-- For now, we're just assuming it's always successful
doUpload :: Config -> ([Node],[Edge]) -> ServerInfo -> IO VertsAndEdges
doUpload c work svr =
  do res <- titan svr =<< compile (removeBadEdges work)
     case filter isFailure res of
      Failure _ r:_ -> Text.putStrLn ("Upload Error: " <> Text.decodeUtf8 r)
      _             -> return ()
     return (Map.empty,Map.empty) -- XXX

--------------------------------------------------------------------------------
--  Statistics

printStats :: ([Node],[Edge]) -> IO ()
printStats ss@(vs,es) =
  do let g  = mkGraph ss
         vs = vertices g
         mn = min 1 nrVert
         sz = maximum (mn : map (length . reachable g) vs)
         nrVert = length vs
         nrEdge = length es
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show nrVert
     putStrLn $ "\tEdges: " ++ show nrEdge


--------------------------------------------------------------------------------
--  Graphing

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: ([Node],[Edge]) -> Graph
mkGraph (ns,es) =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge es
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Edge -> State (Map UID Vertex,Vertex) (Maybe (Vertex,Vertex))
mkEdge (Edge s o _) =
  do nS <- nodeOf s
     nO <- nodeOf o
     return $ Just (nS,nO)

-- Memoizing node numbering
nodeOf :: UID -> State (Map UID Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n

