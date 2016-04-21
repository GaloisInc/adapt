{-# LANGUAGE OverloadedStrings #-}
module IngestDaemon.KafkaManager where

import           Control.Concurrent
import           Control.Concurrent.BoundedChan as BC
import           Control.Monad (forever)
import           Control.Monad.IO.Class (liftIO)
import           Control.Parallel.Strategies
import           Data.ByteString (ByteString)
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

channelToKafka :: BoundedChan Text -> KafkaAddress -> TopicName -> IO ()
channelToKafka ch host topic =
 do r <- runKafka state oper
    case r of
      Left err -> hPutStrLn stderr ("Kafka logging failed: " ++ show err)
      Right () -> hPutStrLn stderr ("Kafka logging terminated somehow.")
 where
 state = mkKafkaState "ingest-logging" host
 oper = forever $ do
   m <- liftIO (BC.readChan ch)
   produceMessages [TopicAndMessage topic $ makeMessage (T.encodeUtf8 m)]

--------------------------------------------------------------------------------
--  Placing the Node IDs on Kafka queues for PX

nodeIdsToKafkaPX :: KafkaAddress
                 -> TopicName
                 -> BoundedChan Text
                 -> IO (Either KafkaClientError ())
nodeIdsToKafkaPX host topic chan = runKafka state oper
 where
 state = mkKafkaState "ingest-px" host
 oper = forever $ do
   m <- liftIO (BC.readChan chan)
   produceMessages [TopicAndMessage topic $ makeMessage (T.encodeUtf8 m)]

--------------------------------------------------------------------------------
--  Getting the CDM input from TA1

-- |Acquire CDM from a given kafka host/topic and place the decoded Adapt
-- Schema vales in the given channel.
kafkaInput :: KafkaAddress -> TopicName -> BoundedChan Statement -> IO (Either KafkaClientError ())
kafkaInput host topic chan = runKafka state oper
 where
 state = mkKafkaState "adapt-ingest" host
 oper = forever $
  do o <- getLastOffset LatestTime 0 topic
     process o

 process :: Offset -> Kafka ()
 process offset =
  do bs <- getMessage offset
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

 getMessage :: Offset -> Kafka [ByteString]
 getMessage offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topic)

emit :: String -> Kafka ()
emit = liftIO . hPutStrLn stderr
