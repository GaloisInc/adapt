{-# LANGUAGE OverloadedStrings     #-}
{-# LANGUAGE DataKinds             #-}
{-# LANGUAGE TypeSynonymInstances  #-}
{-# LANGUAGE FlexibleContexts      #-}
{-# LANGUAGE FlexibleInstances     #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE QuasiQuotes #-}
module Main where

import           Control.Monad
import           Control.Monad.Trans.Either (EitherT)
import           Data.String
import           Data.String.QQ
import           Data.Text (Text, unlines)
import           Lucid
import           Network.HTTP.Media ((//), (/:))
import           Network.Kafka.Consumer
import           Network.Kafka.Protocol (TopicName)
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
         , help    :: Bool
         }

defaultConfig :: Config
defaultConfig =
  Config { inTopic = "ingestd-log"
         , pxTopic = "px-log"
         , seTopic = "se-log"
         , adTopic = "ad-log"
         , acTopic = "ac-log"
         , dxTopic = "dx-log"
         , port    = 8080
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
     run (port c) (dashboard c)

dashboard :: Config -> Application
dashboard c = serve dashboardAPI (lonePage c)

lonePage :: Config -> EitherT ServantErr IO (Html ())
lonePage c = return $ do
  title_ "ADAPT Dashbaord"

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
