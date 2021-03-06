{-# LANGUAGE OverloadedStrings     #-}
{-# LANGUAGE DataKinds             #-}
{-# LANGUAGE TypeSynonymInstances  #-}
{-# LANGUAGE FlexibleContexts      #-}
{-# LANGUAGE FlexibleInstances     #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE ScopedTypeVariables   #-}
{-# LANGUAGE QuasiQuotes           #-}
{-# LANGUAGE RecordWildCards       #-}
{-# LANGUAGE TypeOperators         #-}
module Main where

import           Control.Concurrent (forkIO, threadDelay)
import           Control.Concurrent.MVar
import           Control.Concurrent.STM.TChan
import           Control.Concurrent.STM
import           Control.Exception as X
import           Control.Monad
import           Control.Monad.Trans.Except (ExceptT)
import           Control.Monad.IO.Class (liftIO)
import           Data.ByteString (ByteString)
import           Data.Functor.Identity
import           Data.Monoid
import qualified Data.Sequence as Seq
import           Data.Sequence (Seq)
import qualified Data.Foldable as F
import           Data.String
import           Data.String.QQ
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.IO as T
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
import           System.IO (stderr)
import           System.Exit (exitSuccess)

import Paths_adapt_dashboard

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

     forkPersist $ kafkaInput (kafkaServer c) (inTopic c) ig
     forkPersist $ kafkaInput (kafkaServer c) (pxTopic c) px
     forkPersist $ kafkaInput (kafkaServer c) (seTopic c) se
     forkPersist $ kafkaInput (kafkaServer c) (adTopic c) ad
     forkPersist $ kafkaInput (kafkaServer c) (acTopic c) ac
     forkPersist $ kafkaInput (kafkaServer c) (dxTopic c) dx

     run (port c) (dashboard c stMV)
 where
 forkPersist = forkIO . void . persistant
 persistant :: (Show a, Show e) => IO (Either e a) -> IO ()
 persistant io = forever $ do
      ex <- X.catch (Right <$> io) (pure . Left)
      case ex of
       Right (Right r) ->
         T.hPutStrLn stderr ("Operation completed: " <> T.pack (show r))
       Right (Left e) ->
         do T.hPutStrLn stderr ("Operation failed: " <> T.pack (show e))
            threadDelay 5000000
       Left (e::SomeException) ->
         do T.hPutStrLn stderr ("Operation had an exception: " <> T.pack (show e))
            threadDelay 5000000

data Channels = Channels { igChan, pxChan, seChan, adChan, acChan, dxChan :: TChan Text }
data Status   = Status   { igStat, pxStat, seStat, adStat, acStat, dxStat :: Seq Text }

defaultStatus :: Status
defaultStatus = Status Seq.empty Seq.empty Seq.empty Seq.empty Seq.empty Seq.empty

dashboard :: Config -> MVar Status -> Application
dashboard c curr = serve dashboardAPI (lonePage c curr :<|> rawStuff)

rawStuff :: Application
rawStuff a b =
  do x <- getDataFileName "static"
     serveDirectory x a b

lonePage :: Config -> MVar Status -> ExceptT ServantErr IO (Html ())
lonePage c stMV =
  do st <- liftIO (readMVar stMV)
     return (buildPage st)
 where
 buildPage :: Status -> Html ()
 buildPage (Status {..}) =
  do title_ "ADAPT Dashbaord"
     body_ [] $ do
      meta_ [httpEquiv_ "refresh", content_ "5"]
      renderBox "Ingestion" "ig_textarea" igStat
      -- h2_ "Pattern Extraction"
      -- textarea_ [ id_ "pe_textarea"
      --           , readonly_ "true"
      --           , rows_ "25", cols_ "140"]
      --           (toHtml pxStat)
      renderBox "Segmentation" "se_textarea" seStat
      renderBox "Anomaly Detection" "ad_textarea" adStat
      renderBox "Activity Classification" "ac_textarea" acStat

      renderBox "Diagnostics" "dx_textarea" dxStat
      h2_ "UI"
      h3_ "Manual Activity Classifier"
      a_ [href_ "http://localhost:8181/classification"] "Activity Classifier"
      h3_ "General Graph Viewer"
      a_ [href_ "http://localhost:8181/graph"] "General Graph Viewer"
      h3_ "General Graph Viewer log"
      a_ [href_ "http://localhost:8181/log"] "General Graph Viewer log"
      return ()
  where
  renderBox :: HtmlT Identity () -> Text -> Seq Text -> Html ()
  renderBox name boxid queue =
   do h2_ name
      textarea_ [ id_ boxid
                , readonly_ "true"
                , rows_ "25", cols_ "140"]
                (toHtml $ T.unlines $ reverse $ F.toList queue)

updateStatus :: Channels -> MVar Status -> IO ()
updateStatus (Channels {..}) stMV = forever (go >> threadDelay 100000)
 where
  go :: IO ()
  go = mapM_ (uncurry doMod)
             [ (igChan, (\st x -> st { igStat = let len = Seq.length (igStat st)
                                                in Seq.drop (len-1000) (igStat st Seq.|> x) }))
             , (pxChan, (\st x -> st { pxStat = let len = Seq.length (pxStat st)
                                                in Seq.drop (len-1000) (pxStat st Seq.|> x) }))
             , (seChan, (\st x -> st { seStat = let len = Seq.length (seStat st)
                                                in Seq.drop (len-1000) (seStat st Seq.|> x) }))
             , (adChan, (\st x -> st { adStat = let len = Seq.length (adStat st)
                                                in Seq.drop (len-1000) (adStat st Seq.|> x) }))
             , (acChan, (\st x -> st { acStat = let len = Seq.length (acStat st)
                                                in Seq.drop (len-1000) (acStat st Seq.|> x) }))
             , (dxChan, (\st x -> st { dxStat = let len = Seq.length (dxStat st)
                                                in Seq.drop (len-1000) (dxStat st Seq.|> x) }))
             ]

  doMod ch setter =
     modifyMVar_ stMV $ \st ->
       maybe st (setter st) <$> atomically (tryReadTChan ch)

--------------------------------------------------------------------------------
--  Servant type absurdity

type DashboardAPI = Get '[HTMLLucid] (Html ())
                 :<|> "ext" :> Raw

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
      else process (offset + fromIntegral (length bs))

 getMessage :: Offset -> Kafka [ByteString]
 getMessage offset =
  map tamPayload . fetchMessages <$> withAnyHandle (\h -> fetch' h =<< fetchRequest offset 0 topic)
