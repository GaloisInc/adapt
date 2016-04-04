{-# LANGUAGE OverloadedStrings #-}
module IngestDaemon.KafkaManager where

import           Control.Concurrent
import           Control.Concurrent.BoundedChan as BC
import           Control.Monad (forever)
import           Control.Monad.IO.Class (liftIO)
import           Control.Parallel.Strategies
import           Data.ByteString (ByteString)
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

import           IngestDaemon.Avro

type Statement = Operation Text

--------------------------------------------------------------------------------
--  Placing the Node IDs on Kafka queues for PX

nodeIdsToKafkaPX :: KafkaAddress -> TopicName -> BoundedChan Text -> IO ()
nodeIdsToKafkaPX host topic chan = forever $ do
  r <- runKafka state oper
  case r of
    Left err -> hPutStrLn stderr ("Kafka producer failed: " ++ show err)
    Right () -> hPutStrLn stderr "Impossible: Kafka ingest->px terminted without error."
 where
 state = mkKafkaState "ingest-px" host
 oper = forever $ do
   m <- liftIO (BC.readChan chan)
   produceMessages [TopicAndMessage topic $ makeMessage (T.encodeUtf8 m)]

--------------------------------------------------------------------------------
--  Getting the CDM input from TA1

-- |Acquire CDM from a given kafka host/topic and place the decoded Adapt
-- Schema vales in the given channel.
kafkaInput :: KafkaAddress -> TopicName -> BoundedChan Statement -> IO (Either KafkaError ())
kafkaInput host topic chan = forever $ do
  r <- runKafka state oper
  case r of
       Left err -> do hPutStrLn stderr ("Kafka consumer failed: " ++ show err)
                      threadDelay 100000
       Right () -> hPutStrLn stderr "Impossible: Kafka ingest terminated without error."
 where
 state = mkKafkaState "adapt-ingest" host
 oper =
  do o <- getLastOffset EarliestTime 0 topic
     process o

 process :: Offset -> Kafka ()
 process offset =
  do bs <- getMessage offset
     let handleMsg b =
          case decodeAvro b of
            Right cdmFmt ->
               liftIO $ do
                  let nses = CDM.toSchema cdmFmt
                  ms <- compile nses
                  BC.writeList2Chan chan ms
            Left err    -> emit (show err)
     mapM_ handleMsg bs
     process (offset+1)

 getMessage :: Offset -> Kafka [ByteString]
 getMessage offset =
  map tamPayload . fetchMessages <$> fetch offset 0 topic

emit :: String -> Kafka ()
emit = liftIO . hPutStrLn stderr
