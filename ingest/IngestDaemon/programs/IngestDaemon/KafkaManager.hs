{-# LANGUAGE OverloadedStrings #-}
module IngestDaemon.KafkaManager where

import           Control.Concurrent
import           Control.Concurrent.STM.TBChan as TB
import           Control.Concurrent.STM (atomically)
import           Control.Monad (forever)
import           Control.Monad.IO.Class (liftIO)
import           Data.ByteString (ByteString)
import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as BL
import           Data.Text (Text)
import qualified Data.Text.Encoding as T
import           System.IO (hPutStrLn, stderr)

import           Network.Kafka
import           Network.Kafka.Producer
import           Network.Kafka.Consumer
import           Network.Kafka.Protocol as Kafka

import           CompileSchema
import           CommonDataModel as CDM
import qualified Data.Avro as Avro
import qualified Data.Avro.Schema as Avro
import           IngestDaemon.Types

-- Straight text to a Kafka topic. Used for logging.
channelToKafka :: TBChan Text -> KafkaAddress -> TopicName -> IO (Either KafkaClientError ())
channelToKafka ch host topic =
 do r <- runKafka state oper
    case r of
      Left err -> hPutStrLn stderr ("Kafka logging failed: " ++ show err)
      Right () -> hPutStrLn stderr ("Kafka logging terminated somehow.")
    return r
 where
 state = mkKafkaState "ingest-logging" host
 oper = forever $ do
   m <- liftIO (atomically $ TB.readTBChan ch)
   produceMessages [TopicAndMessage topic $ makeMessage (T.encodeUtf8 m)]

--------------------------------------------------------------------------------
-- PE Signalling

-- We don't place node IDs on the queue for the first engagement, just
-- a signal indicating DB is ready.
finishIngestSignal :: IO () -> KafkaAddress -> TopicName -> TopicName -> IO (Either KafkaClientError ())
finishIngestSignal finisher svr out ipt =
 do r <- runKafka state oper
    case r of
      Left err -> hPutStrLn stderr ("Kafka signaling failed: " ++ show err)
      Right () -> hPutStrLn stderr ("Kafka signaling terminated somehow.")
    return r
 where
 state = mkKafkaState "ingest-pe" svr
 oper =
   do o <- getLastOffset LatestTime 0 ipt
      forever (process o)
 process o = do
  do bs <- getMessage ipt o
     mapM_ propogateSignal bs
     if null bs
      then liftIO (threadDelay 100000) >> process o
      else process (o + fromIntegral (length bs))
 propogateSignal b
  | BS.length b == 1 =
     do emit ("Received control signal: " ++ show (BS.unpack b))
        emit "Calling the finisher to clean up."
        liftIO finisher
        emit ("Propogating control signal: " ++ show (BS.unpack b))
        _ <- produceMessages [TopicAndMessage out $ makeMessage b]
        return ()
  | otherwise =
      emit ("Invalid control signal: " ++ show b)

getMessage :: TopicName -> Offset -> Kafka [ByteString]
getMessage topicNm offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topicNm)

--------------------------------------------------------------------------------
--  Getting the CDM input from TA1

-- | Acquire CDM from a given kafka host/topic and place values a channel.
kafkaInput :: (Text -> IO ())
           -> KafkaAddress
           -> TopicName
           -> Avro.Schema
           -> TBChan Input
           -> IO (Either KafkaClientError ())
kafkaInput logK host topic cdmSchema chan =
  do r <- runKafka state oper
     return r
 where
 state = mkKafkaState "adapt-ingest" host
 oper  = do liftIO (logK "Connected.")
            forever $ getLastOffset LatestTime 0 topic >>= process

 process :: Offset -> Kafka ()
 process offset =
  do bs <- getMessage topic offset
     let handleMsg b =
          case Avro.decode cdmSchema (BL.fromStrict b) of
            Avro.Success cdmFmt -> insertCDM cdmFmt
            Avro.Error err      -> emit (show err)
     mapM_ handleMsg bs
     if null bs
      then liftIO (threadDelay 100000) >> process offset
      else process (offset + fromIntegral (length bs))

 insertCDM cdmFmt =
   liftIO $ do let nses = CDM.toSchema [cdmFmt]
               operations <- compile nses
               let ipts = map (\o -> Input cdmFmt o 0) operations
               mapM_ (atomically . TB.writeTBChan chan) ipts

emit :: String -> Kafka ()
emit = liftIO . hPutStrLn stderr
