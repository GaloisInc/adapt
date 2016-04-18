{-# LANGUAGE OverloadedStrings     #-}
{-# LANGUAGE DataKinds             #-}
{-# LANGUAGE TypeSynonymInstances  #-}
{-# LANGUAGE FlexibleContexts      #-}
{-# LANGUAGE FlexibleInstances     #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE QuasiQuotes #-}
{-# LANGUAGE RecordWildCards #-}
module Main where

import           Control.Concurrent (forkIO, threadDelay)
import           Control.Concurrent.MVar
import           Control.Concurrent.STM.TChan
import           Control.Concurrent.STM
import           Control.Monad
import           Control.Monad.Trans.Either (EitherT)
import           Control.Monad.IO.Class (liftIO)
import           Data.ByteString (ByteString)
import           Data.String
import           Data.String.QQ
import           Data.Text (Text, unlines)
import           Data.Text.Encoding as T
import           Lucid
import           Lucid.Html5
import           Network.HTTP.Media ((//), (/:))
import           Network.Kafka.Consumer
import           Network.Kafka
import           Network.Kafka.Protocol (TopicName, Offset)
import           Network.Wai (Application)
import           Network.Wai.Handler.Warp (run)
import           Servant
import           Servant.Server
import           SimpleGetOpt
import           System.Exit (exitSuccess)

data Config =
  Config { inTopic :: TopicName
         , pxTopic :: TopicName
         , seTopic :: TopicName
         , adTopic :: TopicName
         , acTopic :: TopicName
         , dxTopic :: TopicName
         , port    :: Int
         , kafkaServer :: KafkaAddress
         , help    :: Bool
         }


defaultKafka :: KafkaAddress
defaultKafka = ("localhost", 9092)

defaultConfig :: Config
defaultConfig =
  Config { inTopic     = "ingestd-log"
         , pxTopic     = "px-log"
         , seTopic     = "se-log"
         , adTopic     = "ad-log"
         , acTopic     = "ac-log"
         , dxTopic     = "dx-log"
         , port        = 8080
         , kafkaServer = defaultKafka
         , help = False
         }

options :: OptSpec Config
options =
  OptSpec { progDefaults = defaultConfig
          , progOptions =
              [ Option ['h'] ["help"]
                        "Display help"
                        $ NoArg $ \s -> Right s { help = True }
              , Option [] ["in"]
                        "Ingester (In) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { inTopic = fromString a }
              , Option [] ["px"]
                        "Pattern Extractor (PX) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { pxTopic = fromString a }
              , Option [] ["se"]
                        "Segmenter (SE) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { seTopic = fromString a }
              , Option [] ["ad"]
                        "Anomaly Detector (AD) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { adTopic = fromString a }
              , Option [] ["ac"]
                        "Activity Classifier (AC) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { acTopic = fromString a }
              , Option [] ["dx"]
                        "Diagnostics (DX) Logging Topic"
                        $ ReqArg "topic" $ \a s -> Right s { dxTopic = fromString a }
              ]
          , progParamDocs = []
          , progParams = \_ _ -> Left "Program does not accept parameters."
          }

main :: IO ()
main =
  do c <- getOpts options
     when (help c) (putStrLn (usageString options) >> exitSuccess)

     ig <- newTChanIO
     px <- newTChanIO
     se <- newTChanIO
     ad <- newTChanIO
     ac <- newTChanIO
     dx <- newTChanIO
     let channels = Channels ig px se ad ac dx

     stMV <- newMVar defaultStatus
     forkIO (updateStatus channels stMV)

     forkIO $ void $ kafkaInput (kafkaServer c) (inTopic c) ig
     forkIO $ void $ kafkaInput (kafkaServer c) (inTopic c) px
     forkIO $ void $ kafkaInput (kafkaServer c) (seTopic c) se
     forkIO $ void $ kafkaInput (kafkaServer c) (adTopic c) ad
     forkIO $ void $ kafkaInput (kafkaServer c) (acTopic c) ac
     forkIO $ void $ kafkaInput (kafkaServer c) (dxTopic c) dx

     run (port c) (dashboard c stMV)

data Channels = Channels { igChan, pxChan, seChan, adChan, acChan, dxChan :: TChan Text }
data Status   = Status   { igStat, pxStat, seStat, adStat, acStat, dxStat :: Text }

defaultStatus :: Status
defaultStatus = Status "" "" "" "" "" ""

dashboard :: Config -> MVar Status -> Application
dashboard c curr = serve dashboardAPI (lonePage c curr)

lonePage :: Config -> MVar Status -> EitherT ServantErr IO (Html ())
lonePage c stMV =
  do st <- liftIO (readMVar stMV)
     return (buildPage st)
 where
 buildPage :: Status -> Html ()
 buildPage (Status {..}) =
  do title_ "ADAPT Dashbaord"
     body_ [] $ do
      h2_ "Ingestion"
      p_ (toHtml igStat)
      h2_ "Pattern Extraction"
      p_ (toHtml pxStat)
      h2_ "Segmentation"
      p_ (toHtml seStat)
      h2_ "Anomaly Detection"
      p_ (toHtml adStat)
      h2_ "Activity Classification"
      p_ (toHtml acStat)
      h2_ "Diagnostics"
      p_ (toHtml dxStat)
      return ()

updateStatus :: Channels -> MVar Status -> IO ()
updateStatus (Channels {..}) stMV = forever (go >> threadDelay 100000)
 where
  go :: IO ()
  go = mapM_ (uncurry doMod) [ (igChan, (\st x -> st { igStat = x }))
                             , (pxChan, (\st x -> st { pxStat = x }))
                             , (seChan, (\st x -> st { seStat = x }))
                             , (adChan, (\st x -> st { adStat = x }))
                             , (acChan, (\st x -> st { acStat = x }))
                             , (dxChan, (\st x -> st { dxStat = x }))
                             ]

  doMod ch setter =
     modifyMVar_ stMV $ \st ->
       maybe st (setter st) <$> atomically (tryReadTChan ch)

--------------------------------------------------------------------------------
--  Servant type absurdity

type DashboardAPI = Get '[HTMLLucid] (Html ())

data HTMLLucid

instance Accept HTMLLucid where
    contentType _ = "text" // "html" /: ("charset", "utf-8")

instance MimeRender HTMLLucid (Html a) where
    mimeRender _ = renderBS

dashboardAPI :: Proxy DashboardAPI
dashboardAPI = Proxy


--------------------------------------------------------------------------------
--  Kafka work

-- |Acquire messages from a given kafka host/topic and insert into
-- a channel.
kafkaInput :: KafkaAddress -> TopicName -> TChan Text -> IO (Either KafkaClientError ())
kafkaInput host topic chan = runKafka state oper
 where
 state = mkKafkaState "adapt-ingest-log-recver" host
 oper = forever $
  do o <- getLastOffset LatestTime 0 topic
     process o

 process :: Offset -> Kafka ()
 process offset =
  do bs <- getMessage offset
     let handleMsg b = liftIO $ atomically (writeTChan chan (T.decodeUtf8 b))
     mapM_ handleMsg bs
     if null bs
      then liftIO (threadDelay 100000) >> process offset
      else process (offset+1)

 getMessage :: Offset -> Kafka [ByteString]
 getMessage offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topic)

