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
import qualified Data.List as L
import           Data.Monoid ((<>))
import           Data.Maybe (catMaybes)
import           Data.String
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.IO as T
import           Data.Time (UTCTime, diffUTCTime, getCurrentTime)
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
import           CommonDataModel as CDM
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
inputQueueSize  = 100000

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
                                         return (ServerInfo h p 512)
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
        logGC    = logMsg . ("ingestd[Retry thread]:    " <>)
        logIpt t = logMsg . (("ingestd[KafkaTopic:" <> kafkaString t <> "]") <>)
        logStderr = T.hPutStrLn stderr
        forkPersist name lg = void . forkIO . persistant name lg
    forkPersist "Finisher" logMsg $
      finishIngestSignal (finisher reqStatus inputs) srvInt outTopic triTopic
    _ <- maybe (forkPersist "StderrLogger" logStderr
                       (Left <$> channelToStderr logChan :: IO (Either () ())))
               (forkPersist "KafkaLogger" logStderr . channelToKafka  logChan srvInt)
               (cfg ^. logTopic)
    inputSchema <- CDM.getAvroSchema
    mapM_ (\t -> forkPersist "TA1-input" (logIpt t) $ kafkaInput (logIpt t) srvExt t inputSchema inputs) inTopics
    forkPersist "GC" logMsg (garbageCollectFailures logGC reqStatus inputs)
    now <- getCurrentTime
    logIpt "startup" ("System up at: " <> T.pack (show now))
    persistant "TitanManager" logTitan (titanManager cfg logTitan (cfg ^. titanServer) inputs reqStatus)
  where
  maxAge = 0 -- 1 retry at age 0, dead at age 1
  garbageCollectFailures :: (Text -> IO ()) -> FailedInsertionDB -> TBChan Input -> IO (Either () ())
  garbageCollectFailures _logGC fidb inputs = forever $ do
       threadDelay 10000000 -- 10 seconds
       es <- resetDB fidb
       let ageOperations :: OperationRecord -> IO [Maybe Input]
           ageOperations (OpRecord ipts _) = mapM ageOperation ipts
           ageOperation ipt@(Input {..})
              | inputAge > maxAge   = return Nothing
                 -- XXX ^^^ dropping the statement, increment a statistic counter?
              | otherwise = return $ Just ipt { inputAge = inputAge + 1 }
       T.hPutStrLn stderr $ T.pack $ printf "GC collected %d elements" (length es)
       is <- catMaybes . concat <$> mapM ageOperations es
       mapM_ (atomically . TB.writeTBChan inputs) is

  finisher :: FailedInsertionDB -> TBChan Input -> IO ()
  finisher fidb inputs =
   do es <- resetDB fidb
      let msg = T.pack $ printf "Re-trying %d insertions before signaling PE." (length es)
      T.hPutStrLn stderr msg
      let newOperations = concatMap updateOperation es
      mapM_ (atomically . TB.writeTBChan inputs) newOperations
      delayWhileNonEmpty inputs

  -- Given a failed operation and an HTTP error code from Titan,
  -- build a new operation suitable for re-trying (or not) the operation.
  updateOperation :: OperationRecord -> [Input]
  updateOperation (OpRecord ipts code)
              | code == Just 597 = map genVerts ipts
              | otherwise        = ipts
  genVerts ipt@(Input orig stmt cnt) =
    case stmt of
      InsertEdge {}        -> Input orig stmt { generateVertices = True } cnt
      InsertReifiedEdge {} -> ipt
      InsertVertex {}      -> ipt

persistant :: (Show a, Show e) => Text -> (Text -> IO ()) -> IO (Either e a) -> IO ()
persistant name logMsg io =
  do ex <- X.catch (Right <$> io) (pure . Left)
     ( do T.hPutStrLn stderr ("Thread " <> name <> " had an exception: " <> T.pack (show ex))
          case ex of
           Right (Right r) ->
             do logMsg ("Operation completed: " <> T.pack (show r))
           Right (Left e) ->
             do logMsg ("Operation failed (retry in 5s): " <> T.pack (show e))
                threadDelay 5000000
           Left (e::SomeException) ->
             do logMsg ("Operation had an exception (retry in 5s): " <> T.pack (show e))
                threadDelay 5000000
         ) `X.finally` persistant name logMsg io


--------------------------------------------------------------------------------
--  Titan Manager

titanManager :: Config
             -> (Text -> IO ())
             -> GC.ServerInfo
             -> TBChan Input
             -> FailedInsertionDB
             -> IO (Either ConnectionException ())
titanManager _cfg logTitan svr inputs fidb =
 do logTitan "Connecting to titan..."
    dbc <- GC.connect svr
    case dbc of
      Left e     -> return (Left e)
      Right conn ->
        do runDB logTitan inputs fidb conn
           return (Right ())

runDB :: (Text -> IO ())
      -> TBChan Input
      -> FailedInsertionDB
      -> DBConnection
      -> IO ()
runDB logTitan inputs fidb conn =
  do logTitan "Connected to titan."
     now <- getCurrentTime
     go now reportInterval (0,0)
 where
 reportInterval = 20000
 nrInBulk       = 100 --  Number of commands to handle in bulk

 go :: UTCTime -> Int -> (Int64,Int64) -> IO ()
 go prev ri !cnts@(!nrE,!nrV)
   | ri <= 0 =
      do now <- getCurrentTime
         let rate :: Double
             rate = fromIntegral reportInterval / realToFrac (diffUTCTime now prev)
         logTitan (T.pack $ printf "Sent commands for: %d edges triples, %d verticies. %f statements per second" nrE nrV rate)
         go now reportInterval cnts
   | otherwise =
  do let readCh ch = catMaybes <$> replicateM nrInBulk (TB.tryReadTBChan ch)
     oprsAndRetries <- atomically (readCh inputs)
     if null oprsAndRetries
      then threadDelay 100000 >> go prev ri cnts
      else do let (oprs,operRS) = L.partition (\x -> inputAge x == 0) oprsAndRetries
                  (operVS,operES) = L.partition (isVertex . statement) oprs
                  (vs,es)   = (map statement operVS, map statement operES)
                  rs        = map statement operRS
                  nrVS      = length vs
                  nrES      = length es
                  nrRS      = length rs
                  vsCmdEnv  = serializeOperations vs
                  esCmdEnv  = serializeOperations es
                  rsCmdEnvs = map serializeOperation rs
                  vsReq     = uncurry mkRequest vsCmdEnv
                  esReq     = uncurry mkRequest esCmdEnv
                  rsReqs    = map (uncurry mkRequest) rsCmdEnvs
                  sendReq x o = GC.sendOn conn x (recover o)
                  recover :: [Input] -> Response -> IO ()
                  recover xs resp
                     | respStatus resp /= 200 = insertDB (respRequestId resp) xs fidb
                     | otherwise              = return ()
              when (nrVS > 0) $ sendReq vsReq operVS
              when (nrES > 0) $ sendReq esReq operES
              when (nrRS > 0) $ mapM_ (\(r,o) -> sendReq r [o]) (zip rsReqs operRS)
              let (nrE2,nrV2) = (nrE + fromIntegral nrES,nrV + fromIntegral nrVS)
              go prev (ri - nrES - nrVS) (nrE2,nrV2)

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
