{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE ParallelListComp    #-}
{-# OPTIONS_GHC -Wall #-}
module Titan
  ( -- * Types
    ServerInfo(..)
  , defaultServer
  , Operation(..)
  , ResultId(..)
  , GraphId(..)
  , GremlinValue(..), gremlinNum
    -- * Insertion
  , titan
  , TitanResult(..), isSuccess, isFailure
  , DataMessage(..)
    -- * Lower Level
  , titanWS
  ) where

import           Control.Concurrent.Async (async,wait)
import           Crypto.Hash.SHA256 (hash)
import           Data.Aeson (Value(..), FromJSON(..), ToJSON(..), (.:), (.=))
import qualified Data.Aeson as A
import qualified Data.Aeson.Types as A
import           Data.ByteString (ByteString)
import qualified Data.ByteString.Base64 as B64
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

data ServerInfo = ServerInfo { host     :: String
                             , port     :: Int
                             }
        deriving (Eq,Ord,Show)

defaultServer :: ServerInfo
defaultServer = ServerInfo "localhost" 8182

labelKey :: ByteString
labelKey = "id"

-- Operations represent gremlin-groovy commands such as:
-- assume: graph = TinkerGraph.open()
--         g = graph.traversal(standard())
-- * InsertVertex: g.addVertex(id, 'ident', 'prop1', 'val1', 'prop2', 'val2')
-- * InsertEdge: t.V(id).addE(id,t.V(2))
data Operation id = InsertVertex { label :: Text
                                 , properties :: [(Text,GremlinValue)]
                                 }
                  | InsertEdge { label      :: Text
                               , src,dst    :: id
                               , properties :: [(Text,GremlinValue)]
                               }
  deriving (Eq,Ord,Show)

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

-- `titan server ops` is like `titanWS` except it batches the operations
-- into sets of no more than `batchSize`.  This is to avoid the issue with
-- titan write buffer filling up then blocking while evaluating an
-- insertion which causes a timeout.
titan :: GraphId id => ServerInfo -> [Operation id] -> IO [TitanResult]
titan (ServerInfo {..}) xs =
 do let xss = chunksOf batchSize xs
    mapM (\x -> X.catch (WS.runClient host port "/" (titanWS x))
                        (\(e::ConnectionException) ->
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
 go op =
   do let (cmd,bnd) = serializeOperation op
      send conn (mkGremlinWSCommand cmd bnd)

 mkGremlinWSCommand :: Text -> Env -> Message
 mkGremlinWSCommand cmd bnd = DataMessage $ Text (mkJSON cmd bnd)

 mkJSON :: Text -> Env -> BL.ByteString
 mkJSON cmd bnd =
    A.encode
      Req { requestId = UUID.toText (mkHashedUUID cmd)
          , op        = "eval"
          , processor = ""
          , args = ReqArgs { gremlin  = cmd
                           , bindings = bnd
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

type Env = Map.Map Text A.Value
data RequestArgs =
  ReqArgs { gremlin    :: Text
          , bindings   :: Env
          , language   :: Text
          }

instance ToJSON RequestArgs where
  toJSON (ReqArgs g b l) =
    A.object [ "gremlin"  .= g
             , "language" .= l
             , "bindings" .= b
             , "rebindings" .= A.emptyObject
             ]

instance ToJSON GremlinRequest where
  toJSON (Req i o p a) =
    A.object [ "requestId" .= i
             , "op"        .= o
             , "processor" .= p
             , "args"      .= a
             ]

instance ToJSON GremlinValue where
  toJSON gv =
    case gv of
      GremlinNum i    -> toJSON i
      GremlinString t -> toJSON t
      GremlinList l   -> toJSON l
      GremlinMap xs   -> toJSON (Map.fromList xs)

instance FromJSON RequestArgs where
  parseJSON (A.Object obj) =
      ReqArgs <$> obj .: "gremlin"
              <*> obj .: "bindings"
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
  serializeOperation :: Operation a -> (Text,Env)

instance GraphId Text where
  serializeOperation (InsertVertex l ps) = (cmd,env)
    where
      cmd = escapeChars call
       -- g.addV(id, vectorName, param1, val1, param2, val2 ...)
      call = T.unwords
              [ "g.addV(id, l, "
              , T.unwords $ intersperse "," (map mkParams [1..length ps])
              , ")"
              ]
      env = Map.fromList $ ("l", A.String l) : mkBinding ps
  serializeOperation (InsertEdge l src dst ps)   = (cmd,env)
    where
       cmd = escapeChars call
       -- g.V(dst).has(id,src).next().addE(edgeName, g.V().has(id,dst).next(), param1, val1, ...)
       call = T.unwords
               [ "g.V(dst).has(id,src).next().addE(edgeName, g.V().has(id,dst).next(), "
               , T.unwords $ intersperse "," (map mkParams [1..length ps])
               , ")"
               ]
       env = Map.fromList $ ("src",A.String src) : ("dst",A.String dst) :
                            ("edgeName", A.String l) : mkBinding ps

encodeQuoteText :: Text -> Text
encodeQuoteText = quote . subChars . escapeChars

data GremlinValue = GremlinNum Integer
                  | GremlinString Text
                  | GremlinList [GremlinValue]
                  | GremlinMap [(Text, GremlinValue)]
  deriving (Eq,Ord,Show)

gremlinNum :: Integral i => i -> GremlinValue
gremlinNum = GremlinNum . fromIntegral

encodeGremlinValue :: GremlinValue -> Text
encodeGremlinValue gv =
  case gv of
    GremlinString s -> escapeChars s
    GremlinNum  n   -> T.pack (show n)
    GremlinMap xs   -> error "Gremlin Map currently unsupported"
    GremlinList vs  -> error "Gremlin list currently unsupported"

quote :: Text -> Text
quote b = T.concat ["\'", b, "\'"]

paren :: Text -> Text
paren b = T.concat ["(", b, ")"]

mkBinding :: [(Text,GremlinValue)] -> [(Text,Value)]
mkBinding pvs =
  let lbls = [ (param n, val n) | n <- [1..length pvs] ]
  in concat [ [(pstr, String p), (vstr,toJSON v)]
                    | (pstr,vstr) <- lbls
                    | (p,v) <- pvs ]

-- Build string "param1, val1, param2, val2, ..., paramN, valN"
mkParams :: Int -> Text
mkParams n = T.concat [param n, ",", val n]

-- Construct the variable name for the nth parameter name.
param :: Int -> Text
param n = "param" <> T.pack (show n)

-- Construct the variable name for the Nth value
val :: Int -> Text
val n = "val" <> T.pack (show n)

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
