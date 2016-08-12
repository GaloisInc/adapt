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

import           Control.Monad.IO.Class (liftIO)
import           Control.Concurrent (threadDelay, forkIO)
import qualified Control.Concurrent.Chan as Ch
import           Control.Exception as X
import           Lens.Micro
import           Lens.Micro.TH
import qualified Data.ByteString.Lazy as BL
import qualified Data.ByteString as B
import qualified System.IO as SIO
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
-- import           Network.Kafka as K
-- import           Network.Kafka.Protocol as K
import           Prelude
import           SimpleGetOpt
import           System.IO (stderr)
import           Text.Printf
import           Text.Read (readMaybe)
import           Network.Kafka.Consumer
import           Network.Kafka.Producer
import           Network.Kafka
import           Network.Kafka.Protocol

import           System.Random.TF
import           System.Random
import           Gremlin.Client as GC
import           CompileSchema
import           CommonDataModel as CDM
import           CommonDataModel.Types as CDM
import qualified Data.Avro as Avro
import qualified Data.Avro.Schema as Avro
import           IngestDaemon.Types
import Schema (UID)

data Config =
      Config { _logTopic      :: Maybe TopicName
             , _verbose       :: Bool
             , _help          :: Bool
             , _kafkaInternal :: KafkaAddress
             , _kafkaExternal :: KafkaAddress
             , _startingOffset :: Maybe Integer
             , _stopAfterCount :: Maybe Integer
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
  { _logTopic       = Nothing
  , _verbose        = False
  , _help           = False
  , _kafkaInternal  = defaultKafka
  , _kafkaExternal  = defaultKafka
  , _startingOffset = Nothing
  , _stopAfterCount = Nothing
  , _inputTopics    = []
  , _outputTopic    = "pe"
  , _triggerTopic   = "in-finished"
  , _titanServer    = GC.defaultServer
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
                        \str s -> Right $ s & inputTopics %~ (L.nub . (fromString str:))
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
                  , Option ['s'] ["starting-offset"]
                    "Start reading input from a given Kafka offset"
                    $ ReqArg "INT" $
                      \str s ->
                        case readMaybe str of
                          Just n  -> Right (s & startingOffset .~ n)
                          Nothing -> Left "Could not parse starting offset."
                  , Option ['c'] ["count"]
                    "Stop reading input after consuming a particular number of statements (each input topic will read this many statements!)."
                    $ ReqArg "INT" $
                      \str s ->
                        case readMaybe str of
                          Just n  -> Right (s & stopAfterCount .~ n)
                          Nothing -> Left "Could not parse starting offset."
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
      else neverFail (mainLoop c)
 where
 neverFail op =
   do X.catch op (\(e::X.SomeException) -> T.hPutStrLn stderr $ "Retrying because: " <> T.pack (show e))
      neverFail op

mainLoop :: Config -> IO ()
mainLoop cfg =
 do logChan <- Ch.newChan
    let logMsg m = void (Ch.writeChan logChan (T.encodeUtf8 m))
    forkIOsafe "Logs2Kafka" (emitLogData cfg logChan)
    startTime <- getCurrentTime
    logMsg $ "Starting Ingestd at " <> T.pack (show startTime)
    dbconn <- connectToTitan logMsg (cfg ^. titanServer)
    logMsg "Connected to Titan."
    inputSchema <- CDM.getAvroSchema
    let sendToDB = runDB logMsg dbconn
        mkFromKafka :: TopicName -> FromKafkaSettings
        mkFromKafka t = FKS { sourceServer  = cfg ^. kafkaExternal
                            , sourceSchema  = inputSchema
                            , initialOffset = fromIntegral <$> cfg ^. startingOffset
                            , maximumCount  = cfg ^. stopAfterCount
                            , sendInputs    = liftIO . sendToDB
                            , logKafkaMsg   = logMsg
                            , sourceTopic   = t
                            }
        fks = map mkFromKafka (cfg ^. inputTopics)
    case fks of
      [] -> T.putStrLn "No input sources specified."
      fs ->
        do mapM_ (\f -> forkIOsafe "KafkaConsumer" (kafkaInputToDB f >> return ())) fs
           forwardFinishSignal (cfg ^. kafkaExternal)
                               (cfg ^. triggerTopic)
                               (cfg ^. kafkaInternal)
                               (cfg ^. outputTopic)

-- Thread that reads from finish input signal and propagates it to the output.
forwardFinishSignal :: KafkaAddress -> TopicName -- Input
                    -> KafkaAddress -> TopicName -- Output
                    -> IO ()
forwardFinishSignal inSvr inTopic outSvr outTopic=
  do ch <- Ch.newChan
     forkIOsafe "finishSignalReader" (readKafka "ingest-sig-consumer" inSvr inTopic (Ch.writeChan ch))
     writeKafka "ingest-sig-producer" outSvr outTopic (Ch.readChan ch)

-- Thread that pushes log message to dashboard or stderr.
emitLogData :: Config -> Ch.Chan B.ByteString -> IO ()
emitLogData cfg logChan =
  case cfg ^. logTopic of
    Just ltop -> writeKafka "ingest-log-producer"
                            (cfg ^. kafkaInternal)
                            ltop
                            (Ch.readChan logChan)
    Nothing   ->
      forever $ do
        m <- Ch.readChan logChan
        T.hPutStrLn stderr (T.decodeUtf8 m)

-- Information needed by each thread that pulls from a kafka topic of TA1
-- sources.
data FromKafkaSettings =
  FKS { sourceServer  :: KafkaAddress
      , sourceSchema  :: Avro.Schema
      , initialOffset :: Maybe Offset
      , maximumCount  :: Maybe Integer
      , sendInputs    :: [Input] -> Kafka ()
      , logKafkaMsg   :: Text -> IO ()
      , sourceTopic   :: TopicName
      }

-- | Acquire CDM from a given kafka host/topic, compile to Adapt Schema,
-- and send that data to the database.
--
-- Normal operation: one thread per input topic
kafkaInputToDB :: FromKafkaSettings
           -> IO (Either KafkaClientError ())
kafkaInputToDB cfg =
  do r <- runKafka state oper
     case r of
       Left err -> logKafkaMsg cfg ("Error reading kafka topic '"
                                    <> topicStr <> "':" <> T.pack (show err))
       Right () -> return ()
     kafkaInputToDB cfg
 where
 src = sourceTopic cfg
 topicStr = T.decodeUtf8 (_kString $ _tName src)
 state = mkKafkaState "adapt-ingest" (sourceServer cfg)
 oper  = do
   off  <- maybe (getLastOffset LatestTime 0 src) pure (initialOffset cfg)
   uids <- randomUIDs <$> liftIO newTFGen
   now  <- liftIO getCurrentTime
   liftIO $ logKafkaMsg cfg ("Connected to Kafka for reading topic " <> topicStr)
   process now 0 0 uids off

 -- Read data, compile to our schema, and send to Gremlin
 -- Bail after `maximumCount`, if non-Nothing.
 process :: UTCTime -> Int -> Integer -> [UID] -> Offset -> Kafka ()
 process start !cnt total uids offset
  | maybe False (total >=) (maximumCount cfg) = return ()
  | otherwise =
   do bs0 <- getMessage (sourceTopic cfg) offset
      let bs = maybe bs0 (\x -> take (fromIntegral $ x - total) bs0) (maximumCount cfg)
          nr = length bs
          newTotal = total + fromIntegral nr
          decodeMsg b =
           case Avro.decode (sourceSchema cfg) (BL.fromStrict b) of
             Avro.Success cdmFmt ->
               return (Just cdmFmt)
             Avro.Error err      ->
               liftIO (SIO.hPutStrLn stderr (show err) >> return Nothing)
      ms   <- catMaybes <$> mapM decodeMsg bs
      let (ipts,newUIDs) = convertToSchemas uids ms
      sendInputs cfg ipts
      if null bs
       then do now <- liftIO $ do reportRate (logKafkaMsg cfg) start cnt
                                  threadDelay 100000
                                  getCurrentTime
               process now 0 total uids offset
       else process start (cnt+nr) newTotal newUIDs (offset + fromIntegral nr)

reportRate :: (Text -> IO ()) -> UTCTime -> Int -> IO ()
reportRate sendOutput start cnt
  | cnt < 10000 = return ()
  | otherwise =
 do now <- getCurrentTime
    let delta = realToFrac (diffUTCTime now start)
        rate = (fromIntegral cnt) / delta  :: Double
    liftIO $ sendOutput (T.pack $ printf "Ingest complete at %f stmt/sec" rate)

convertToSchemas :: [UID] -> [CDM.TCCDMDatum] -> ([Input],[UID])
convertToSchemas us xs = go us xs []
 where
  go uids [] acc = (concat acc,uids)
  go uids (m:ms) acc =
   let (is,rest) = convertToSchema uids m
   in go rest ms (is:acc)

convertToSchema :: [UID] -> CDM.TCCDMDatum -> ([Input],[UID])
convertToSchema uids cdmFmt =
   let (ns,es) = CDM.toSchema [cdmFmt]
       esAndUID = zip es uids
       operations = compile (ns,esAndUID)
   in (map (\o -> Input cdmFmt o 0) operations, drop (length es) uids)

connectToTitan :: (Text -> IO()) -> GC.ServerInfo -> IO GC.DBConnection
connectToTitan logMsg svr =
 do dbc <- GC.connect svr { maxOutstandingRequests = 64 }
    case dbc of
      Left _  -> do logMsg "Could not connect to Titan."
                    threadDelay 1000000
                    connectToTitan logMsg svr
      Right c -> return c

--------------------------------------------------------------------------------
--  Titan Code

-- Sends all inputs to the database in batches.  If an insertion fails then retry 
-- with a smaller batch size, dropping the statement entirely if it fails
-- `deadAt` times.
runDB :: (Text -> IO ())
      -> DBConnection
      -> [Input]
      -> IO ()
runDB _ _ [] = return ()
runDB emit conn inputOps = do
  let (sendThese,waitThese) = L.splitAt nrBulk inputOps
  go sendThese
  runDB emit conn waitThese
 where
 nrBulk = 100
 deadAt = 2 -- retry age zero and one inputs, drop on third failure.
 go oprs =
  do let (operVS,operES) = L.partition (isVertex . statement) oprs
         (vs,es)   = (map statement operVS, map statement operES)
         nrVS      = length vs
         nrES      = length es
         vsCmdEnv  = serializeOperations vs
         esCmdEnv  = serializeOperations es
         vsReq     = uncurry mkRequest vsCmdEnv
         esReq     = uncurry mkRequest esCmdEnv
         sendReq x o = GC.sendOn conn x (recover o)
         recover :: [Input] -> Response -> IO ()
         recover [] _ = error "BUG: failed insertion on an empty batch."
         recover xs@(x:_) resp
            | respStatus resp /= 200 =
                -- Retry by simply calling runDB on the individual
                -- inputs instead of the bulk set of inputs.
                let live = filter ((<deadAt) . inputAge) (map ageInput xs)
                    batches
                      | inputAge x < (deadAt-1)  =
                          let (a,b) = L.splitAt (length xs `div` 2) live
                          in [a,b]
                      | otherwise      = map (\y -> [y]) live
                in void $ forkIO (mapM_ (runDB emit conn) batches)
                   -- ^^^ XXX consider a channel and long-lived thread here
            | otherwise              = return ()
     when (nrVS > 0) $ sendReq vsReq operVS
     when (nrES > 0) $ sendReq esReq operES

--------------------------------------------------------------------------------
--  Utils

ageInput :: Input -> Input
ageInput i = i { inputAge = inputAge i + 1 }

isVertex :: Operation a -> Bool
isVertex (InsertVertex {}) = True
isVertex _                  = False

kafkaString :: TopicName -> Text
kafkaString (TName (KString s)) = T.decodeUtf8 s

randomUIDs :: TFGen -> [UID]
randomUIDs g =
  let (u,g2) = randomUID g
  in u : randomUIDs g2

randomUID :: TFGen -> (UID,TFGen)
randomUID g1 =
  let (w1,g2) = next g1
      (w2,g3) = next g2
      (w3,g4) = next g3
      (w4,g5) = next g4
  in ((fromIntegral w1,fromIntegral w2,fromIntegral w3,fromIntegral w4),g5)

getMessage :: TopicName -> Offset -> Kafka [B.ByteString]
getMessage topicNm offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topicNm)

-- Helper routine for reading from kafka topics
readKafka :: KafkaClientId
          -> KafkaAddress -> TopicName
          -> (B.ByteString -> IO ())
          -> IO ()
readKafka name svr topic putMsg =
 do _ <- runKafka (mkKafkaState name svr) (withLastOffset (op baseDelay)  topic)
    return ()
  where
  baseDelay = 50000
  maxDelay  = 1000000
  op delayTime off =
    do bs <- getMessage topic off
       if null bs
          then do liftIO $ threadDelay delayTime
                  let newDelay = min (delayTime * 2) maxDelay
                  op newDelay off
          else do liftIO (mapM_ putMsg bs)
                  op baseDelay (off + fromIntegral (length bs))

-- Helper routine for writing to kafka topics
writeKafka :: KafkaClientId
          -> KafkaAddress -> TopicName
          -> IO B.ByteString
          -> IO ()
writeKafka name srv topic getMsg =
 do _ <- runKafka (mkKafkaState name srv) (forever op)
    return ()
  where
  op =
    do m <- liftIO getMsg
       produceMessages [TopicAndMessage topic $ makeMessage m]

withLastOffset :: (Offset -> Kafka a) -> TopicName -> Kafka a
withLastOffset op topic =
    do off <- getLastOffset LatestTime 0 topic
       op off

forkIOsafe :: Text -> IO () -> IO ()
forkIOsafe threadName op = forkIO go >> return ()
 where
 go =
  do X.catch op (\(e :: X.SomeException) -> T.hPutStrLn stderr $ "Thread '" <> threadName <> "' failed because: " <> T.pack (show e))
     go
