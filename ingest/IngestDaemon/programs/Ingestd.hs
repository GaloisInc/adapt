{-# LANGUAGE OverloadedStrings   #-}
{-# LANGUAGE RecordWildCards     #-}
{-# LANGUAGE ViewPatterns        #-}
{-# LANGUAGE BangPatterns        #-}
{-# LANGUAGE TupleSections       #-}
{-# LANGUAGE FlexibleInstances   #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE TemplateHaskell     #-}
-- | Ingestd is the ingest daemon which takes TA-1 data from Kafka queue,
-- decodes it as Avro-encodedCDM, translates to Adapt Schema, uploads to
-- Titan, and pushes node IDs to PX via Kafka.
module Main where

import           Control.Applicative ((<$>))
import           Control.Concurrent
import           Control.Concurrent (threadDelay)
import           Control.Concurrent.BoundedChan as BC
import           Control.Exception as X
import           Control.Lens
import           Control.Monad (when, forever, void)
import           Data.Binary (encode,decode)
import qualified Data.ByteString as BS
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Lazy as ByteString
import qualified Data.Foldable as F
import           Data.Graph hiding (Node, Edge)
import           Data.Int (Int32)
import           Data.Int (Int64)
import           Data.List (partition,intersperse)
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (maybeToList)
import           Data.Monoid ((<>))
import qualified Data.Set as Set
import           Data.String
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.IO as T
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.Encoding as Text
import qualified Data.Text.Lazy.IO as Text
import           Data.Time (UTCTime, addUTCTime, getCurrentTime)
import           MonadLib        hiding (handle)
import           MonadLib.Monads hiding (handle)
import           Network.Kafka as K
import           Network.Kafka.Protocol as K
import           Numeric (showHex)
import           Prelude
import           SimpleGetOpt
import           System.Entropy (getEntropy)
import           System.Exit (exitFailure)
import           System.IO (stderr)
import           Text.Printf
import           Text.Read (readMaybe)

import           Titan
import           IngestDaemon.KafkaManager

data Config =
      Config { _logTopic      :: Maybe TopicName
             , _verbose       :: Bool
             , _help          :: Bool
             , _kafkaInternal :: KafkaAddress
             , _kafkaExternal :: KafkaAddress
             , _inputTopics   :: [TopicName]
             , _outputTopic   :: TopicName
             , _triggerTopic  :: TopicName
             , _titanServer   :: Titan.ServerInfo
             } deriving (Show)

makeLenses ''Config

inputQueueSize  :: Int
inputQueueSize  = 1000

outputQueueSize :: Int
outputQueueSize = 1000

defaultKafka :: KafkaAddress
defaultKafka = ("localhost", 9092)

defaultConfig :: Config
defaultConfig = Config
  { _logTopic      = Nothing
  , _verbose       = False
  , _help          = False
  , _kafkaInternal = defaultKafka
  , _kafkaExternal = defaultKafka
  , _inputTopics   = []
  , _outputTopic   = "px"
  , _triggerTopic  = "in-finished"
  , _titanServer   = Titan.defaultServer
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = []
               , progParams    = \_ s -> Left "No extra parameters are accepted."
               , progOptions   =
                  [ Option ['v'] ["verbose"]
                    "Verbose debugging messages"
                    $ NoArg $ \s -> Right $ s & verbose .~ True
                  , Option ['l'] ["logTopic"]
                    "Send statistics to the given Kafka topic"
                    $ ReqArg "Topic" $ \t s -> Right (s & logTopic .~ Just (fromString t))
                  , Option ['h'] ["help"]
                    "Print help"
                    $ NoArg $ \s -> Right $ s & help .~ True
                  , Option ['i'] ["inputTopic"]
                    "Set a TA-1 topic to be used as input"
                    $ ReqArg "Topic" $
                        \str s -> Right $ s & inputTopics %~ (fromString str:)
                  , Option ['o'] ["outputTopic"]
                    "Set a PX topic to be used as output"
                    $ ReqArg "Topic" $
                        \str s -> Right (s & outputTopic .~ fromString str)
                  , Option ['f'] ["finish-message-topic"]
                    "Topic on which the 'processing complete' signal should be received (on the internal Kafka server)"
                    $ ReqArg "Topic" $
                        \str s -> Right (s & triggerTopic .~ fromString str)
                  , Option ['t'] ["titan"]
                    "Set the titan server"
                    $ ReqArg "host:port" $
                        \str s ->
                            let svr = uncurry ServerInfo <$> (parseHostPort str)
                            in case svr of
                                Just res -> Right (s & titanServer .~ res)
                                Nothing  -> Left "Could not parse host:port string."
                  , Option ['k'] ["kafka-internal"]
                    "Set the kafka server (for logging and px topics)"
                    $ ReqArg "host:port" $
                        \str s ->
                            let svr = parseHostPort str
                            in case svr of
                                Just res -> Right (s & kafkaInternal .~ res)
                                Nothing  -> Left "Could not parse host:port string."
                  , Option ['e'] ["kafka-external"]
                    "Set the kafka external server (for -i topics)"
                    $ ReqArg "host:port" $
                        \str s ->
                            let svr = parseHostPort str
                            in case svr of
                                Just res -> Right (s & kafkaExternal .~ res)
                                Nothing  -> Left "Could not parse host:port string."
                  ]
               }

parseHostPort :: (IsString s, Num n) => String -> Maybe (s,n)
parseHostPort str =
  let (h,p) = break (== ':') str
  in case (h,readMaybe (drop 1 p)) of
      ("",_)         -> Nothing
      (_,Nothing)    -> Nothing
      (hst,Just pt) -> Just ( fromString (hst :: String)
                            , fromIntegral (pt::Int))

main :: IO ()
main =
  do c <- getOpts opts
     if c ^. help
      then dumpUsage opts
      else mainLoop c

mainLoop :: Config -> IO ()
mainLoop cfg =
 do inputs  <- newBoundedChan inputQueueSize   :: IO (BoundedChan (Operation Text))
    logChan <- newBoundedChan 100 :: IO (BoundedChan Text)
    let srvInt   = cfg ^. kafkaInternal
        srvExt   = cfg ^. kafkaExternal
        inTopics = cfg ^. inputTopics
        outTopic = cfg ^. outputTopic
        triTopic = cfg ^. triggerTopic
        logMsg   = void . BC.tryWriteChan logChan
        logPXMsg = logMsg . ("ingestd[PE-Kafka thread]: " <>)
        logTitan = logMsg . ("ingestd[Titan thread]:    " <>)
        logIpt t = logMsg . (("ingestd[from " <> kafkaString t <> "]") <>)
        logStderr = T.hPutStrLn stderr
        forkPersist log = void . forkIO . persistant log
    forkPersist logMsg $ finishIngestSignal srvInt outTopic triTopic
    _ <- maybe (forkPersist logStderr (Left <$> channelToStderr logChan :: IO (Either () ())))
               (forkPersist logStderr . channelToKafka  logChan srvInt)
               (cfg ^. logTopic)
    mapM_ (\t -> forkPersist (logIpt t) $ kafkaInput srvExt t inputs) inTopics
    now <- getCurrentTime
    logIpt "startup" ("System up at: " <> T.pack (show now))
    persistant logTitan $
     do logTitan "Connecting to titan..."
        Titan.withTitan (cfg ^. titanServer) resHdl (runDB logTitan inputs)
  where
  resHdl _ _ = return ()

persistant :: (Show a, Show e) => (Text -> IO ()) -> IO (Either e a) -> IO ()
persistant logMsg io =
  do ex <- X.catch (Right <$> io) (pure . Left)
     case ex of
      Right (Right r) ->
        do logMsg ("Operation completed: " <> T.pack (show r))
           persistant logMsg io
      Right (Left e) ->
        do logMsg ("Operation failed (retry in 5s): " <> T.pack (show e))
           threadDelay 5000000
           persistant logMsg io
      Left (e::SomeException) ->
        do logMsg ("Operation had an exception (retry in 5s): " <> T.pack (show e))
           threadDelay 5000000
           persistant logMsg io

runDB :: (Text -> IO ())
      -> (BoundedChan Statement)
      -> Titan.Connection
      -> IO ()
runDB logTitan inputs conn =
  do logTitan "Connected to titan."
     go commitInterval reportInterval (0,0)
 where
 commitInterval = 1
 reportInterval = 1000

 go :: Int -> Int -> (Int64,Int64) -> IO ()
 go ci 0 !cnts@(!nrE,!nrV) =
  do logTitan (T.pack $ printf "Ingested %d edges, %d verticies." nrE nrV)
     go ci reportInterval cnts
 go 0 ri !cnts@(!nrE,!nrV) =
  do Titan.commit conn
     threadDelay 10000 -- XXX Locking exceptions in titan without a delay!
     go commitInterval ri cnts
 go ci ri !(!nrE,!nrV) =
  do op <- BC.readChan inputs
     Titan.send op conn
     let (nrE2,nrV2) = if isVertex op then (nrE,nrV+1) else (nrE+1,nrV)
     go (ci - 1) (ri - 1) (nrE2,nrV2)

isVertex :: Operation a -> Bool
isVertex (InsertVertex _ _) = True
isVertex _                  = False

channelToStderr :: BC.BoundedChan Text -> IO ()
channelToStderr ch = forever (BC.readChan ch >>= T.hPutStrLn stderr)

kafkaString :: TopicName -> Text
kafkaString (TName (KString s)) = T.decodeUtf8 s
