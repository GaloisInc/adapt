{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# OPTIONS_GHC -Wall #-}
module Titan
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  , ErrorHandle(..)
  , defaultErrorHandle
  , Operation(..)
  , HostName
  , ResultId(..)
  , GraphId(..)
    -- * Insertion
  , titan
  , titanWith
  , TitanResult(..)
    -- * Query
  ) where

import Network.Socket as Net
import Network.HTTP.Types
import Network.HTTP.Client
import Network.HTTP.Client.TLS

import qualified Control.Exception as X
import Data.List (intersperse)
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as T
import qualified Data.Text.Lazy.Encoding as T
import Data.String
import Data.ByteString.Lazy (ByteString)
import qualified Data.ByteString.Lazy.Char8 as BC
import qualified Data.Set as Set
import qualified Data.Map as Map
import System.IO (stderr, hPutStr)
import qualified Data.ByteString.Char8 as BCNOTL
import Data.Aeson (decode, Value(..), FromJSON(..), (.:))
import Debug.Trace

newtype ServerInfo = ServerInfo { host     :: HostName
                             }
        deriving (Eq,Ord,Show)

instance IsString ServerInfo where
  fromString = ServerInfo

defaultServer :: ServerInfo

defaultServer = ServerInfo "http://localhost:8182"


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

data ErrorHandle id = Custom (HttpException -> ServerInfo -> Operation id -> IO TitanResult)
                    | Silent    -- ^ Silently convert exceptions to Either types
                    | ToStderr  -- ^ Emit a stderr message and convert exceptions to Either types
                    | Rethrow   -- ^ re-throw exceptions

defaultErrorHandle :: GraphId id => ErrorHandle id
defaultErrorHandle = Custom $ \x si t ->
    do case x of
              StatusCodeException {} -> return (Failure x)
              ResponseTimeout        -> titanWith ToStderr si t -- Single retry on failure
              _                      -> X.throw x

newtype ResultId = ResultId Text
                   deriving (Eq, Ord, Show)
instance FromJSON ResultId where
  parseJSON (Object o) = do
    Object r <- o.:"result"
    [Object d] <- r.:"data"
    i <- d.:"id"
    case i of
      String t -> return (ResultId (T.fromStrict t))
      Number {} -> do n <- parseJSON i
                      return (ResultId (T.pack (show (n::Int))))
      _        -> fail "No id"
  parseJSON _ = fail "Couldn't parse JSON"

data TitanResult = Success Status ResultId | Failure HttpException | ParseError

titan :: GraphId id => ServerInfo -> Operation id -> IO TitanResult
titan = titanWith defaultErrorHandle

titanWith :: GraphId id => ErrorHandle id -> ServerInfo -> Operation id -> IO TitanResult
titanWith eh si@(ServerInfo {..}) t =
 do res <-  X.try doRequest
    case res of
       Right s -> return s
       Left  httpException ->
          case eh of
            Custom f -> f httpException si t
            Silent   -> return (Failure httpException)
            ToStderr -> do hPutStr stderr (show httpException)
                           return (Failure httpException)
            Rethrow  -> X.throw httpException


 where
  doRequest =
    do req <- parseUrl host
       let req' = req { method      = "POST"
                      , requestBody = RequestBodyLBS (serializeOperation t)
                      , requestHeaders = [(hUserAgent, "Adapt/Trint")
                                         ,(hAccept,"*/*")
                                         ,(hContentType,  "application/x-www-form-urlencoded")
                                         ,("Accept-Encoding", "")
                                         ]
                      }
       m <- newManager tlsManagerSettings
       withResponse req' m handleResponse
  handleResponse :: Response BodyReader -> IO TitanResult
  handleResponse x = do
    bs <- responseBody x
    case decode (BC.fromStrict bs) of
      Just i -> return (Success (responseStatus x) i) 
      Nothing -> return ParseError

-- The generation of Gremlin code is currently done through simple (sinful)
-- concatenation. A better solution would be a Haskell Gremlin Language library
-- and quasi quoter.
{-
serializeOperation :: Operation -> ByteString
serializeOperation (InsertVertex l ps) = gremlinScript $ BC.concat [call, args]
  where
     -- g.addV(label, vectorName, param1, val1, param2, val2 ...)
    call = "g.addV"
    args = paren $ BC.concat $ intersperse "," ("label" : encodeQuoteText l : concatMap attrs ps)
--serializeOperation (InsertEdge l src dst ps)   = gremlinScript call
  where
     -- g.V(id).addE(edgeName, g.V(otherId), param1, val1, ...)
     -- g.V().has(label,src).next().addE(edgeName, g.V().has(label,dst).next(), param1, val1, ...)
     call = BC.concat $ ["g.V().has(label,", encodeQuoteText src
                        , ").next().addEdge(", encodeQuoteText l
                                        , ", g.V().has(label,", encodeQuoteText dst, ").next(), "
                            ] ++ args ++ [")"]
     args = intersperse "," (concatMap attrs ps)-}
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
  serializeOperation (InsertEdge l (ResultId src) (ResultId dst) ps) = traceShowId $ gremlinScript call
    where
      call = BC.concat $ ["g.V(", T.encodeUtf8 src, ").addE(", encodeQuoteText l , ", g.V(", T.encodeUtf8 dst, "), "] ++ args ++ [")"]
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
