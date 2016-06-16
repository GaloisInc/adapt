{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE ParallelListComp  #-}
module Main where

import           Control.Exception
import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as BL
import           Data.Graph hiding (Node, Edge)
import           Data.List.Split (chunksOf)
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (catMaybes)
import           Data.Monoid ((<>))
import           Data.Proxy (Proxy(..))
import           Data.String
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import           MonadLib        hiding (handle)
import           MonadLib.Monads hiding (handle)
import           SimpleGetOpt
import           System.Exit (exitFailure)
import           System.FilePath ((<.>))
import           Text.Groom
import           Text.Printf
import           Text.Read (readMaybe)

import qualified CommonDataModel.Avro  as Avro
import qualified CommonDataModel       as CDM
import qualified CommonDataModel.Types as CDM

import           Schema

import Network.Kafka as Kafka
import Network.Kafka.Protocol as Kafka
import Network.Kafka.Producer as KProd

data Config = Config { lintOnly   :: Bool
                     , quiet      :: Bool
                     , stats      :: Bool
                     , ast        :: Bool
                     , verbose    :: Bool
                     , help       :: Bool
                     , pushKafka  :: Bool
                     , files      :: [File]
                     , finished   :: Bool
                     , ta1_to_ta2_kafkaTopic :: TopicName
                     , ingest_control_topic :: TopicName
                     } deriving (Show)

data File = CDMFile { getFP :: FilePath }
  deriving (Show)

defaultConfig :: Config
defaultConfig = Config
  { lintOnly  = False
  , quiet     = False
  , stats     = False
  , ast       = False
  , verbose   = False
  , help      = False
  , pushKafka = False
  , files = []
  , finished = False
  , ta1_to_ta2_kafkaTopic = "ta2"
  , ingest_control_topic = "in-finished"
  }

includeFile :: Config -> FilePath -> Either String Config
includeFile c fp = Right c { files = CDMFile fp : files c }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files (.prov*) and CDM files (all others) to be scanned.")]
               , progParams    = \p s -> includeFile s p
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
                    $ NoArg $ \s -> Right s { pushKafka = True }
                  , Option [] ["ta2-kafka-topic"]
                    "Set the kafka topic for the TA1-TA2 comms"
                    $ ReqArg "Topic" $
                        \tp s -> Right s { ta1_to_ta2_kafkaTopic = fromString tp }
                  , Option ['f'] ["finished"]
                    "Send the 'finished' signal to ingestd"
                    $ NoArg $ \s -> Right s { finished = True }
                  , Option [] ["ingest-control-topic"]
                    "Set the kafka topic for ingester control messages."
                    $ ReqArg "Topic" $
                        \tp s -> Right s { ingest_control_topic = fromString tp }
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
      else do mapM_ (handleFile c) (files c)
              when  (finished c)  (sendFinishedSignal c)

handleFile :: Config -> File -> IO ()
handleFile c fl = do
  eres <- ingest fl
  case eres of
    Left e  -> do
      putStrLn $ "Trint: Error ingesting file " ++ (getFP fl) ++ ":"
      putStrLn e
    Right ((ns,es),stmtBS) -> do
      processStmts c (getFP fl) (ns,es) stmtBS

  where
  ingest :: File -> IO (Either String (([Node],[Edge]), [BS.ByteString]))
  ingest (CDMFile fp)  =
    do t <- handle onError (BL.readFile fp)
       let prox = Proxy :: Proxy CDM.TCCDMDatum
           eBytes = Avro.decodeObjectContainerFor (Avro.getBytesOfObject prox) t
           eStmts = Avro.decodeObjectContainer t
       case (eStmts,eBytes) of
        (Left err,_)    -> return $ Left $ "Object container decode failure: " <> show err
        (_, Left err)   -> return $ Left $ "Impossible case: decode failure: " <> show err
        (Right (_,_,xs), Right (_,_,bs)) ->
           do let (ns,es) = CDM.toSchema (concat xs)
                  bytestringsOfStmts = BL.toStrict <$> concat bs
              return $ Right ((ns,es), bytestringsOfStmts)
  onError :: IOException -> IO a
  onError e = do putStrLn ("Error reading " ++ getFP fl ++ ":")
                 print e
                 exitFailure

processStmts :: Config -> FilePath -> ([Node],[Edge]) -> [BS.ByteString] -> IO ()
processStmts c fp res stmtBS
  | lintOnly c = return ()
  | otherwise = do
      when  (stats  c)    (printStats res)
      when  (ast c)       (handleAstGeneration c fp res)
      when  (pushKafka c) (handleKafkaIngest c stmtBS)

sendFinishedSignal :: Config -> IO ()
sendFinishedSignal c =
 do result <- sendStatus (ingest_control_topic c) Done
    either (calmly . show) (dbg . show) result
 where
  calmly s = unless (quiet c) (putStrLn s)
  dbg s    = when (verbose c) (putStrLn s)

data ProcessingStatus = Working | Done deriving (Enum)

sendStatus :: TopicName -> ProcessingStatus -> IO (Either KafkaClientError [ProduceResponse])
sendStatus topic stat =
  do let encStat = BS.pack [fromIntegral (fromEnum stat)]
         msg     = TopicAndMessage topic (makeMessage encStat)
     runKafka (mkKafkaState "adapt-trint" ("localhost", 9092))
              (produceMessages [msg])

handleAstGeneration :: Config -> FilePath -> ([Node],[Edge]) -> IO ()
handleAstGeneration c fp (ns,es) = do
  let astfile = fp <.> "trint"
  dbg ("Writing ast to " ++ astfile)
  output astfile $ Text.unlines $ map (Text.pack . groom) ns ++
                                  map (Text.pack . groom) es
 where
  dbg s    = when (verbose c) (putStrLn s)
  output f t = handle (onError f) (Text.writeFile f t)

  onError :: String -> IOException -> IO ()
  onError f e = do putStrLn ("Error writing " ++ f ++ ":")
                   print e

handleKafkaIngest :: Config -> [BS.ByteString] -> IO ()
handleKafkaIngest c stmts =
  do dbg $ printf "Obtained %d statements." (length stmts)
     let topic = ta1_to_ta2_kafkaTopic c
         ms    = map (TopicAndMessage topic . makeMessage) stmts
     _ <- runKafka (mkKafkaState "adapt-trint-ta1-from-file" ("localhost", 9092))
              (mapM_ produceMessages (chunksOf 128 ms))
     calmly $ printf "Sent %d statements to kafka[%s]." (length ms) (show topic)
     dbg    $ printf "\tBytes of CDM sent to kafka[ta2]: %d" (sum (map BS.length stmts))
 where
  calmly s = unless (quiet c) (putStrLn s)
  dbg s    = when (verbose c) (putStrLn s)

--------------------------------------------------------------------------------
--  Statistics

printStats :: ([Node],[Edge]) -> IO ()
printStats ss@(_,es) =
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
mkGraph (_,es) =
  let (newedges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge es
  in buildG (0,maxNodes) newedges

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

