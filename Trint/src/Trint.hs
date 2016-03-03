{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import PP as PP
import Ingest
import SimpleGetOpt
import qualified Namespaces as NS
import Types as T
import qualified Graph as G
import qualified RDF

import Control.Applicative ((<$>))
import Control.Monad (when)
import Control.Exception
import Data.Int (Int32)
import Data.Monoid ((<>))
import Data.Maybe (catMaybes,isNothing, mapMaybe)
import Data.List (partition,intersperse)
import Data.Graph
import Data.Time (UTCTime, addUTCTime)
import qualified Data.Set as Set
import qualified Data.Map as Map
import           Data.Map (Map)
import Numeric (showHex)
import MonadLib        hiding (handle)
import MonadLib.Monads hiding (handle)
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import Data.Text.Lazy.Encoding (encodeUtf8)
import qualified Data.ByteString.Lazy as ByteString
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
                     , graph      :: Bool
                     , stats      :: Bool
                     , turtle     :: Bool
                     , ast        :: Bool
                     , provn      :: Bool
                     , fakeData   :: Bool
                     , verbose    :: Bool
                     , help       :: Bool
                     , upload     :: Maybe ServerInfo
                     , files      :: [FilePath]
                     } deriving (Show)

defaultConfig :: Config
defaultConfig = Config
  { lintOnly = False
  , quiet    = False
  , graph    = False
  , stats    = False
  , turtle   = False
  , ast      = False
  , provn    = False
  , fakeData = False
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
                    "Produce a pretty-printed internal AST"
                    $ NoArg $ \s -> Right s { ast = True }
                  , Option ['p'] ["provn"]
                    "Produce a pretty-printed, reformatted, PROV representation"
                    $ NoArg $ \s -> Right s { provn = True }
                  , Option ['f'] ["fake"]
                    "When pretty-printing, use fake data to fill in missing fields"
                    $ NoArg $ \s -> Right s { fakeData = True }
                  , Option ['g'] ["graph"]
                    "Produce a dot file representing a graph of the conceptual model."
                    $ NoArg $ \s -> Right s { graph = True }
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
                  , Option ['t'] ["turtle"]
                    "Produce a turtle RDF description of the graph."
                    $ NoArg $ \s -> Right s { turtle = True }
                  , Option ['u'] ["upload"]
                    "Uploads the data by inserting it into a Titan database using gremlin."
                    $ OptArg "Database host" $ \str s -> Right s { upload = Just $ maybe defaultServer ServerInfo str }
                  , Option ['h'] ["help"]
                    "Prints this help message."
                    $ NoArg $ \s -> Right s { help = True }
                  ]
               }

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
    Right (pxs,res,ws) -> do
      unless (quiet c) $ printWarnings ws
      processStmts c fp res pxs

  where
  ingest = do
    t <- handle onError (Text.readFile fp)
    return (ingestText t)
  onError :: IOException -> IO a
  onError e = do putStrLn ("Error reading " ++ fp ++ ":")
                 print e
                 exitFailure

processStmts :: Config -> FilePath -> [Stmt] -> [Prefix] -> IO ()
processStmts c fp res pxs
  | lintOnly c = return ()
  | otherwise = do
      when (graph  c) $ do
        let dotfile =  fp <.> "dot"
        dbg ("Writing dot to " ++ dotfile)
        output dotfile (G.graph res)
      when (stats  c) $ do
        printStats res

      when (turtle c) $ do
        let ttlfile = fp <.> "ttl"
        dbg ("Writing turtle RDF to " ++ ttlfile)
        output ttlfile (RDF.turtle res)
      when (ast c) $ do
        let astfile = fp <.> "trint"
        dbg ("Writing ast to " ++ astfile)
        output astfile $ Text.unlines $ map (Text.pack . show) res
      when (provn c) $ do
        let provfile = fp <.> "trint.provn"
        dbg ("Writing prettified prov-n to " ++ provfile)
        gen <- newTFGen
        let theStatements | fakeData c = useFakeData gen res
                         | otherwise  = res
        output provfile (renderProv pxs theStatements)
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
        convertResultId rid = TopicAndMessage "segmenter" (ridToMessage rid)
        rids = catMaybes $ Map.elems vs ++ Map.elems es
        ridToMessage (ResultId r) = makeMessage (ByteString.toStrict (encodeUtf8 r))

printWarnings :: [Warning] -> IO ()
printWarnings ws = Text.putStrLn doc
  where doc = Text.unlines $ intersperse "\n" $ map (Text.pack . show . pp) ws

--------------------------------------------------------------------------------
--  Pretty Printing

renderProv :: [Prefix] -> [Stmt] -> Text
renderProv pfx0 stmts =
   let pfx    = trintns : provtc : pfx0
       header = map renderPrefix pfx
       ss     = map (renderStmt pfx) (zipWith nameUsed stmts [0..])
   in Text.unlines $ concatMap (map (Text.pack . show))
                     [[text "document"]
                     , header
                     , ss
                     , [text "endDocument"]]

-- Any used predicate that does not have a self-identifier is given an
-- arbitrary ident of 'trint:NUM'.
nameUsed :: Stmt -> Integer -> Stmt
nameUsed (StmtPredicate p) i =
      case (predType p,predIdent p) of
        (Used,Nothing) -> StmtPredicate (p { predIdent = Just (trintURI NS..: Text.pack (showHex i "")) })
        _              -> StmtPredicate p
nameUsed (StmtLoc (Located r p)) i = StmtLoc (Located r (nameUsed p i))
nameUsed x _ = x

renderPrefix :: Prefix -> Doc
renderPrefix (Prefix p u) = text "prefix " PP.<> pp p PP.<> text " <" PP.<> pp (show u) PP.<> text ">"

renderStmt :: [Prefix] -> Stmt -> Doc
renderStmt pfx stmt = pp (onIdent abbreviate stmt)
  where
  abbreviate (Qualified q x) = Qualified (shorthand q) x
  abbreviate x               = x

  shorthand :: Text -> Text
  shorthand t = maybe t id (Map.lookup t dict)

  dict :: Map Text Text
  dict = Map.fromList [ (Text.pack $ show fq,sh) | Prefix sh fq <- pfx]

-- | A fake Prov prefix for use with manufactured data such as generated 'used'
-- identifiers.
trintns :: Prefix
trintns = Prefix "trint" trintURI

trintURI :: NS.URI
trintURI = NS.perr "http://galois.com/adapt/trint"

-- | The prefix for ProvTC
provtc :: Prefix
provtc = Prefix "prov-tc" NS.adapt


--------------------------------------------------------------------------------
--  Fake Data for 'used' relations

useFakeData :: TFGen -> [Stmt] -> [Stmt]
useFakeData g0 ss = zipWith fakeStmt gens ss
 where
  gens = map (splitn g0 (ceiling $ logBase 2 (fromIntegral nrSS))) [0 .. fromIntegral nrSS - 1]
  nrSS = length ss

fakeStmt :: TFGen -> Stmt -> Stmt
fakeStmt g stmt@(StmtPredicate p@(Predicate {..}))
    | predType == Used =
      let ks  = Set.fromList (map reprOf predAttrs)
          gs  = map (splitn g (ceiling $ logBase 2 (fromIntegral nrAttrs))) [0 .. fromIntegral nrAttrs - 1]
          kgs = zip attrs gs
          mkNewAttr (rep,gi) | rep `Set.member` ks = []
                             | otherwise = randomValOf gi rep
          newAttrs = concatMap mkNewAttr kgs
      in StmtPredicate p { predAttrs = predAttrs ++ newAttrs }
    | otherwise = stmt
  where attrs :: [ PredicateAttrRepr ]
        attrs = [ PR_AtTime, PR_Cmd, PR_MachineID {- UoE attrs have been requested: UR_CWD , UR_PPID, UR_PID, UR_ProgramName, UR_USER-} ]
        nrAttrs = length attrs
        epoch = read "2016-01-13 19:53:37.839456 UTC"
        randomValOf :: TFGen -> PredicateAttrRepr -> [PredicateAttr]
        randomValOf g r =
            case r of
              PR_AtTime    -> [ AtTime (addUTCTime (fromIntegral $ fst $ randomR (0::Int,10000000) g) epoch) ]
              PR_Cmd       -> [ Cmd "./Fake-Command" ]
              PR_MachineID -> [ MachineID $ randomUUID g ] -- Text.pack $ concatMap show $ flip showHex "" $ take 4 (randoms g :: [Int32]) ]
              _            -> error "Incomplete random predicate attr generation."
fakeStmt g (StmtLoc (Located r s)) = StmtLoc (Located r (fakeStmt g s))
fakeStmt g sOther = sOther

randomUUID :: TFGen -> Text
randomUUID = Text.pack . concatMap (flip showHex "" . abs) . (take 4 :: [Int32] -> [Int32]). randoms

data PredicateAttrRepr
        = PR_Raw
        | PR_AtTime
        | PR_StartTime
        | PR_EndTime
        | PR_GenOp
        | PR_Permissions
        | PR_ReturnVal
        | PR_Operation
        | PR_Args
        | PR_Cmd
        | PR_DeriveOp
        | PR_ExecOp
        | PR_MachineID
        | PR_SourceAddress
        | PR_DestinationAddress
        | PR_SourcePort
        | PR_DestinationPort
        | PR_Protocol
        deriving (Eq, Ord)

reprOf :: PredicateAttr -> PredicateAttrRepr
reprOf p =
  case p of
        Raw {}                -> PR_Raw
        AtTime {}             -> PR_AtTime
        StartTime {}          -> PR_StartTime
        EndTime {}            -> PR_EndTime
        GenOp {}              -> PR_GenOp
        Permissions {}        -> PR_Permissions
        ReturnVal {}          -> PR_ReturnVal
        Operation {}          -> PR_Operation
        Args {}               -> PR_Args
        Cmd {}                -> PR_Cmd
        DeriveOp {}           -> PR_DeriveOp
        ExecOp {}             -> PR_ExecOp
        MachineID {}          -> PR_MachineID
        SourceAddress {}      -> PR_SourceAddress
        DestinationAddress {} -> PR_DestinationAddress
        SourcePort {}         -> PR_SourcePort
        DestinationPort {}    -> PR_DestinationPort
        Protocol {}           -> PR_Protocol

--------------------------------------------------------------------------------
--  Database Upload


-- We should probably do this in a transaction or something to tell when vertex insertion errors out
-- For now, we're just assuming it's always successful
doUpload :: Config -> [Stmt] -> ServerInfo -> IO VertsAndEdges
doUpload c stmts svr =
 do stats <- push es
    let vertIds = Map.fromList(zip (map label es) stats)
    let ps' = mapMaybe (resolveId vertIds) ps
    stats' <- push ps'
    let edgeIds = Map.fromList(zip (map label ps) stats')
    -- let err = filter ( (/= status200)) stats
    -- when (not $ quiet c) $ putStrLn $ unlines $ map show err
    return (vertIds, edgeIds)
  where
  (es,ps) = partition isVert(map translateInsert stmts)

  isVert InsertVertex{} = True
  isVert _ = False

  resolveId vertIds (InsertEdge l s d p) = 
    do s' <- Map.findWithDefault Nothing s vertIds
       d' <- Map.findWithDefault Nothing d vertIds
       return (InsertEdge l s' d' p)
  resolveId _ (InsertVertex l p) = return (InsertVertex l p)

  push :: GraphId i => [Operation i] -> IO [Maybe ResultId]
  push = mapM $ \stmt ->
    do res <- titan svr stmt
       case res of
         Success _ i -> return (Just i)
         Failure exn -> error (show exn)
         ParseError -> return (Nothing)
         
translateInsert :: Stmt -> Operation Text
translateInsert (StmtPredicate (Predicate {..})) =
    InsertEdge { label = pretty predType
               , src   = pretty predSubject
               , dst   = pretty predObject
               , properties = pa }
   where
    pa = map transPA predAttrs
    transPA p =
      case p of
        AtTime t             -> ("atTime"             , textOfTime t   )
        StartTime t          -> ("startTime"          , textOfTime t   )
        EndTime t            -> ("endTime"            , textOfTime t   )
        GenOp gop            -> ("genOp"              , gop )
        Permissions t        -> ("permissions"        , t   )
        ReturnVal t          -> ("returnVal"          , t   )
        Operation uop        -> ("operation"          , uop )
        Args t               -> ("args"               , t   )
        Cmd t                -> ("cmd"                , t   )
        DeriveOp dop         -> ("deriveOp"           , dop )
        ExecOp eop           -> ("execOp"             , eop )
        MachineID mid        -> ("machineID"          , mid )
        SourceAddress t      -> ("sourceAddress"      , t   )
        DestinationAddress t -> ("destinationAddress" , t   )
        SourcePort t         -> ("sourcePort"         , t   )
        DestinationPort t    -> ("destinationPort"    , t   )
        Protocol t           -> ("protocol"           , t   )
        Raw k v              -> (k,v) -- XXX warn

translateInsert (StmtEntity e)    =
  case e of
    Agent i as           -> InsertVertex (pretty i) (("vertexType", "agent") : (map translateAA as))
    UnitOfExecution i as -> InsertVertex (pretty i) (("vertexType", "unitOfExecution") : (map translateUOEA as))
    Artifact i as        -> InsertVertex (pretty i) (("vertexType", "artifact") : (map translateArA as))
    Resource i devty as  -> InsertVertex (pretty i) (("vertexType", "resource") : (translateDevId as))
  where
  translateAA as   =
   case as of
    AAName t      -> ("name", t)
    AAUser t      -> ("user", t)
    AAMachine mid -> ("machine", mid)

  translateUOEA as =
    case as of
        UAUser t        -> ("user"        , t)
        UAPID p         -> ("PID"         , p)
        UAPPID p        -> ("PPID"        , p)
        UAMachine mid   -> ("machine"     , mid)
        UAStarted t     -> ("started"     , textOfTime t)
        UAHadPrivs p    -> ("hadPrivs"    , p)
        UAPWD t         -> ("PWD"         , t)
        UAEnded t       -> ("ended"       , textOfTime t)
        UAGroup t       -> ("group"       , t)
        UACommandLine t -> ("commandLine" , t)
        UASource t      -> ("source"      , t)
        UAProgramName t -> ("programName" , t)
        UACWD t         -> ("CWD"         , t)
        UAUID t         -> ("UID"         , t)

  translateArA as  =
    case as of
      ArtAType at              -> ("type"               , at)
      ArtARegistryKey t        -> ("registryKey"        , t)
      ArtACoarseLoc cl         -> ("coarseLoc"          , cl)
      ArtAFineLoc fl           -> ("fineLoc"            , fl)
      ArtACreated t            -> ("created"            , textOfTime t)
      ArtAVersion v            -> ("version"            , v)
      ArtADeleted t            -> ("deleted"            , textOfTime t)
      ArtAOwner t              -> ("owner"              , t)
      ArtASize int             -> ("size"               , Text.pack $ show int)
      ArtADestinationAddress t -> ("destinationAddress" , t)
      ArtADestinationPort t    -> ("destinationPort"    , t)
      ArtASourceAddress t      -> ("sourceAddress"      , t)
      ArtASourcePort t         -> ("sourcePort"         , t)
      Taint w                  -> ("Taint"              , Text.pack $ show w)
  translateDevId Nothing   = []
  translateDevId (Just i)  = [("devId", i)]

translateInsert (StmtLoc (Located _ s)) = translateInsert s

textOfTime :: Time -> Text
textOfTime = Text.pack . show -- XXX

--------------------------------------------------------------------------------
--  Statistics

printStats :: [Stmt] -> IO ()
printStats ss =
  do let g  = mkGraph ss
         vs = vertices g
         mn = length (take 1 ss) -- one node suggests the minimum subgraph is one.
         sz = min nrStmt $ maximum (mn : map (length . reachable g) vs) -- XXX O(n^2) algorithm!
         nrStmt = length ss
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show (min (length vs) nrStmt)
     putStrLn $ "\tPredicates:       " ++ show (nrStmt - length vs)
     putStrLn $ "\tTotal statements: " ++ show nrStmt


--------------------------------------------------------------------------------
--  Graphing

data NodeInfo = Node Int | Edge (Int,Int)

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: [Stmt] -> Graph
mkGraph ss =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge ss
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Stmt -> State (Map Text Vertex,Vertex) (Maybe Edge)
mkEdge (StmtPredicate (Predicate s o _ _ _)) =
  do nS <- nodeOf (pretty s)
     nO <- nodeOf (pretty o)
     return $ Just (nS,nO)
mkEdge (StmtLoc (T.Located _ s)) = mkEdge s
mkEdge (StmtEntity {})         = return Nothing

-- Memoizing node numbering
nodeOf :: Text -> State (Map Text Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n

