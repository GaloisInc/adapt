{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE FlexibleInstances #-}
module CompileSchema
  ( -- * Operations
    compile, compileNode, compileEdge
    -- * Types
    , GremlinValue(..)
    , Operation(..)
  ) where

import           Data.Binary (encode,decode)
import qualified Data.ByteString as BS
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Lazy as ByteString
import           Data.Char (isUpper)
import qualified Data.Char as C
import           Data.Foldable as F
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (catMaybes)
import           Data.Monoid
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import           Data.Time
import           Schema
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
                               , properties       :: [(Text,GremlinValue)]
                               , generateVertices :: Bool
                               }
  deriving (Eq,Ord,Show)

insertEdge :: Text -> id -> id -> [(Text,GremlinValue)] -> Operation id
insertEdge l s d p = InsertEdge l s d p False

data GremlinValue = GremlinNum Integer
                  | GremlinString Text
                  | GremlinList [GremlinValue]
                  | GremlinMap [(Text, GremlinValue)]
  deriving (Eq,Ord,Show)

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
         eMe   = uidToBase64 euid
         [esrc, edst] = map uidToBase64 [edgeSource e, edgeDestination e]
         fixCamelCase str =
           let go x | isUpper x = T.pack ['_' , x]
                    | otherwise = T.pack [C.toUpper x]
           in T.take 1 str <> T.concatMap go (T.drop 1 str)
         vLbl  = fixCamelCase $ T.pack $ show (edgeRelationship e) -- XXX Fix camel case vs snake case
         v     = InsertVertex vLbl eMe [("relationship", GremlinString vLbl)]
         eTo   = insertEdge e1Lbl esrc eMe []
         eFrom = insertEdge e2Lbl eMe edst []
     return ([v], [eTo, eFrom])

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
      File {..} -> ("file"
                   , mkSource entitySource
                     : ("url", GremlinString entityURL)
                     : ("file-version", gremlinNum entityFileVersion)
                     : mayAppend ( (("size",) . gremlinNum) <$> entityFileSize)
                                 (propertiesOf entityInfo)
                   )
      NetFlow {..} -> 
                ("netflow"
                , mkSource entitySource
                  : ("srcAddress", GremlinString entitySrcAddress)
                  : ("dstAddress", GremlinString entityDstAddress)
                  : ("srcPort", gremlinNum entitySrcPort)
                  : ("dstPort", gremlinNum entityDstPort)
                  : propertiesOf entityInfo
                )
      Memory {..} ->
               ("memory"
               , mkSource entitySource
                 : maybe id (\p -> (("pageNumber", gremlinNum p):)) entityPageNumber
                 ( ("address", gremlinNum entityAddress)
                 : propertiesOf entityInfo
                 )
               )

instance PropertiesAndTypeOf Resource where
  propertiesAndTypeOf (Resource {..}) =
               ("resource"
               , ("srcSinkType", enumOf resourceSource)
                 : propertiesOf resourceInfo
               )

instance PropertiesOf SubjectType where
  propertiesOf s =
   let subjTy = ("subjectType",) . GremlinString
   in case s of
       SubjectProcess    ->
         [subjTy "process"]
       SubjectThread     ->
         [subjTy "thread"]
       SubjectUnit       ->
         [subjTy "unit"]
       SubjectBlock      ->
         [subjTy "block"]
       SubjectEvent et sx  ->
         [subjTy "event", ("eventType", enumOf et) ]
          ++ F.toList ((("sequence",) . gremlinNum) <$> sx)

gremlinTime :: UTCTime -> GremlinValue
gremlinTime t = GremlinString (T.pack $ show t)

gremlinList :: [Text] -> GremlinValue
gremlinList = GremlinList . map GremlinString

gremlinArgs :: [BS.ByteString] -> GremlinValue
gremlinArgs = gremlinList . map T.decodeUtf8

instance PropertiesOf a => PropertiesOf (Maybe a) where
  propertiesOf Nothing  = []
  propertiesOf (Just x) = propertiesOf x

instance PropertiesAndTypeOf Subject where
  propertiesAndTypeOf (Subject {..}) =
              ("subject"
              , mkSource subjectSource
                : maybe id (\s -> (("startedAtTime", gremlinTime s) :)) subjectStartTime
                ( concat
                   [ propertiesOf subjectType
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
  propertiesOf x = [("properties", GremlinMap (map (\(a,b) -> (a,GremlinString b)) (Map.toList x)))]

instance PropertiesAndTypeOf Host  where
  propertiesAndTypeOf (Host {..}) =
          ( "host"
          , catMaybes [ mkSource <$> hostSource
                      , ("hostIP",) . GremlinString <$> hostIP
                      ]
          )

instance PropertiesAndTypeOf Agent where
  propertiesAndTypeOf (Agent {..}) =
      ("agent"
      , ("userID", gremlinNum agentUserID)
        : concat [ F.toList (("gid",) . GremlinList . map gremlinNum <$> agentGID)
                 , F.toList (("agentType",) . enumOf <$> agentType)
                 , F.toList (mkSource <$> agentSource)
                 , propertiesOf agentProperties
                 ]
      )

nodeUID_base64 :: Node -> Text
nodeUID_base64 = uidToBase64 . nodeUID

uidToBase64 :: UID -> Text
uidToBase64 = T.decodeUtf8 . B64.encode . ByteString.toStrict . encode

newUID :: IO UID
newUID = (decode . ByteString.fromStrict) <$> getEntropy (8 * 4)

gremlinNum :: Integral i => i -> GremlinValue
gremlinNum = GremlinNum . fromIntegral

