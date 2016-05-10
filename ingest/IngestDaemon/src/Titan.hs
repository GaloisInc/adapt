{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# OPTIONS_GHC -Wall #-}
module Titan
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  , Operation(..)
    -- * High level interface
  , withTitan, Titan.send
  , DataMessage(..), ControlMessage(..), Message(..)
    -- * Insertion
  , titan
  , TitanResult(..), isSuccess, isFailure
    -- * Lower Level
  , titanWS
  , Connection
  , ResultId(..)
  , GraphId(..)
  , GremlinValue(..)
  ) where

import           Control.Concurrent (forkIO)
import           Control.Concurrent.Async (async,wait)
import           Control.Monad (forever)
import           Crypto.Hash.SHA256 (hash)
import           Data.Aeson (Value(..), FromJSON(..), ToJSON(..), (.:), (.=))
import qualified Data.Aeson as A
import qualified Data.Aeson.Types as A
import           Data.ByteString (ByteString)
import qualified Data.ByteString.Char8 as BC
import qualified Data.ByteString.Lazy as BL
import           Data.List (intersperse)
import           Data.List.Split (chunksOf)
import qualified Data.Map as Map
import           Data.Monoid ((<>))
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.UUID as UUID
import           Network.WebSockets as WS
import           Control.Exception as X

import CompileSchema

data ServerInfo = ServerInfo { host     :: String
                             , port     :: Int
                             }
        deriving (Eq,Ord,Show)

defaultServer :: ServerInfo
defaultServer = ServerInfo "localhost" 8182

labelKey :: ByteString
labelKey = "id"

newtype ResultId = ResultId Text
                   deriving (Eq, Ord, Show)

instance FromJSON ResultId where
  parseJSON (Object o) = do
    Object r   <- o.:"result"
    [Object d] <- r.:"data"
    i <- d.:"id"
    case i of
      String t -> return (ResultId t)
      Number {} -> do n <- parseJSON i
                      return (ResultId (T.pack (show (n::Int))))
      _        -> fail "No id"
  parseJSON _ = fail "Couldn't parse JSON"

data TitanResult = Success [DataMessage] | Failure Int BL.ByteString

isSuccess, isFailure :: TitanResult -> Bool
isSuccess (Success _) = True
isSuccess _ = False
isFailure = not . isSuccess

batchSize :: Int
batchSize = 500

-- The Titan monad is just a websocket.
type Titan a = WS.ClientApp a

withTitan :: forall a. ServerInfo ->  (Message -> Titan ()) -> Titan a -> IO (Either ConnectionException a)
withTitan (ServerInfo {..}) recvHdl mainOper =
   X.catch (WS.runClient host port "/" (fmap Right . loop))
           (return . Left)
 where
 loop :: WS.ClientApp a
 loop conn = do _ <- forkIO (forever (receive conn >>= flip recvHdl conn))
                mainOper conn

send :: GraphId id => Operation id -> Titan ()
send op conn = WS.send conn (mkGremlinWSCommand (T.decodeUtf8 (serializeOperation op)))

-- `titan server ops` is like `titanWS` except it batches the operations
-- into sets of no more than `batchSize`.  This is to avoid the issue with
-- titan write buffer filling up then blocking while evaluating an
-- insertion which causes a timeout.
titan :: GraphId id => ServerInfo -> [Operation id] -> IO [TitanResult]
titan (ServerInfo {..}) xs =
 do let xss = chunksOf batchSize xs
    mapM (\x -> X.catch (WS.runClient host port "/" (titanWS x))
                        (\(_e::ConnectionException) ->
                             do putStrLn "runClient exception"
                                return (Failure (-1) BL.empty)))
         xss

titanWS :: GraphId id => [Operation id] -> WS.ClientApp TitanResult
titanWS ops conn =
   do result <- async (receiveTillClose (length ops) [])
      mapM_ go ops
      wait result
 where
 receiveTillClose n acc
  | n <= 0    =
   do sendClose conn BL.empty
      return (Success (reverse acc))
  | otherwise =
   do msg <- receive conn
      case msg of
        ControlMessage (Close code reason)
            | code == 1000 -> return $ Success (reverse acc)
            | otherwise    -> return $ Failure (fromIntegral code) reason
        ControlMessage _   -> receiveTillClose n acc
        DataMessage _   -> receiveTillClose (n-1) acc

 go :: GraphId id => Operation id -> IO ()
 go = WS.send conn . mkGremlinWSCommand . T.decodeUtf8 . serializeOperation

mkGremlinWSCommand :: Text -> Message
mkGremlinWSCommand cmd = DataMessage $ Text (mkJSON cmd)

mkJSON :: Text -> BL.ByteString
mkJSON cmd =
    A.encode
      Req { requestId = UUID.toText (mkHashedUUID cmd)
          , op        = "eval"
          , processor = ""
          , args = ReqArgs { gremlin  = cmd
                           , language = "gremlin-groovy"
                           }
          }

mkHashedUUID :: Text -> UUID.UUID
mkHashedUUID t =
  maybe (error "Impossible uuid decode failure") id
        (UUID.fromByteString (BL.take 16 (BL.fromStrict (hash (T.encodeUtf8 t)))))

--------------------------------------------------------------------------------
--  Gremlin WebSockets JSON API

data GremlinRequest =
  Req { requestId :: Text
      , op        :: Text
      , processor :: Text
      , args      :: RequestArgs
      }

data RequestArgs =
  ReqArgs { gremlin    :: Text
          , language   :: Text
          }

instance ToJSON RequestArgs where
  toJSON (ReqArgs g l) =
    A.object [ "gremlin"  .= g
             , "language" .= l
             , "bindings" .= A.Null
             , "rebindings" .= A.emptyObject
             ]

instance ToJSON GremlinRequest where
  toJSON (Req i o p a) =
    A.object [ "requestId" .= i
             , "op"        .= o
             , "processor" .= p
             , "args"      .= a
             ]

instance FromJSON RequestArgs where
  parseJSON (A.Object obj) =
      ReqArgs <$> obj .: "gremlin"
              <*> obj .: "language"
  parseJSON j = A.typeMismatch "RequestArgs" j

instance FromJSON GremlinRequest where
  parseJSON (A.Object obj) =
      Req <$> obj .: "requestId"
          <*> obj .: "op"
          <*> obj .: "processor"
          <*> obj .: "args"
  parseJSON j = A.typeMismatch "GremlinRequest" j


--------------------------------------------------------------------------------
--  Gremlin language serialization

class GraphId a where
  serializeOperation :: Operation a -> ByteString

instance GraphId Text where
  serializeOperation (InsertVertex l ps) = escapeChars $ BC.concat [call, args]
    where
       -- g.addV(id, vectorName, param1, val1, param2, val2 ...)
      call = "g.addV"
      args = paren $ BC.concat $ intersperse "," (labelKey : encodeQuoteText l : concatMap attrs ps)
  serializeOperation (InsertEdge l src dst ps)   = escapeChars call
    where
       -- g.V(src).next().addEdge(edgeName, g.V(dst).next(), param1, val1, ...)
       call = BC.concat $ ["g.V(", encodeQuoteText src
                          , ").next().addEdge(", encodeQuoteText l
                                          , ", g.V(", encodeQuoteText dst, ").next(), "
                              ] ++ args ++ [")"]
       args = intersperse "," (concatMap attrs ps)
instance GraphId ResultId where
  serializeOperation (InsertVertex l ps) = escapeChars $ BC.concat [call, args]
    where
      call = "g.addV"
      args = paren $ BC.concat $ intersperse "," (labelKey : encodeQuoteText l : concatMap attrs ps)
  serializeOperation (InsertEdge l (ResultId src) (ResultId dst) ps) = escapeChars call
    where
      call = BC.concat $ ["g.V(", T.encodeUtf8 src, ").next().addEdge(", encodeQuoteText l , ", g.V(", T.encodeUtf8 dst, ").next(), "] ++ args ++ [")"]
      args = intersperse "," (concatMap attrs ps)

encodeQuoteText :: Text -> ByteString
encodeQuoteText = quote . subChars . escapeChars . T.encodeUtf8

encodeGremlinValue :: GremlinValue -> ByteString
encodeGremlinValue gv =
  case gv of
    GremlinString s -> encodeQuoteText s
    GremlinNum  n   -> BC.pack (show n)
    GremlinMap xs   -> BC.concat ["[ "
                                 , BC.concat (intersperse "," $ map renderKV xs)
                                 , " ]"
                                 ]
    GremlinList vs  -> BC.concat ["[ "
                                 , BC.concat (intersperse "," $ map encodeGremlinValue vs)
                                 , " ]"
                                 ]
  where renderKV (k,v) = encodeQuoteText k <> " : " <> encodeGremlinValue v

quote :: ByteString -> ByteString
quote b = BC.concat ["\'", b, "\'"]

paren :: ByteString -> ByteString
paren b = BC.concat ["(", b, ")"]

attrs :: (Text,GremlinValue) -> [ByteString]
attrs (a,b) = [encodeQuoteText a, encodeGremlinValue b]

escapeChars :: ByteString -> ByteString
escapeChars b
  | not (BC.any (`Set.member` escSet) b) = b
  | otherwise = BC.concatMap (\c -> if c `Set.member` escSet then BC.pack ['\\', c] else BC.singleton c) b

escSet :: Set.Set Char
escSet = Set.fromList ['\\', '"']

subChars :: ByteString -> ByteString
subChars b
  | not (BC.any (`Set.member` badChars) b) = b
  | otherwise = BC.map (\c -> maybe c id (Map.lookup c charRepl)) b

charRepl :: Map.Map Char Char
charRepl = Map.fromList [('\t',' ')]

badChars :: Set.Set Char
badChars = Map.keysSet charRepl
