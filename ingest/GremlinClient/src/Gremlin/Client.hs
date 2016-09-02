{-# LANGUAGE RecordWildCards            #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ScopedTypeVariables        #-}
{-# LANGUAGE ParallelListComp           #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE BangPatterns               #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# OPTIONS_GHC -fno-warn-orphans       #-}
module Gremlin.Client
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  , Response(..), Request(..)
    -- * Resource safe, interface
  , DB, DBT, withDB, sendCommand, sendRequest
    -- * Connection oriented, manual resource management, interface.
  , connect, DBConnection(..), mkRequest, wait
    -- * Lower Level
  , UUID
  , WS.ConnectionException(..)
  ) where

import           Control.Concurrent (forkIO, threadDelay, killThread)
import           Control.Concurrent.Async
import           Control.Concurrent.Async (async,wait)
import           Control.Concurrent.STM (atomically, retry)
import           Control.Concurrent.STM.TVar (TVar, readTVar, writeTVar, newTVarIO)
import           Control.Concurrent.MVar
import           Control.Exception as X
import           Control.Monad (forever)
import           Crypto.Hash.SHA256 (hash)
import           Data.Aeson (Value(..), FromJSON(..), ToJSON(..), (.:), (.=))
import qualified Data.Aeson as A
import qualified Data.Aeson.Types as A
import qualified Data.ByteString.Lazy as BL
import           Data.HashMap.Strict (HashMap)
import qualified Data.HashMap.Strict as HMap
import           Data.IORef
import           Data.List (intersperse)
import           Data.List.Split (chunksOf)
import qualified Data.Map.Strict as Map
import           Data.Monoid ((<>))
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.IO as T
import qualified Data.Text.Encoding as T
import           Data.UUID (UUID)
import qualified Data.UUID as UUID
import           MonadLib
import qualified Network.WebSockets as WS
import System.IO (stderr)

data ServerInfo = ServerInfo { host     :: String
                             , port     :: Int
                             , maxOutstandingRequests :: Int
                             }
        deriving (Eq,Ord,Show)

defaultServer :: ServerInfo
defaultServer = ServerInfo "localhost" 8182 128

--------------------------------------------------------------------------------
-- The database monad interface providing a automatic, regional, resource
-- management API


-- | The Gremlin client monad allows for sending or handling responses while
-- tracking the number of outstanding requests.
type DB a = DBT IO a
newtype DBT m a = DB { runDB :: StateT DBState m a
                  } deriving (Monad, Applicative, Functor)

data DBState = DBS { lock :: Mutex
                   , connection :: WS.Connection
                   }

instance Monad m => StateM (DBT m) DBState where
  get = DB get
  set = DB . set

instance MonadT DBT where
  lift = DB . lift

withDB :: ServerInfo ->  (WS.Message -> DB ()) -> DB () -> IO (Either WS.ConnectionException ())
withDB (ServerInfo {..}) recvHdl mainOper =
  do lock <- newMutex maxOutstandingRequests
     X.catch (WS.runClient host port "/" (fmap Right . loop lock))
             (return . Left)
 where
 loop :: Mutex -> WS.Connection -> IO ()
 loop lock conn =
  do _ <- forkIO (forever (WS.receive conn >>= responseHandler lock conn))
     runM (runDB mainOper) (DBS lock conn)
     return ()
 responseHandler :: Mutex -> WS.Connection -> WS.Message -> IO ()
 responseHandler lock conn msg =
  do case msg of
        WS.DataMessage _ -> mutexInc lock
        _                -> return ()
     runM (runDB $ recvHdl msg) (DBS lock conn)
     return ()

-- | Send a raw command and its environment.  All values in the command
-- must already be properly escaped!
sendCommand :: Text -> Env -> DB UUID
sendCommand cmd env =
 do DBS lock conn <- get
    let (req,uuid) = mkGremlinWSCommand cmd env
    lift $ mutexDec lock
    lift $ WS.send conn req
    return uuid

sendRequest :: Request -> DB ()
sendRequest req =
  do DBS lock conn <- get
     lift $ sendRequestIO req lock conn

sendRequestIO :: Request -> Mutex -> WS.Connection -> IO ()
sendRequestIO req lock conn =
  do mutexDec lock
     WS.send conn (WS.DataMessage $ WS.Text (A.encode req))
     return ()

-- | Make the data insertion command and return the request's UUID
-- N.B. The UUID is deterministic, computed as a hash of command and
-- environment, so identical commands will have identical UUIDs!
mkGremlinWSCommand :: Text -> Env -> (WS.Message,UUID)
mkGremlinWSCommand cmd bnd =
  let req = mkRequest cmd bnd
  in (WS.DataMessage $ WS.Text (A.encode req), requestId req)

mkHashedUUID :: Text -> Env -> UUID
mkHashedUUID t env =
  let unique = T.concat $ t : map (T.pack . show) (Map.toList env)
  in maybe (error "Impossible uuid decode failure") id
           (UUID.fromByteString (BL.take 16 (BL.fromStrict (hash (T.encodeUtf8 unique)))))


mkRequest :: Text -> Env -> Request
mkRequest cmd bnd =
      Req { requestId = mkHashedUUID cmd bnd
          , op        = "eval"
          , processor = ""
          , args = ReqArgs { gremlin  = cmd
                           , bindings = bnd
                           , language = "gremlin-groovy"
                           }
          }

--------------------------------------------------------------------------------
--  Manual Resource Management API

data DBConnection = DBC { sendOn :: Request -> (Response -> IO ()) -> IO ()
                        -- ^ @res <- sendOn dbc req@ sends a request over a database connection,
                        -- acquiring an async.  Use `wait res` to get the
                        -- response.
                        , close  :: IO ()
                        -- ^ Close a connection.
                        }

-- | Create a persistent connection to the database websocket.
--
-- N.B. This mechanism is currently extremely inefficient!
connect :: ServerInfo -> IO (Either WS.ConnectionException DBConnection)
connect si =
  do reqMVar  <- newEmptyMVar
     respMap  <- newTVarIO HMap.empty
     let recvResponse = recvHdl respMap
         loop = mainOper respMap reqMVar
     dbThread <- forkIO (safe "withDB-connect" (void $ withDB si recvResponse loop))
     let doSend r op = putMVar reqMVar (r,op)
         doClose     = killThread dbThread
     return $ Right $ DBC doSend doClose
 where
  recvHdl :: TVar (HashMap UUID (Response -> IO ())) -> WS.Message -> DB ()
  recvHdl respMap msg =
    case msg of
       WS.DataMessage dm ->
          do let bs = case dm of { WS.Text val -> val; WS.Binary val -> val }
             case A.decode bs of
              Just resp ->
               do op <- lift $ atomically $ do
                          mp <- readTVar respMap
                          let uuid = respRequestId resp
                          case HMap.lookup uuid mp of
                            Nothing -> return (\_ -> return ())
                            Just op ->
                              do writeTVar respMap (HMap.delete uuid mp)
                                 return op
                  lift $ op resp
              Nothing   -> return ()
       WS.ControlMessage _ -> return ()
  mainOper :: TVar (HashMap UUID (Response -> IO ()))
           -> MVar (Request,Response -> IO ())
           -> DB ()
  mainOper respMap reqMVar =
   do DBS lock conn <- get
      lift $ foreverSafe $
              do (req,op) <- takeMVar reqMVar
                 sendAsync respMap req op lock conn

  foreverSafe op =
    X.catch (forever op)
            (\e -> case X.fromException e of
                    Just X.ThreadKilled -> X.throw e
                    Nothing             -> foreverSafe op)

  sendAsync :: TVar (HashMap UUID (Response -> IO ()))
            -> Request
            -> (Response -> IO ())
            -> Mutex -> WS.Connection
            -> IO ()
  sendAsync respMap req op lock conn =
    do atomically $
         do mp <- readTVar respMap
            let !mp2 = HMap.insert (requestId req) op mp
            writeTVar respMap mp2
       sendRequestIO req lock conn

--------------------------------------------------------------------------------
--  Gremlin WebSockets JSON API

data Request =
  Req { requestId :: UUID
      , op        :: Text
      , processor :: Text
      , args      :: RequestArgs
      }

type Env = Map.Map Text A.Value -- XXX Hash map
data RequestArgs =
  ReqArgs { gremlin    :: Text
          , bindings   :: Env
          , language   :: Text
          }

-- | A gremlin response consists of the UUID from the original request, the
-- status (HTTP code numbers), and a resulting environment
-- usually containing a 'data' field if a value was returned.
data Response =
   Resp { respRequestId :: UUID
        , respResult    :: HashMap Text A.Value
        , respStatus    :: Int
        }

instance FromJSON Response where
  parseJSON (Object o) =
    do mp <- o .: ("result" :: Text)
       uuid <- o .: ("requestId" :: Text)
       stat <- o .: ("status" :: Text)
       code <- stat .: ("code" :: Text)
       return (Resp uuid mp code)

  parseJSON j = A.typeMismatch "Response" j

instance ToJSON RequestArgs where
  toJSON (ReqArgs g b l) =
    A.object [ "gremlin"  .= g
             , "language" .= l
             , "bindings" .= b
             , "rebindings" .= A.emptyObject
             ]

instance ToJSON Request where
  toJSON (Req i o p a) =
    A.object [ "requestId" .= i
             , "op"        .= o
             , "processor" .= p
             , "args"      .= a
             ]

instance ToJSON UUID where
  toJSON = toJSON . UUID.toText

instance FromJSON UUID where
  parseJSON (A.String s) = case UUID.fromText s of
                            Nothing -> fail "Could not decode UUID."
                            Just x  -> return x
  parseJSON j = A.typeMismatch "UUID" j

instance FromJSON RequestArgs where
  parseJSON (A.Object obj) =
      ReqArgs <$> obj .: "gremlin"
              <*> obj .: "bindings"
              <*> obj .: "language"
  parseJSON j = A.typeMismatch "RequestArgs" j

instance FromJSON Request where
  parseJSON (A.Object obj) =
      Req <$> obj .: "requestId"
          <*> obj .: "op"
          <*> obj .: "processor"
          <*> obj .: "args"
  parseJSON j = A.typeMismatch "Request" j

encodeQuoteText :: Text -> Text
encodeQuoteText = quote . subChars . escapeChars

quote :: Text -> Text
quote b = T.concat ["\'", b, "\'"]

escapeChars :: Text -> Text
escapeChars b
  | not (T.any (`Set.member` escSet) b) = b
  | otherwise = T.concatMap (\c -> if c `Set.member` escSet then T.pack ['\\', c] else T.singleton c) b

escSet :: Set.Set Char
escSet = Set.fromList ['\\', '"']

subChars :: Text -> Text
subChars b
  | not (T.any (`Set.member` badChars) b) = b
  | otherwise = T.map (\c -> maybe c id (Map.lookup c charRepl)) b

charRepl :: Map.Map Char Char
charRepl = Map.fromList [('\t',' ')]

badChars :: Set.Set Char
badChars = Map.keysSet charRepl

--------------------------------------------------------------------------------
--  Utility: Mutex type

newtype Mutex = Mutex (IORef Int)

newMutex :: Int -> IO Mutex
newMutex n = Mutex <$> newIORef n

mutexInc :: Mutex -> IO ()
mutexInc (Mutex ref) = atomicModifyIORef' ref (\v -> (v+1,()))

mutexDec :: Mutex -> IO ()
mutexDec m@(Mutex ref) =
  do success <- atomicModifyIORef ref (\v -> if v > 0 then (v-1,True) else (v,False))
     if success then return ()
                else threadDelay 50000 >> mutexDec m

-- Utility: Catcher
-- XXX This needs to check for 'ThreadKilled' and not retry.
safe :: T.Text -> IO () -> IO ()
safe threadName op = go
 where
  go =
    do X.catch op (\(e :: X.SomeException) -> T.hPutStrLn stderr $ "Thread '" <> threadName <> "' failed because: " <> T.pack (show e))
       threadDelay 500000 -- 500 ms
       go
