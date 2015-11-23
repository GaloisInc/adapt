{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# OPTIONS_GHC -Wall #-}
module Titan
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  , Operation(..)
  , HostName
    -- * Insertion
  , titan
    -- * Query
  ) where

import Network.Socket as Net
import Network.HTTP.Types
import Network.HTTP.Client
import Network.HTTP.Client.TLS

import qualified Control.Exception as X
import Data.List (intersperse)
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy.Encoding as T
import Data.String
import Data.ByteString.Lazy (ByteString)
import qualified Data.ByteString.Lazy.Char8 as BC
import qualified Data.Set as Set
import qualified Data.Map as Map

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
data Operation = InsertVertex { label :: Text
                              , properties :: [(Text,Text)]
                              }
               | InsertEdge { label      :: Text
                            , src,dst    :: Text
                            , properties :: [(Text,Text)]
                            }
  deriving (Eq,Ord,Show)

titan :: ServerInfo -> Operation -> IO Status
titan (ServerInfo {..}) t =
 do X.catch
     (do req <- parseUrl host
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
      )
      (\e -> case e of (StatusCodeException s _ _) -> return s ; _ -> X.throw e)
 where
  handleResponse :: Response BodyReader -> IO Status
  handleResponse = return .  responseStatus

-- The generation of Gremlin code is currently done through simple (sinful)
-- concatenation. A better solution would be a Haskell Gremlin Language library
-- and quasi quoter.
serializeOperation :: Operation -> ByteString
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
