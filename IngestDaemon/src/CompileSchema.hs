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
data Operation id = InsertVertex { ident      :: Text
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
compileNode n = [InsertVertex (nodeUID_base64 n) (propertiesOf n)]

compileEdge :: Edge -> IO ([Operation Text], [Operation Text])
compileEdge e =
  do euid <- newUID
     let erel  = T.pack $ show (edgeRelationship e)
         e1Lbl = "src-" <> erel
         e2Lbl = "dst-" <> erel
         eMe   = uidToBase64 euid
         [esrc, edst] = map uidToBase64 [edgeSource e, edgeDestination e]
         v     = InsertVertex eMe [("relationship", GremlinString erel)]
         eTo   = insertEdge e1Lbl esrc eMe []
         eFrom = insertEdge e2Lbl eMe edst []
     return ([v], [eTo, eFrom])

class PropertiesOf a where
  propertiesOf :: a -> [(Text,GremlinValue)]

instance PropertiesOf Node where
  propertiesOf node =
   case node of
    NodeEntity entity     -> propertiesOf entity
    NodeResource resource -> propertiesOf resource
    NodeSubject subject   -> propertiesOf subject
    NodeHost host         -> propertiesOf host
    NodeAgent agent       -> propertiesOf agent

enumOf :: Enum a => a -> GremlinValue
enumOf = GremlinNum . fromIntegral . fromEnum

mkSource :: InstrumentationSource -> (Text,GremlinValue)
mkSource = ("source",) . enumOf

mkType :: Text -> (Text,GremlinValue)
mkType = ("type",) . GremlinString

mayAppend :: Maybe (Text,GremlinValue) -> [(Text,GremlinValue)] -> [(Text,GremlinValue)]
mayAppend Nothing  = id
mayAppend (Just x) = (x :)

instance PropertiesOf OptionalInfo where
  propertiesOf (Info {..}) =
        catMaybes [ ("time",) . gremlinTime <$> infoTime
                  , ("permissions",)  . gremlinNum <$> infoPermissions
          ] <> propertiesOf infoOtherProperties

instance PropertiesOf Entity where
  propertiesOf e =
   case e of
      File {..} ->   mkType "file"
                   : mkSource entitySource
                   : ("url", GremlinString entityURL)
                   : ("fileVersion", gremlinNum entityFileVersion)
                   : mayAppend ( (("fileSize",) . gremlinNum) <$> entityFileSize)
                               (propertiesOf entityInfo)
      NetFlow {..} -> 
                  mkType "netflow"
                : mkSource entitySource
                : ("srcAddress", GremlinString entitySrcAddress)
                : ("dstAddress", GremlinString entityDstAddress)
                : ("srcPort", gremlinNum entitySrcPort)
                : ("dstPort", gremlinNum entityDstPort)
                : propertiesOf entityInfo
      Memory {..} ->
                 mkType "memory"
               : mkSource entitySource
               : maybe id (\p -> (("pageNumber", gremlinNum p):)) entityPageNumber
               ( ("address", gremlinNum entityAddress)
               : propertiesOf entityInfo
               )

instance PropertiesOf Resource where
  propertiesOf (Resource {..}) =
                 mkType "resource"
               : mkSource resourceSource
               : propertiesOf resourceInfo
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

instance PropertiesOf Subject where
  propertiesOf (Subject {..}) =
                mkType "subject"
              : mkSource subjectSource
              : maybe id (\s -> (("startTime", gremlinTime s) :)) subjectStartTime
              ( concat
                 [ propertiesOf subjectType
                 , F.toList (("pid"        ,) . gremlinNum    <$> subjectPID        )
                 , F.toList (("ppid"       ,) . gremlinNum    <$> subjectPPID       )
                 , F.toList (("unitid"     ,) . gremlinNum    <$> subjectUnitID     )
                 , F.toList (("endtime"    ,) . gremlinTime   <$> subjectEndTime    )
                 , F.toList (("commandline",) . GremlinString <$> subjectCommandLine)
                 , F.toList (("importlibs" ,) . gremlinList   <$> subjectImportLibs )
                 , F.toList (("exportlibs" ,) . gremlinList   <$> subjectExportLibs )
                 , F.toList (("processinfo",) . GremlinString <$> subjectProcessInfo)
                 , F.toList (("location"   ,) . gremlinNum    <$> subjectLocation   )
                 , F.toList (("size"       ,) . gremlinNum    <$> subjectSize       )
                 , F.toList (("ppt"        ,) . GremlinString <$> subjectPpt        )
                 , F.toList (("env"        ,) . GremlinMap . propertiesOf  <$> subjectEnv)
                 , F.toList (("args"       ,) . gremlinArgs   <$> subjectArgs       )
                 , propertiesOf subjectOtherProperties
                 ])

instance PropertiesOf (Map Text Text) where
  propertiesOf = Map.toList . fmap GremlinString

instance PropertiesOf Host  where
  propertiesOf (Host {..}) =
          mkType "host" :
            catMaybes [ mkSource <$> hostSource
                      , ("hostIP",) . GremlinString <$> hostIP
                      ]

instance PropertiesOf Agent where
  propertiesOf (Agent {..}) =
        mkType "agent"
      : ("userID", gremlinNum agentUserID)
      : concat [ F.toList (("gid",) . GremlinList . map gremlinNum <$> agentGID)
               , F.toList (("principleType",) . enumOf <$> agentType)
               , F.toList (mkSource <$> agentSource)
               , propertiesOf agentProperties
               ]

nodeUID_base64 :: Node -> Text
nodeUID_base64 = uidToBase64 . nodeUID

uidToBase64 :: UID -> Text
uidToBase64 = T.decodeUtf8 . B64.encode . ByteString.toStrict . encode

newUID :: IO UID
newUID = (decode . ByteString.fromStrict) <$> getEntropy (8 * 4)

gremlinNum :: Integral i => i -> GremlinValue
gremlinNum = GremlinNum . fromIntegral

