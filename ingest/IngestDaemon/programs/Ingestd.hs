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
import           Control.Concurrent (threadDelay)
-- import           Control.Exception as X
import           Lens.Micro
import           Lens.Micro.TH
import qualified Data.ByteString.Lazy as BL
import qualified Data.ByteString as B
import qualified System.IO as SIO
-- import           Data.Int (Int64)
import qualified Data.List as L
-- import           Data.Monoid ((<>))
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
-- import           Network.Kafka.Producer
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
 do let -- srvInt   = cfg ^. kafkaInternal
        srvExt   = cfg ^. kafkaExternal
        inTopics = cfg ^. inputTopics
        -- outTopic = cfg ^. outputTopic
        -- triTopic = cfg ^. triggerTopic
        -- logMsg   = void . atomically . TB.tryWriteTBChan logChan
        -- logTitan = logMsg . ("ingestd[Titan thread]:    " <>)
        -- logGC    = logMsg . ("ingestd[Retry thread]:    " <>)
        -- logIpt t = logMsg . (("ingestd[KafkaTopic:" <> kafkaString t <> "]") <>)
        logStderr = T.hPutStrLn stderr
        -- forkPersist name lg = void . forkIO . persistant name lg

    dbconn <- connectToTitan (cfg ^. titanServer)
    let [it] = inTopics -- XXX
    inputSchema <- CDM.getAvroSchema
    _ <- kafkaInputToDB srvExt it inputSchema (runDB logStderr dbconn)
    return ()

-- | Acquire CDM from a given kafka host/topic and place values a channel.
kafkaInputToDB :: KafkaAddress
           -> TopicName
           -> Avro.Schema
           -> ([Input] -> IO ())
           -> IO (Either KafkaClientError ())
kafkaInputToDB host topic cdmSchema handleTranslatedData =
  do r <- runKafka state oper
     return r
 where
 state = mkKafkaState "adapt-ingest" host
 oper  = do
   off  <- getLastOffset LatestTime 0 topic
   uids <- randomUIDs <$> liftIO newTFGen
   now <- liftIO getCurrentTime
   process now 0 uids off

 -- Read data, compile to our schema, and send to Gremlin
 process :: UTCTime -> Int -> [UID] -> Offset -> Kafka ()
 process start !cnt uids offset =
  do bs <- getMessage topic offset
     -- liftIO (SIO.hPutStrLn stderr (printf "Receiving %d kafka messages." (length bs)))
     let decodeMsg b =
          case Avro.decode cdmSchema (BL.fromStrict b) of
            Avro.Success cdmFmt -> return (Just cdmFmt)
            Avro.Error err      -> liftIO (SIO.hPutStrLn stderr (show err) >> return Nothing)
     ms   <- catMaybes <$> mapM decodeMsg bs
     let (ipts,newUIDs) = convertToSchemas uids ms []
     liftIO $ handleTranslatedData ipts
     if null bs
      then do liftIO (reportRate start cnt)
              liftIO (threadDelay 100000)
              now <- liftIO getCurrentTime
              process now 0 uids offset
      else process start (cnt+length bs) newUIDs (offset + fromIntegral (length bs))

reportRate :: UTCTime -> Int -> IO ()
reportRate start cnt
  | cnt < 10000 = return ()
  | otherwise =
 do now <- getCurrentTime
    let delta = realToFrac (diffUTCTime now start)
        rate = (fromIntegral cnt) / delta  :: Double
    liftIO $ T.hPutStrLn stderr (T.pack $ printf "Ingest complete at %f stmt/sec" rate)

convertToSchemas :: [UID] -> [CDM.TCCDMDatum] -> [[Input]] -> ([Input],[UID])
convertToSchemas uids [] acc = (concat acc,uids)
convertToSchemas uids (m:ms) acc =
   let (is,rest) = convertToSchema uids m
   in convertToSchemas rest ms (is:acc)

convertToSchema :: [UID] -> CDM.TCCDMDatum -> ([Input],[UID])
convertToSchema uids cdmFmt =
   let (ns,es) = CDM.toSchema [cdmFmt]
       esAndUID = zip es uids
       operations = compile (ns,esAndUID)
   in (map (\o -> Input cdmFmt o 0) operations, drop (length es) uids)

connectToTitan :: GC.ServerInfo -> IO GC.DBConnection
connectToTitan svr =
 do dbc <- GC.connect svr { maxOutstandingRequests = 64 }
    case dbc of
      Left _  -> error "Could not connect to Titan!" -- XXX retry
      Right c -> return c

--------------------------------------------------------------------------------
--  Titan Manager

runDB :: (Text -> IO ())
      -> DBConnection
      -> [Input]
      -> IO ()
runDB _ _ [] = return ()
runDB emit conn inputOps = do
  let (sendThese,waitThese) = L.splitAt 100 inputOps
  go sendThese
  runDB emit conn waitThese
 where
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
         recover _xs resp
            | respStatus resp /= 200 = return () -- XXX insertDB (respRequestId resp) xs fidb
            | otherwise              = return ()
     -- now <- getCurrentTime
     -- emit (T.pack $ printf "[%s] Sending %d nodes, %d edges" (show now) nrVS nrES)
     when (nrVS > 0) $ sendReq vsReq operVS
     when (nrES > 0) $ sendReq esReq operES

--------------------------------------------------------------------------------
--  Utils

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
