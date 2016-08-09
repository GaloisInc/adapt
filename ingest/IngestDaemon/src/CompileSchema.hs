{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE ParallelListComp  #-}
module CompileSchema
  ( -- * Operations
    compile, compileNode, compileEdge, serializeOperation, serializeOperations
    -- * Types
    , GremlinValue(..)
    , Operation(..)
  ) where

import qualified Data.Aeson as A
import           Data.Binary (encode,decode)
import qualified Data.ByteString as BS
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Lazy as ByteString
import           Data.Char (isUpper)
import qualified Data.Char as C
import           Data.Foldable as F
import           Data.Int (Int64)
import           Data.List (intersperse)
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (catMaybes)
import           Data.Monoid
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import           Data.Word
import           Schema hiding (Env)
import           System.Entropy (getEntropy)

-- Operations represent gremlin-groovy commands such as:
-- assume: graph = TinkerGraph.open()
--         g = graph.traversal(standard())
data Operation id = InsertVertex { vertexType :: Text
                                 , ident      :: Text
                                 , properties :: [(Text,GremlinValue)]
                                 }
                  | InsertEdge { ident            :: Text
                               , src,dst          :: id
                               , generateVertices :: Bool
                               }
                  | InsertReifiedEdge
                            { labelNode :: Text
                            , labelE1   :: Text
                            , labelE2   :: Text
                            , nodeIdent,src,dst :: id
                            }
  deriving (Eq,Ord,Show)

data GremlinValue = GremlinNum Integer
                  | GremlinBytess [[Word8]]
                  | GremlinString Text
                  | GremlinList [GremlinValue]
                  | GremlinMap [(Text, GremlinValue)]
  deriving (Eq,Ord,Show)

instance A.ToJSON GremlinValue where
  toJSON gv =
    case gv of
      GremlinNum i     -> A.toJSON i
      GremlinBytess xs -> A.toJSON xs
      GremlinString s -> A.toJSON s
      -- XXX maps and lists are only notionally supported
      GremlinMap xs   -> A.toJSON (Map.fromList xs)
      GremlinList vs  -> A.toJSON vs


compile :: ([Node], [Edge]) -> IO [Operation Text]
compile (ns,es) =
  do let vertOfNodes = concatMap compileNode ns
     (concat -> vertOfEdges, concat -> edgeOfEdges) <- unzip <$> mapM compileEdge es
     pure $ concat [vertOfNodes , vertOfEdges , edgeOfEdges]

compileNode :: Node -> [Operation Text]
compileNode n = [InsertVertex ty (nodeUID_base64 n) props]
 where
  (ty,props) = propertiesAndTypeOf n

compileEdge :: Edge -> IO ([Operation Text], [Operation Text])
compileEdge e =
  do euid <- newUID
     let e1Lbl = vLbl <> " out"
         e2Lbl = vLbl <> " in"
         eNode   = uidToBase64 euid
         [esrc, edst] = map uidToBase64 [edgeSource e, edgeDestination e]
         fixCamelCase str =
           let go x | isUpper x = T.pack ['_' , x]
                    | otherwise = T.pack [C.toUpper x]
           in T.take 1 str <> T.concatMap go (T.drop 1 str)
         vLbl  = fixCamelCase $ T.pack $ show (edgeRelationship e)
         eTriple = InsertReifiedEdge vLbl e1Lbl e2Lbl eNode esrc edst
     return ([], [eTriple])

class PropertiesOf a where
  propertiesOf :: a -> [(Text,GremlinValue)]

class PropertiesAndTypeOf a where
  propertiesAndTypeOf :: a -> (Text,[(Text,GremlinValue)])

instance PropertiesAndTypeOf Node where
  propertiesAndTypeOf node =
   case node of
    NodeEntity entity     -> propertiesAndTypeOf entity
    NodeResource resource -> propertiesAndTypeOf resource
    NodeSubject subject   -> propertiesAndTypeOf subject
    NodeHost host         -> propertiesAndTypeOf host
    NodeAgent agent       -> propertiesAndTypeOf agent

enumOf :: Enum a => a -> GremlinValue
enumOf = GremlinNum . fromIntegral . fromEnum

mkSource :: InstrumentationSource -> (Text,GremlinValue)
mkSource = ("source",) . enumOf

mayAppend :: Maybe (Text,GremlinValue) -> [(Text,GremlinValue)] -> [(Text,GremlinValue)]
mayAppend Nothing  = id
mayAppend (Just x) = (x :)

instance PropertiesOf OptionalInfo where
  propertiesOf (Info {..}) =
        catMaybes [ ("time",) . gremlinTime <$> infoTime
                  , ("permissions",)  . gremlinNum <$> infoPermissions
          ] <> propertiesOf infoOtherProperties

instance PropertiesAndTypeOf Entity where
  propertiesAndTypeOf e =
   case e of
      File {..} -> ("Entity-File"
                   , mkSource entitySource
                     : ("url", GremlinString entityURL)
                     : ("file-version", gremlinNum entityFileVersion)
                     : mayAppend ( (("size",) . gremlinNum) <$> entityFileSize)
                                 (propertiesOf entityInfo)
                   )
      NetFlow {..} -> 
                ("Entity-NetFlow"
                , mkSource entitySource
                  : ("srcAddress", GremlinString entitySrcAddress)
                  : ("dstAddress", GremlinString entityDstAddress)
                  : ("srcPort", gremlinNum entitySrcPort)
                  : ("dstPort", gremlinNum entityDstPort)
                  : mayAppend ( (("ipProtocol",) . gremlinNum) <$> entityIPProtocol)
                    (propertiesOf entityInfo)
                )
      Memory {..} ->
               ("Entity-Memory"
               , mkSource entitySource
                 : maybe id (\p -> (("pageNumber", gremlinNum p):)) entityPageNumber
                 ( ("address", gremlinNum entityAddress)
                 : propertiesOf entityInfo
                 )
               )

instance PropertiesAndTypeOf Resource where
  propertiesAndTypeOf (Resource {..}) =
               ("Resource"
               , ("srcSinkType", enumOf resourceSource)
                 : propertiesOf resourceInfo
               )

instance PropertiesOf SubjectType where
  propertiesOf s = [("subjectType", gremlinNum (fromEnum s))]

instance PropertiesOf EventType where
  propertiesOf s = [("eventType", gremlinNum (fromEnum s))]

gremlinTime :: Int64 -> GremlinValue
gremlinTime = gremlinNum

gremlinList :: [Text] -> GremlinValue
gremlinList = GremlinList . map GremlinString

gremlinArgs :: [BS.ByteString] -> GremlinValue
gremlinArgs = GremlinBytess . map BS.unpack

instance PropertiesOf a => PropertiesOf (Maybe a) where
  propertiesOf Nothing  = []
  propertiesOf (Just x) = propertiesOf x

instance PropertiesAndTypeOf Subject where
  propertiesAndTypeOf (Subject {..}) =
              ("Subject"
              , mkSource subjectSource
                : maybe id (\s -> (("startedAtTime", gremlinTime s) :)) subjectStartTime
                ( concat
                   [ propertiesOf subjectType
                   , propertiesOf subjectEventType
                   , F.toList (("sequence"   ,) . gremlinNum <$> subjectEventSequence)
                   , F.toList (("pid"        ,) . gremlinNum    <$> subjectPID        )
                   , F.toList (("ppid"       ,) . gremlinNum    <$> subjectPPID       )
                   , F.toList (("unitid"     ,) . gremlinNum    <$> subjectUnitID     )
                   , F.toList (("endedAtTime"    ,) . gremlinTime   <$> subjectEndTime    )
                   , F.toList (("commandLine",) . GremlinString <$> subjectCommandLine)
                   , F.toList (("importLibs" ,) . gremlinList   <$> subjectImportLibs )
                   , F.toList (("exportLibs" ,) . gremlinList   <$> subjectExportLibs )
                   , F.toList (("pInfo",) . GremlinString <$> subjectProcessInfo)
                   , F.toList (("location"   ,) . gremlinNum    <$> subjectLocation   )
                   , F.toList (("size"       ,) . gremlinNum    <$> subjectSize       )
                   , F.toList (("ppt"        ,) . GremlinString <$> subjectPpt        )
                   , F.toList (("env"        ,) . GremlinMap . propertiesOf  <$> subjectEnv)
                   , F.toList (("args"       ,) . gremlinArgs   <$> subjectArgs       )
                   , propertiesOf subjectOtherProperties
                   ])
               )

instance PropertiesOf (Map Text Text) where
  propertiesOf x
    | Map.null x = []
    | otherwise  = [("properties", GremlinMap (map (\(a,b) -> (a,GremlinString b)) (Map.toList x)))]

instance PropertiesAndTypeOf Host  where
  propertiesAndTypeOf (Host {..}) =
          ( "Host"
          , catMaybes [ mkSource <$> hostSource
                      , ("hostIP",) . GremlinString <$> hostIP
                      ]
          )

instance PropertiesAndTypeOf Agent where
  propertiesAndTypeOf (Agent {..}) =
      ("Agent"
      , ("userID", GremlinString agentUserID)
        : concat [ gidProps agentGID
                 , F.toList (("agentType",) . enumOf <$> agentType)
                 , F.toList (mkSource <$> agentSource)
                 , propertiesOf agentProperties
                 ]
      )

gidProps :: Maybe GID -> [(Text, GremlinValue)]
gidProps Nothing = []
gidProps (Just xs) = map (("gid",) . GremlinString) xs

nodeUID_base64 :: Node -> Text
nodeUID_base64 = uidToBase64 . nodeUID

uidToBase64 :: UID -> Text
uidToBase64 = T.decodeUtf8 . B64.encode . ByteString.toStrict . encode

newUID :: IO UID
newUID = (decode . ByteString.fromStrict) <$> getEntropy (8 * 4)

gremlinNum :: Integral i => i -> GremlinValue
gremlinNum = GremlinNum . fromIntegral

--------------------------------------------------------------------------------
--  Gremlin language serialization

serializeOperations :: [Operation Text] -> (Text,Env)
serializeOperations ops =
  let (cmds,envs) = unzip $ map (uncurry serializeOperationFrom) (zip [1,1001..] ops)
  in (T.intercalate ";" cmds, Map.unions envs)

serializeOperation :: Operation Text -> (Text,Env)
serializeOperation = serializeOperationFrom 0

type Env = Map.Map Text A.Value

serializeOperationFrom :: Int -> Operation Text -> (Text,Env)
serializeOperationFrom start (InsertVertex ty l ps) = (cmd,env)
    where
       cmd = escapeChars call
       -- graph.addVertex(label, tyParam, 'ident', vertexName, param1, val1, param2, val2 ...)
       call = T.unwords
                [ "graph.addVertex(label, " <> tyParamVar <> ", 'ident', " <> identVar
                , if (not (null ps)) then "," else ""
                , T.unwords $ intersperse "," (map mkParams [start+2..start+1+length ps])
                , ")"
                ]
       tyParamVar = "tyParam" <> T.pack (show start)
       identVar   = "l" <> T.pack (show (start+1))
       env = Map.fromList $ (tyParamVar, A.String ty) : (identVar, A.String l) : mkBinding (start+2) ps

serializeOperationFrom start (InsertReifiedEdge  lNode lE1 lE2 nId srcId dstId) = (cmd,env)
    where
    cmd = escapeChars call
    call = T.unwords
            [ "edgeNode = graph.addVertex(label, " <> tyParamVar <> ", 'ident', " <> identVar <> ") ; "
            , "g.V().has('ident'," <> srcVar <> ").next().addEdge(" <> lE1Var <> ",edgeNode) ; "
            , "edgeNode.addEdge(" <> lE2Var <> ", g.V().has('ident', " <> dstVar <> ").next())"
            ]
    tyParamVar = "tyParam" <> T.pack (show $ start + 0)
    identVar   = "nId"     <> T.pack (show $ start + 1)
    srcVar     = "srcId"   <> T.pack (show $ start + 2)
    dstVar     = "dstId"   <> T.pack (show $ start + 3)
    lE1Var     = "lE1"     <> T.pack (show $ start + 4)
    lE2Var     = "lE2"     <> T.pack (show $ start + 5)
    env = Map.fromList [ (tyParamVar, A.String lNode)
                       , (lE1Var, A.String lE1), (lE2Var, A.String lE2)
                       , (identVar, A.String nId)
                       , (srcVar, A.String srcId), (dstVar, A.String dstId)
                       ]

serializeOperationFrom start (InsertEdge l src dst genVerts)   =
     if genVerts
      then (testAndInsertCmd, env)
      else (nonTestCmd, env)
    where
      -- g.V().has('ident',src).next().addEdge(edgeTy, g.V().has('ident',dst).next(), param1, val1, ...)
      nonTestCmd =
        escapeChars $
            "g.V().has('ident'," <> srcVar <> ").next().addEdge(" <> edgeTyVar <> ", g.V().has('ident'," <> dstVar <> ").next())"
      -- x = g.V().has('ident',src)
      -- y = g.V().has('ident',dst)
      -- if (!x.hasNext()) { x = g.addV('ident',src) }
      -- if (!y.hasNext()) { y = g.addV('ident',dst) }
      -- x.next().addEdge(edgeName, y.next())
      testAndInsertCmd = escapeChars $
             T.unwords
              [ "x = g.V().has('ident'," <> srcVar <> ") ;"
              , "y = g.V().has('ident'," <> dstVar <> ") ;"
              , "if (!x.hasNext()) { x = g.addV('ident'," <> srcVar <> ") } ;"
              , "if (!y.hasNext()) { y = g.addV('ident'," <> dstVar <> ") } ;"
              , "x.next().addEdge(" <> edgeTyVar <> ", y.next())"
              ]
      srcVar    = "srcVar"    <> T.pack (show $ start + 0)
      dstVar    = "dstVar"    <> T.pack (show $ start + 1)
      edgeTyVar = "edgeTyVar" <> T.pack (show $ start + 2)
      env = Map.fromList [ (srcVar, A.String src)
                         , (dstVar, A.String dst)
                         , (edgeTyVar, A.String l)
                         ]

mkBinding :: Int -> [(Text, GremlinValue)] -> [(Text, A.Value)]
mkBinding start pvs =
  let lbls = [ (param n, val n) | n <- [start..] ]
  in concat [ [(pstr, A.String p), (vstr, A.toJSON v)]
                    | (pstr,vstr) <- lbls
                    | (p,v) <- pvs ]

-- Build string "param1, val1, param2, val2, ..."
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
