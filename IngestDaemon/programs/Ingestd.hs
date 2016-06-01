{-# LANGUAGE OverloadedStrings   #-}
{-# LANGUAGE RecordWildCards     #-}
{-# LANGUAGE ViewPatterns        #-}
{-# LANGUAGE BangPatterns        #-}
{-# LANGUAGE TupleSections       #-}
{-# LANGUAGE FlexibleInstances   #-}
{-# LANGUAGE FlexibleContexts    #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE ParallelListComp    #-}
{-# LANGUAGE TemplateHaskell     #-}
-- | Ingestd is the ingest daemon which takes TA-1 data from Kafka queue,
-- decodes it as Avro-encodedCDM, translates to Adapt Schema, uploads to
-- Titan, and pushes node IDs to PE via Kafka.
module Main where

import           Control.Concurrent (forkIO)
import           Control.Concurrent (threadDelay)
import           Control.Concurrent.STM.TBChan as TB
import           Control.Concurrent.STM (atomically)
import           Control.Exception as X
import           Lens.Micro
import           Lens.Micro.TH
import           Data.Int (Int64)
import           Data.Monoid ((<>))
import           Data.Maybe (catMaybes)
import           Data.String
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.IO as T
import           Data.Time (getCurrentTime)
import           MonadLib        hiding (handle)
import           Network.Kafka as K
import           Network.Kafka.Protocol as K
import           Prelude
import           SimpleGetOpt
import           System.IO (stderr)
import           Text.Printf
import           Text.Read (readMaybe)

import           Gremlin.Client as GC
import           CompileSchema
import           IngestDaemon.KafkaManager
import           IngestDaemon.Types

data Config =
      Config { _logTopic      :: Maybe TopicName
             , _verbose       :: Bool
             , _help          :: Bool
             , _kafkaInternal :: KafkaAddress
             , _kafkaExternal :: KafkaAddress
             , _inputTopics   :: [TopicName]
             , _outputTopic   :: TopicName
             , _triggerTopic  :: TopicName
             , _titanServer   :: GC.ServerInfo
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
  , _outputTopic   = "pe"
  , _triggerTopic  = "in-finished"
  , _titanServer   = GC.defaultServer
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = []
               , progParams    = \_ _ -> Left "No extra parameters are accepted."
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
                    "Set a PE topic to be used as output"
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
                            let svr = do (h,p) <- parseHostPort str
                                         return (ServerInfo h p 128)
                            in case svr of
                                Just res -> Right (s & titanServer .~ res)
                                Nothing  -> Left "Could not parse host:port string."
                  , Option ['k'] ["kafka-internal"]
                    "Set the kafka server (for logging and pe topics)"
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

-- The main loop of ingestd is admittedly a little hairy.  The tasks are:
--
--  * Fork off threads to receive from Kafka
--    * Inputs channel: decode the input from ta1, put that data on a more
--                      convenient channel. Inputs might be from an
--                      internal Kafka server (on this same machine) or
--                      external/ta3 machine.
--    * Trigger: The trigger is a one-byte message from the user to
--               indicate all the data has been ingested into the database.
--               Once received, the 'finisher' checks the list of failed
--               operations, retry these operations, then returns to allow
--               signaling of PE via the Kafka 'outTopic'.
--    * Logs: Much of the logging is shunted, via Kafka 'in-log' topic,
--            to the dashboard.  In adapt-in-a-box, stderr is sent to a
--            log file (/tmp/ingestd-stderr*).
--    * reqStatus: This is the mutable map of UUID->operations that have
--                 not received a success message.
mainLoop :: Config -> IO ()
mainLoop cfg =
 do inputs  <- newTBChanIO inputQueueSize
    logChan <- newTBChanIO 100
    reqStatus <- newDB
    let srvInt   = cfg ^. kafkaInternal
        srvExt   = cfg ^. kafkaExternal
        inTopics = cfg ^. inputTopics
        outTopic = cfg ^. outputTopic
        triTopic = cfg ^. triggerTopic
        logMsg   = void . atomically . TB.tryWriteTBChan logChan
        logTitan = logMsg . ("ingestd[Titan thread]:    " <>)
        logIpt t = logMsg . (("ingestd[from " <> kafkaString t <> "]") <>)
        logStderr = T.hPutStrLn stderr
        forkPersist lg = void . forkIO . persistant lg
    forkPersist logMsg $
      finishIngestSignal (finisher reqStatus inputs) srvInt outTopic triTopic
    _ <- maybe (forkPersist logStderr
                       (Left <$> channelToStderr logChan :: IO (Either () ())))
               (forkPersist logStderr . channelToKafka  logChan srvInt)
               (cfg ^. logTopic)
    mapM_ (\t -> forkPersist (logIpt t) $ kafkaInput srvExt t inputs) inTopics
    now <- getCurrentTime
    logIpt "startup" ("System up at: " <> T.pack (show now))
    persistant logTitan (titanManager cfg logTitan (cfg ^. titanServer) inputs reqStatus)
  where
  finisher :: FailedInsertionDB -> TBChan Input -> IO ()
  finisher fidb inputs =
   do es <- resetDB fidb
      let msg = T.pack $ printf "Re-trying %d insertions before signaling PE." (length es)
          newOperations = catMaybes (map updateOperation es)
      T.hPutStrLn stderr msg
      mapM_ (atomically . TB.writeTBChan inputs) newOperations
      delayWhileNonEmpty inputs

  -- Given a failed operation and an HTTP error code from Titan,
  -- build a new operation suitable for re-trying (or not) the operation.
  updateOperation :: OperationRecord -> Maybe Input
  updateOperation (OpRecord ipt@(Input orig stmt) code) =
    Just $ case stmt of
            InsertEdge {}
              | code == Just 597 -> Input orig stmt { generateVertices = True }
              | otherwise        -> ipt
            InsertVertex {}      -> ipt


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


--------------------------------------------------------------------------------
--  Titan Manager

titanManager :: Config
             -> (Text -> IO ())
             -> GC.ServerInfo
             -> TBChan Input
             -> FailedInsertionDB
             -> IO (Either ConnectionException ())
titanManager _cfg logTitan svr inputs reqStatus =
 do logTitan "Connecting to titan..."
    dbc <- GC.connect svr
    case dbc of
      Left e   -> return (Left e)
      Right db -> runDB logTitan inputs reqStatus db >> return (Right ())

runDB :: (Text -> IO ())
      -> TBChan Input
      -> FailedInsertionDB
      -> DBConnection
      -> IO ()
runDB logTitan inputs db conn =
  do logTitan "Connected to titan."
     go reportInterval (0,0)
 where
 reportInterval = 1000

 go :: Int -> (Int64,Int64) -> IO ()
 go 0 !cnts@(!nrE,!nrV) =
  do logTitan (T.pack $ printf "Ingested %d edges, %d verticies." nrE nrV)
     go reportInterval cnts
 go ri !(!nrE,!nrV) =
  do opr <- atomically (TB.readTBChan inputs)
     let (cmd,env) = serializeOperation stmt
         stmt = statement opr
         req  = mkRequest cmd env
     GC.sendOn conn req $ \resp ->
             when (respStatus resp /= 200) (insertDB (respRequestId resp) opr db)
     let (nrE2,nrV2) = if isVertex stmt then (nrE,nrV+1) else (nrE+1,nrV)
     go (ri - 1) (nrE2,nrV2)

--------------------------------------------------------------------------------
--  Utils

isVertex :: Operation a -> Bool
isVertex (InsertVertex {}) = True
isVertex _                  = False

channelToStderr :: TBChan Text -> IO ()
channelToStderr ch = forever $ atomically (TB.readTBChan ch) >>= T.hPutStrLn stderr

kafkaString :: TopicName -> Text
kafkaString (TName (KString s)) = T.decodeUtf8 s

delayWhileNonEmpty :: TBChan a -> IO ()
delayWhileNonEmpty ch =
  do b <- atomically (isEmptyTBChan ch)
     if b then return ()
          else threadDelay 100000 >> delayWhileNonEmpty ch
