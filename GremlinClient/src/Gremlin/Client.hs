{-# LANGUAGE RecordWildCards            #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE ScopedTypeVariables        #-}
{-# LANGUAGE ParallelListComp           #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
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
import qualified Data.Map as Map
import           Data.Monoid ((<>))
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import           Data.UUID (UUID)
import qualified Data.UUID as UUID
import           MonadLib
import qualified Network.WebSockets as WS

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

sendRequest :: Request -> DB UUID
sendRequest req =
  do DBS lock conn <- get
     lift $ mutexDec lock
     lift $ WS.send conn (WS.DataMessage $ WS.Text (A.encode req))
     return (requestId req)

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

data DBConnection = DBC { sendOn :: Request -> IO (Async Response)
                        -- ^ @res <- sendOn dbc req@ sends a request over a database connection,
                        -- acquiring an async.  Use `wait res` to get the
                        -- response.
                        , close  :: IO ()
                        -- ^ Close a connection.
                        }

-- | Create a persistent connection to the database websocket.
connect :: ServerInfo -> IO (Either WS.ConnectionException DBConnection)
connect si =
  do reqMVar  <- newEmptyMVar
     respMVar <- newEmptyMVar
     respMap  <- newTVarIO Map.empty
     dbThread <- forkIO $ void $ withDB si (recvHdl respMap reqMVar respMVar) (mainOper respMap reqMVar respMVar)
     let doSend r = putMVar reqMVar r >> takeMVar respMVar
         doClose = killThread dbThread
     return $ Right $ DBC doSend doClose
 where
  recvHdl :: TVar (Map.Map UUID Response) -> MVar Request -> MVar (Async Response) -> WS.Message -> DB ()
  recvHdl respMap reqMVar respMVar msg =
    case msg of
       WS.DataMessage dm ->
          do let bs = case dm of { WS.Text val -> val; WS.Binary val -> val }
             case A.decode bs of
              Just resp ->
                  lift $ atomically $ do
                      mp <- readTVar respMap
                      writeTVar respMap (Map.insert (respRequestId resp) resp mp)
              Nothing   -> return ()
       WS.ControlMessage _ -> return ()
  mainOper :: TVar (Map.Map UUID Response) -> MVar Request -> MVar (Async Response) -> DB ()
  mainOper respMap reqMVar respMVar =
    do req    <- lift $ takeMVar reqMVar
       future <- sendAsync respMap req
       lift $ putMVar respMVar future

  sendAsync :: TVar (Map.Map UUID Response) -> Request -> DB (Async Response)
  sendAsync respMap req =
    do uuid <- sendRequest req
       lift $ async $ atomically $
        do mp <- readTVar respMap
           case Map.lookup uuid mp of
            Nothing   -> retry
            Just resp ->
              do writeTVar respMap (Map.delete uuid mp)
                 return resp

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
       return (Resp uuid mp stat)

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

{-
instance ToJSON GremlinValue where
  toJSON gv =
    case gv of
      GremlinNum i    -> toJSON i
      x               -> toJSON (encodeGremlinValue x)
-}

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

{-
encodeGremlinValue :: GremlinValue -> Text
encodeGremlinValue gv =
  case gv of
    GremlinString s -> escapeChars s
    GremlinNum  n   -> T.pack (show n)
    -- XXX maps and lists are only notionally supported
    GremlinMap xs   -> T.concat ["'["
                                 , T.concat (intersperse "," $ map renderKV xs)
                                 , "]'"
                                 ]
    GremlinList vs  -> T.concat ["'[ "
                                 , T.concat (intersperse "," $ map encodeGremlinValue vs)
                                 , " ]'"
                                 ]
  where renderKV (k,v) = encodeQuoteText k <> " : " <> encodeGremlinValue v
-}

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
                else threadDelay 1000 >> mutexDec m


