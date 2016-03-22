{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# OPTIONS_GHC -Wall #-}
module Titan
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  -- , ErrorHandle(..)
  -- , defaultErrorHandle
  , Operation(..)
  -- , HostName
  , ResultId(..)
  , GraphId(..)
    -- * Insertion
  , titan
  , TitanResult(..)
    -- * Query
  ) where

-- import Network.Socket as Net
-- import Network.HTTP.Types
-- import Network.HTTP.Client
-- import Network.HTTP.Client.TLS

import Crypto.Hash.SHA256 (hash)
import qualified Data.ByteString.Base64 as B64

import Data.List (intersperse)
import Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import Data.ByteString (ByteString)
import qualified Data.ByteString.Lazy as BL
import qualified Data.ByteString.Char8 as BC
import qualified Data.Set as Set
import qualified Data.Map as Map
import Data.Aeson (Value(..), FromJSON(..), ToJSON(..), (.:), (.=))
import qualified Data.Aeson as A
import qualified Data.Aeson.Types as A

import Network.WebSockets as WS

data ServerInfo = ServerInfo { host     :: String
                             , port     :: Int
                             }
        deriving (Eq,Ord,Show)

defaultServer :: ServerInfo
defaultServer = ServerInfo "localhost" 8182


-- Operations represent gremlin-groovy commands such as:
-- assume: g = TinkerGraph.open()
--         t = g.traversal(standard())
-- * InsertVertex: g.addVertex(label, 'ident', 'prop1', 'val1', 'prop2', 'val2')
-- * InsertEdge: t.V(1).addE('ident',t.V(2))
data Operation id = InsertVertex { label :: Text
                                 , properties :: [(Text,Text)]
                                 }
                  | InsertEdge { label      :: Text
                               , src,dst    :: id
                               , properties :: [(Text,Text)]
                               }
  deriving (Eq,Ord,Show)

-- data ErrorHandle id = Custom (HttpException -> ServerInfo -> Operation id -> IO TitanResult)
--                     | Silent    -- ^ Silently convert exceptions to Either types
--                     | ToStderr  -- ^ Emit a stderr message and convert exceptions to Either types
--                     | Rethrow   -- ^ re-throw exceptions

-- defaultErrorHandle :: GraphId id => ErrorHandle id
-- defaultErrorHandle = Custom $ \x si t ->
--     do case x of
--               StatusCodeException {} -> return (Failure x)
--               ResponseTimeout        -> titanWith ToStderr si t -- Single retry on failure
--               _                      -> X.throw x

newtype ResultId = ResultId Text
                   deriving (Eq, Ord, Show)
instance FromJSON ResultId where
  parseJSON (Object o) = do
    Object r <- o.:"result"
    [Object d] <- r.:"data"
    i <- d.:"id"
    case i of
      String t -> return (ResultId t)
      Number {} -> do n <- parseJSON i
                      return (ResultId (T.pack (show (n::Int))))
      _        -> fail "No id"
  parseJSON _ = fail "Couldn't parse JSON"

data TitanResult = Success

titan :: GraphId id => ServerInfo -> [Operation id] -> IO TitanResult
titan (ServerInfo {..}) t = WS.runClient host port "/" (titanWS t)

titanWS :: GraphId id => [Operation id] -> WS.ClientApp TitanResult
titanWS ops conn =
   do mapM_ go ops
      return Success
 where
 go :: GraphId id => Operation id -> IO ()
 go = send conn . mkGremlinWSCommand . T.decodeUtf8 . serializeOperation

 mkGremlinWSCommand :: Text -> Message
 mkGremlinWSCommand cmd = DataMessage $ Text $ mkJSON cmd

 mkJSON :: Text -> BL.ByteString
 mkJSON cmd =
    A.encode
      Req { requestId = T.decodeUtf8 $ B64.encode $ hash (T.encodeUtf8 cmd)
          , op        = "eval"
          , processor = ""
          , args = ReqArgs { gremlin  = cmd
                           , language = "gremlin-groovy"
                           }
          }



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
  serializeOperation (InsertVertex l ps) = gremlinScript $ BC.concat [call, args]
    where
       -- g.addV(label, vectorName, param1, val1, param2, val2 ...)
      call = "g.addV"
      args = paren $ BC.concat $ intersperse "," ("label" : encodeQuoteText l : concatMap attrs ps)
  serializeOperation (InsertEdge l src dst ps)   = gremlinScript call
    where
       -- g.V().has(label,src).next().addE(edgeName, g.V().has(label,dst).next(), param1, val1, ...)
       call = BC.concat $ ["g.V().has(label,", encodeQuoteText src
                          , ").next().addEdge(", encodeQuoteText l
                                          , ", g.V().has(label,", encodeQuoteText dst, ").next(), "
                              ] ++ args ++ [")"]
       args = intersperse "," (concatMap attrs ps)
instance GraphId ResultId where
  serializeOperation (InsertVertex l ps) = gremlinScript $ BC.concat [call, args]
    where
      call = "g.addV"
      args = paren $ BC.concat $ intersperse "," ("label" : encodeQuoteText l : concatMap attrs ps)
  serializeOperation (InsertEdge l (ResultId src) (ResultId dst) ps) = gremlinScript call
    where
      call = BC.concat $ ["g.V(", T.encodeUtf8 src, ").next().addEdge(", encodeQuoteText l , ", g.V(", T.encodeUtf8 dst, ").next(), "] ++ args ++ [")"]
      args = intersperse "," (concatMap attrs ps)

gremlinScript :: ByteString -> ByteString
gremlinScript s =
  BC.concat [ "{ \"gremlin\" : \""
             , escapeChars s, "\""
             , " }"]

encodeQuoteText :: Text -> ByteString
encodeQuoteText = quote . subChars . escapeChars . T.encodeUtf8

quote :: ByteString -> ByteString
quote b = BC.concat ["\'", b, "\'"]

paren :: ByteString -> ByteString
paren b = BC.concat ["(", b, ")"]

attrs :: (Text,Text) -> [ByteString]
attrs (a,b) = [encodeQuoteText a, encodeQuoteText b]

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
