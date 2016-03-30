{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE FlexibleInstances #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import CommonDataModel.FromProv
import Data.Int (Int32)
import PP (pretty, pp)
import SimpleGetOpt

import Control.Applicative ((<$>))
import Control.Monad (when)
import Control.Exception
import Data.Int (Int32)
import Data.Monoid ((<>))
import qualified Data.Foldable as F
import Data.Maybe (catMaybes,isNothing, mapMaybe)
import Data.List (partition,intersperse)
import Data.Graph hiding (Node, Edge)
import Data.Time (UTCTime, addUTCTime)
import Text.Read (readMaybe)
import qualified Data.Set as Set
import qualified Data.Map as Map
import           Data.Map (Map)
import Numeric (showHex)
import Data.Binary (encode,decode)
import System.Entropy (getEntropy)
import MonadLib        hiding (handle)
import MonadLib.Monads hiding (handle)
import Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import qualified Data.Text.Lazy.Encoding as Text
import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as ByteString
import qualified Data.ByteString.Base64 as B64
import System.FilePath ((<.>))
import System.Exit (exitFailure)
import System.Random (randoms,randomR)
import System.Random.TF
import System.Random.TF.Gen

import Titan
import Network.HTTP.Types

import Network.Kafka as Kafka
import Network.Kafka.Producer as KProd

type VertsAndEdges = (Map Text (Maybe ResultId), Map Text (Maybe ResultId))

data Config = Config { lintOnly   :: Bool
                     , quiet      :: Bool
                     , stats      :: Bool
                     , ast        :: Bool
                     , verbose    :: Bool
                     , help       :: Bool
                     , upload     :: Maybe ServerInfo
                     , files      :: [FilePath]
                     } deriving (Show)

defaultConfig :: Config
defaultConfig = Config
  { lintOnly = False
  , quiet    = False
  , stats    = False
  , ast      = False
  , verbose  = False
  , help     = False
  , upload   = Nothing
  , files    = []
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files to be scanned.")]
               , progParams    = \p s -> Right s { files = p : files s }
               , progOptions   =
                  [ Option ['l'] ["lint"]
                    "Check the given file for syntactic and type issues."
                    $ NoArg $ \s -> Right s { lintOnly = True }
                  , Option ['q'] ["quiet"]
                    "Quiet linter warnings"
                    $ NoArg $ \s -> Right s { quiet = True }
                  , Option ['v'] ["verbose"]
                    "Verbose debugging messages"
                    $ NoArg $ \s -> Right s { verbose = True }
                  , Option ['a'] ["ast"]
                    "Produce a pretty-printed internal CDM AST"
                    $ NoArg $ \s -> Right s { ast = True }
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
                  , Option ['u'] ["upload"]
                    "Uploads the data by inserting it into a Titan database using gremlin."
                    $ OptArg "Database host" $
                        \str s -> let svr = uncurry ServerInfo <$> (parseHostPort =<< str)
                                  in case (str,svr) of
                                    (Nothing,_)  -> Right s { upload = Just defaultServer }
                                    (_,Just res) -> Right s { upload = Just res }
                                    (_,Nothing) -> Left "Could not parse host:port string."
                  , Option ['h'] ["help"]

                    "Prints this help message."
                    $ NoArg $ \s -> Right s { help = True }
                  ]
               }

parseHostPort :: String -> Maybe (String,Int)
parseHostPort str =
  let (h,p) = break (== ':') str
  in case (h,readMaybe p) of
      ("",_)         -> Nothing
      (_,Nothing)    -> Nothing
      (hst,Just pt) -> Just (hst,pt)

main :: IO ()
main =
  do c <- getOpts opts
     if help c
      then dumpUsage opts
      else mapM_ (trint c) (files c)

trint :: Config -> FilePath -> IO ()
trint c fp = do
  eres <- ingest
  case eres of
    Left e  -> do
      putStrLn $ "Error ingesting " ++ fp ++ ":"
      print (pp e)
      return ()
    Right (ns,es,ws) -> do
      unless (quiet c) $ printWarnings ws
      processStmts c fp (ns,es)

  where
  ingest = do
    t <- handle onError (Text.readFile fp)
    translateTextCDM t
  onError :: IOException -> IO a
  onError e = do putStrLn ("Error reading " ++ fp ++ ":")
                 print e
                 exitFailure

processStmts :: Config -> FilePath -> ([Node],[Edge]) -> IO ()
processStmts c fp res@(ns,es)
  | lintOnly c = return ()
  | otherwise = do
      when (stats  c) $ do
        printStats res

      when (ast c) $ do
        let astfile = fp <.> "trint"
        dbg ("Writing ast to " ++ astfile)
        output astfile $ Text.unlines $ map (Text.pack . show) ns ++ map (Text.pack . show) es
      case upload c of
        Just r ->
          do
            vses <- doUpload c res r
            runKafka (mkKafkaState "adapt-kafka" ("localhost", 9092)) (pushDataToKafka vses)
            return ()
        Nothing ->
          return ()
 where
  dbg s = when (verbose c) (putStrLn s)
  output f t = handle (onError f) $ Text.writeFile f t
  onError :: String -> IOException -> IO ()
  onError f e = do putStrLn ("Error writing " ++ f ++ ":")
                   print e

pushDataToKafka :: VertsAndEdges -> Kafka ()
pushDataToKafka (vs, es) = do produceMessages $ map convertResultId rids
                              return ()
  where convertResultId :: ResultId -> TopicAndMessage
        convertResultId rid = TopicAndMessage "pattern" (ridToMessage rid)
        rids = catMaybes $ Map.elems vs ++ Map.elems es
        ridToMessage (ResultId r) = makeMessage (T.encodeUtf8 r)

printWarnings :: [Warning] -> IO ()
printWarnings ws = Text.putStrLn doc
  where doc = Text.unlines $ intersperse "\n" $ map (Text.pack . show . unW) ws
        unW (Warn w) = w

--------------------------------------------------------------------------------
--  Database Upload

removeBadEdges :: ([Node],[Edge]) -> ([Node],[Edge])
removeBadEdges (ns,es) =
  let exists x = x `Set.member` is
      is = Set.fromList (map nodeUID ns)
  in (ns, filter (\e -> exists (edgeSource e) && exists (edgeDestination e)) es)

-- We should probably do this in a transaction or something to tell when vertex insertion errors out
-- For now, we're just assuming it's always successful
doUpload :: Config -> ([Node],[Edge]) -> ServerInfo -> IO VertsAndEdges
doUpload c work svr =
  do res <- titan svr =<< compileWork (removeBadEdges work)
     case filter isFailure res of
      Failure _ r:_ -> Text.putStrLn ("Upload Error: " <> Text.decodeUtf8 r)
      _             -> return ()
     return (Map.empty,Map.empty) -- XXX
 where
 compileWork (ns,es) =
  do let vertOfNodes = concatMap compileNode ns
     (concat -> vertOfEdges, concat -> edgeOfEdges) <- unzip <$> mapM compileEdge es
     pure $ concat [vertOfNodes , vertOfEdges , edgeOfEdges]

 compileNode :: Node -> [Operation Text]
 compileNode n = [InsertVertex (nodeUID_base64 n) (propertiesOf n)]

 compileEdge :: Edge -> IO ([Operation Text], [Operation Text])
 compileEdge e =
  do euid <- newUID
     let e1Lbl = ""
     let e2Lbl = ""
     let eMe   = uidToBase64 euid
     let [esrc, edst] = map uidToBase64 [edgeSource e, edgeDestination e]
     let v     = InsertVertex eMe (propertiesOf e)
         eTo   = InsertEdge e1Lbl esrc eMe []
         eFrom = InsertEdge e2Lbl eMe edst []
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

instance PropertiesOf Edge where
  propertiesOf (Edge _src _dst rel) = propertiesOf rel

instance PropertiesOf Relationship where
  propertiesOf r =
    case r of
      WasGeneratedBy     -> [rel "wasGeneratedBy"]
      WasInvalidatedBy   -> [rel "wasInvalidatedBy"]
      Used               -> [rel "used"]
      IsPartOf           -> [rel "isPartOf"]
      WasInformedBy      -> [rel "wasInformedBy"]
      RunsOn             -> [rel "runsOn"]
      ResidesOn          -> [rel "residesOn"]
      WasAttributedTo    -> [rel "wasAttributedTo"]
      WasDerivedFrom a b -> [rel "wasDerivedFrom", ("strength", enumOf a), ("derivation", enumOf b)]
    where
      rel      = ("relation",)  . GremlinString

stringOf :: Show a => a -> GremlinValue
stringOf  = GremlinString . T.toLower . T.pack . show

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
                  , ("integrityTag",) . enumOf     <$> infoTrustworthiness
                  , ("sensitivity",)  . enumOf     <$> infoSensitivity
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
               : ("pageNumber", gremlinNum entityPageNumber)
               : ("address", gremlinNum entityAddress)
               : propertiesOf entityInfo

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
       SubjectEvent et s  ->
         [subjTy "event", ("eventType", enumOf et) ]
          ++ F.toList ((("sequence",) . gremlinNum) <$> s)

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
              : ("startTime", gremlinTime subjectStartTime)
              : concat
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
                 ]

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
      : ("userID", GremlinString agentUserID)
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

--------------------------------------------------------------------------------
--  Statistics

printStats :: ([Node],[Edge]) -> IO ()
printStats ss@(vs,es) =
  do let g  = mkGraph ss
         vs = vertices g
         mn = min 1 nrVert
         sz = maximum (mn : map (length . reachable g) vs)
         nrVert = length vs
         nrEdge = length es
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show nrVert
     putStrLn $ "\tEdges: " ++ show nrEdge


--------------------------------------------------------------------------------
--  Graphing

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: ([Node],[Edge]) -> Graph
mkGraph (ns,es) =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge es
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Edge -> State (Map UID Vertex,Vertex) (Maybe (Vertex,Vertex))
mkEdge (Edge s o _) =
  do nS <- nodeOf s
     nO <- nodeOf o
     return $ Just (nS,nO)

-- Memoizing node numbering
nodeOf :: UID -> State (Map UID Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n

