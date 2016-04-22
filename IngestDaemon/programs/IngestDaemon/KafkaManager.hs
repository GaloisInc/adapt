{-# LANGUAGE OverloadedStrings #-}
module IngestDaemon.KafkaManager where

import           Control.Concurrent
import           Control.Concurrent.BoundedChan as BC
import           Control.Monad (forever)
import           Control.Monad.IO.Class (liftIO)
import           Control.Parallel.Strategies
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

import           Schema
import           CompileSchema
import           CommonDataModel as CDM
import           CommonDataModel.Avro

type Statement = Operation Text

channelToKafka :: BoundedChan Text -> KafkaAddress -> TopicName -> IO (Either KafkaClientError ())
channelToKafka ch host topic =
 do r <- runKafka state oper
    case r of
      Left err -> hPutStrLn stderr ("Kafka logging failed: " ++ show err)
      Right () -> hPutStrLn stderr ("Kafka logging terminated somehow.")
    return r
 where
 state = mkKafkaState "ingest-logging" host
 oper = forever $ do
   m <- liftIO (BC.readChan ch)
   produceMessages [TopicAndMessage topic $ makeMessage (T.encodeUtf8 m)]

--------------------------------------------------------------------------------
-- PE Signalling

-- We don't place node IDs on the queue for the first engagement, just
-- a signal indicating KB is ready.
finishIngestSignal :: KafkaAddress -> TopicName -> TopicName -> IO (Either KafkaClientError ())
finishIngestSignal svr out ipt =
 do r <- runKafka state oper
    case r of
      Left err -> hPutStrLn stderr ("Kafka signaling failed: " ++ show err)
      Right () -> hPutStrLn stderr ("Kafka signaling terminated somehow.")
    return r
 where
 state = mkKafkaState "ingest-px" svr
 oper =
   do o <- getLastOffset LatestTime 0 ipt
      forever (process o)
 process o = do
  do bs <- getMessage ipt o
     mapM_ propogateSignal bs
     if null bs
      then liftIO (threadDelay 100000) >> process o
      else process (o+1)
 propogateSignal b
  | BS.length b == 1 =
     do produceMessages [TopicAndMessage out $ makeMessage b]
        return ()
  | otherwise = return () -- First engagement: the only valid signals are 0,1

getMessage :: TopicName -> Offset -> Kafka [ByteString]
getMessage topicNm offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topicNm)

--------------------------------------------------------------------------------
--  Getting the CDM input from TA1

-- |Acquire CDM from a given kafka host/topic and place the decoded Adapt
-- Schema values in the given channel.
kafkaInput :: KafkaAddress -> TopicName -> BoundedChan Statement -> IO (Either KafkaClientError ())
kafkaInput host topic chan =
  do r <- runKafka state oper
     return r
 where
 state = mkKafkaState "adapt-ingest" host
 oper = forever $
  do o <- getLastOffset LatestTime 0 topic
     process o

 process :: Offset -> Kafka ()
 process offset =
  do bs <- getMessage topic offset
     let handleMsg b =
          case runGetOrFail getAvro (BL.fromStrict b) of
            Right (_,_,cdmFmt) ->
               liftIO $ do
                  let nses = CDM.toSchema [cdmFmt]
                  ms <- compile nses
                  BC.writeList2Chan chan ms
            Left err    -> emit (show err)
     mapM_ handleMsg bs
     if null bs
      then liftIO (threadDelay 100000) >> process offset
      else process (offset+1)

emit :: String -> Kafka ()
emit = liftIO . hPutStrLn stderr
