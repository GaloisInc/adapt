{-# LANGUAGE OverloadedStrings   #-}
{-# LANGUAGE RecordWildCards     #-}
{-# LANGUAGE ViewPatterns        #-}
{-# LANGUAGE TupleSections       #-}
{-# LANGUAGE FlexibleInstances   #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE TemplateHaskell     #-}
-- | Ingestd is the ingest daemon which takes TA-1 data from Kafka queue,
-- decodes it as Avro-encodedCDM, translates to Adapt Schema, uploads to
-- Titan, and pushes node IDs to PX via Kafka.
module Main where

import Prelude
import SimpleGetOpt
import Control.Applicative ((<$>))
import Control.Monad (when, forever, void)
import Control.Exception as X
import Data.Int (Int32)
import Data.Monoid ((<>))
import qualified Data.Foldable as F
import Data.Maybe (maybeToList)
import Data.List (partition,intersperse)
import Data.Graph hiding (Node, Edge)
import Data.Time (UTCTime, addUTCTime)
import Text.Read (readMaybe)
import qualified Data.Set as Set
import qualified Data.Map as Map
import           Data.Map (Map)
import Numeric (showHex)
import Data.Binary (encode,decode)
import System.Entropy (getEntropy)
import MonadLib        hiding (handle)
import MonadLib.Monads hiding (handle)
import Data.Text (Text)
import Control.Lens
import Data.String
import qualified Data.Text as T
import qualified Data.Text.IO as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import qualified Data.Text.Lazy.Encoding as Text
import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as ByteString
import qualified Data.ByteString.Base64 as B64
import System.Exit (exitFailure)
import System.IO (stderr)
import Control.Concurrent
import Control.Concurrent.BoundedChan as BC

import Network.Kafka as K
import Network.Kafka.Protocol as K

import Titan
import IngestDaemon.KafkaManager

data Config =
      Config { _logTopic    :: Maybe TopicName
             , _verbose     :: Bool
             , _help        :: Bool
             , _kafkaServer :: K.KafkaAddress
             , _inputTopics :: [K.TopicName]
             , _outputTopic :: TopicName
             , _titanServer :: Titan.ServerInfo
             } deriving (Show)

makeLenses ''Config

inputQueueSize  :: Int
inputQueueSize  = 1000

outputQueueSize :: Int
outputQueueSize = 1000

defaultKafka :: KafkaAddress
defaultKafka = ("localhost", 9092)

defaultConfig :: Config
defaultConfig = Config
  { _logTopic     = Nothing
  , _verbose      = False
  , _help         = False
  , _kafkaServer  = defaultKafka
  , _inputTopics  = []
  , _outputTopic = "pattern"
  , _titanServer  = Titan.defaultServer
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = []
               , progParams    = \_ s -> Left "No extra parameters are accepted."
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
                    "Set a PX topic to be used as output"
                    $ ReqArg "Topic" $
                        \str s -> Right (s & outputTopic .~ fromString str)
                  , Option ['t'] ["titan"]
                    "Set the titan server"
                    $ OptArg "host:port" $
                        \str s ->
                            let svr = uncurry ServerInfo <$> (parseHostPort =<< str)
                            in case (str,svr) of
                                (Nothing,_)  -> Right (s & titanServer .~ defaultServer)
                                (_,Just res) -> Right (s & titanServer .~ res)
                                (_,Nothing)  -> Left "Could not parse host:port string."
                  , Option ['k'] ["kafka"]
                    "Set the kafka server"
                    $ OptArg "host:port" $
                        \str s ->
                            let svr = parseHostPort =<< str
                            in case (str,svr) of
                                (Nothing,_)  -> Right (s & kafkaServer .~ defaultKafka)
                                (_,Just res) -> Right (s & kafkaServer .~ res)
                                (_,Nothing)  -> Left "Could not parse host:port string."
                  ]
               }

parseHostPort :: (IsString s, Num n) => String -> Maybe (s,n)
parseHostPort str =
  let (h,p) = break (== ':') str
  in case (h,readMaybe p) of
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

mainLoop :: Config -> IO ()
mainLoop cfg =
 do inputs  <- newBoundedChan inputQueueSize   :: IO (BoundedChan (Operation Text))
    outputs <- newBoundedChan outputQueueSize  :: IO (BoundedChan Text)
    logChan <- newBoundedChan 100 :: IO (BoundedChan Text)
    let srv      = cfg ^. kafkaServer
        inTopics = cfg ^. inputTopics
        outTopic = cfg ^. outputTopic
        logMsg   = void . BC.tryWriteChan logChan
        readLog  = BC.readChan logChan
        logPXMsg = logMsg . ("ingestd[PX-Kafka thread]: " <>)
        logTitan = logMsg . ("ingestd[Titan thread]:    " <>)
        logIpt t = logMsg . (("ingestd[from " <> kafkaString t <> "]") <>)
        logStderr = T.hPutStrLn stderr
        forkPersist log = void . forkIO . persistant log
    _ <- forkPersist logPXMsg (nodeIdsToKafkaPX srv outTopic outputs)
    _ <- maybe (forkPersist logStderr  (channelToStderr logChan))
               (forkPersist logStderr . channelToKafka  logChan srv)
               (cfg ^. logTopic)
    mapM_ (\t -> forkPersist (logIpt t) $ void $ kafkaInput srv t inputs) inTopics
    persistant logTitan $
     void $ Titan.withTitan (cfg ^. titanServer) resHdl $ \conn ->
      forever $ do op <- BC.readChan inputs
                   Titan.send op conn
                   mapM_ (BC.writeChan outputs) (maybeToList $ getVertexLabel op)
  where
  resHdl _ _ = return ()

persistant :: Show a => (Text -> IO ()) -> IO a -> IO ()
persistant logMsg io =
  do ex <- X.catch (Right <$> io) (pure . Left)
     case ex of
      Right r ->
        do logMsg ("Operation completed: " <> T.pack (show r))
           persistant logMsg io
      Left (e::SomeException) ->
        do logMsg ("Operation had an exception: " <> T.pack (show e))
           threadDelay 5000000
           persistant logMsg io

getVertexLabel :: Operation Text -> Maybe Text
getVertexLabel (InsertVertex label _) = Just label
getVertexLabel _                      = Nothing

channelToStderr :: BC.BoundedChan Text -> IO ()
channelToStderr ch = forever (BC.readChan ch >>= T.hPutStrLn stderr)

kafkaString :: TopicName -> Text
kafkaString (TName (KString s)) = T.decodeUtf8 s
