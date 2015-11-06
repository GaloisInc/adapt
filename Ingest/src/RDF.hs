{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}

module RDF
  ( turtle
  ) where

import qualified Data.Set as Set
import Data.Set (Set)
import Data.Map (Map)
import qualified Data.Map as Map
import Data.Monoid ((<>))
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import Data.Time
import Data.Char (toLower)
import MonadLib
import Types

turtle :: [Stmt] -> Text
turtle = runMe . mapM_ tripleStmt

data Triple = Triple Text Text Text
  deriving (Eq, Ord)

type UsedTimeMap = Map NodeId (Integer, Integer)

type M a = StateT (Set Triple, [Triple]) (StateT UsedTimeMap Id) a

type NodeId = Text

runMe :: M a -> Text
runMe m = showTriples ts'
  where
  ((_, (_,ts)), ntts) = runId
                $ runStateT Map.empty
                $ runStateT (Set.empty,[]) m
  ts' = ts


putTriple :: Triple -> M ()
putTriple t = sets_ (\(s,l) -> (Set.insert t s, if Set.member t s then l else t:l))

showTriples :: [Triple] -> Text
showTriples ts = Text.unlines (headers ++ map turtleTriple ts)
  where
  headers =
    [ "@prefix prov: <http://www.w3.org/ns/prov#> ."
    , "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ."
    , "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ."
    , "@prefix dt: <http://m000.github.com/ns/v1/desktop#> ."
    , "@prefix tc: <http://spade.csl.sri.com/rdf/audit-tc.rdfs#> ."
    , "@prefix foaf: <http://xmlns.com/foaf/0.1/> ."
    ]

turtleTriple :: Triple -> Text
turtleTriple (Triple a b c) = Text.intercalate " " [ a, b, c, "."]

tripleStmt :: Stmt -> M ()
tripleStmt (StmtEntity e)    = tripleEntity e
tripleStmt (StmtPredicate p) = triplePredicate p

tripleEntity :: Entity -> M ()
tripleEntity e = case e of
  Agent i aattr           -> putEntity i "prov:Activity" >> mapM_ putAgentAttr aattr
  UnitOfExecution i uattr -> putEntity i "prov:Activity" >> mapM_ putUOEAttr uattr
  Artifact i aattr        -> putEntity i "prov:Entity"   >> mapM_ putArtifactAttr aattr
  Resource i _devTy _devId -> putEntity i "prov:Entity"
  where
  putEntity nodeid typeof = putTriple $ Triple (angleBracket nodeid) "a" typeof

  subj = angleBracket (nameOf e)
  put a b = putTriple (Triple subj a (quote b))

  putAgentAttr a =
    case a of
      AAName t    -> put "foaf:name" t
      AAUser t    -> put "foaf:user" t
      AAMachine m -> put "tc:machine" m

  putUOEAttr a =
    case a of
         UAUser t       -> put "foaf:user" t
         UAPID t        -> put "tc:pid" t
         UAPPID t       -> put "tc:ppid" t
         UAMachine t    -> put "tc:machine" t
         UAHadPrivs t   -> put "tc:privs" t
         UAPWD t        -> put "tc:pwd" t
         UAStarted t    -> putTriple $ Triple subj "tc:time" (Text.pack $ show $ utcToEpoch t)
         UAEnded t      -> return ()
         UAGroup t      -> put "tc:group" t
         UASource t     -> put "tc:source" t
         UACWD t        -> put "tc:cwd" t
         UAUID t        -> put "tc:uid" t
         UACommandLine t        -> put "tc:commandLine" t
         UAProgramName t        -> put "tc:programName" t
  putArtifactAttr a =
    case a of
       ArtAType t               -> put "tc:artifactType" t
       ArtARegistryKey t        -> put "tc:registryKey" t
       ArtADestinationAddress t -> put "tc:destinationAddress" t
       ArtADestinationPort t    -> put "tc:destinationPort" t
       ArtASourceAddress t      -> put "tc:sourceAddress" t
       ArtASourcePort t         -> put "tc:sourcePort" t
       _ -> return ()

triplePredicate :: Predicate -> M ()
triplePredicate Predicate{..} =
    thisPred >> mapM_ aux predAttrs
 where
  thisObj  = angleBracket predObject
  thisVerb =
      let (h:t) = show predType
          vb    = Text.singleton  (toLower h) <> Text.pack t
      in if predType < Description
            then "prov:" <> vb
            else "dc:" <> vb
  thisPred
    | predType == Description = return ()
    | otherwise               = putTriple $ Triple subj thisVerb thisObj
  subj = angleBracket predSubject
  aux (Raw verb obj)    = return () -- XXX blank nodes in RDF are unloved by all putTriple $ Triple subj verb obj
  aux (AtTime t)        = putTriple $ Triple subj "tc:time" (Text.pack $ show $ utcToEpoch t)
  aux (StartTime t)     = putTriple $ Triple subj "prov:startTime" (Text.pack $ show $ utcToEpoch t)
  aux (EndTime t)       = putTriple $ Triple subj "prov:endTime" (Text.pack $ show $ utcToEpoch t)
  aux (GenOp g)         = putTriple $ Triple subj "tc:genOp" g
  aux (Permissions t)   = putTriple $ Triple subj "tc:permissions" t
  aux (ReturnVal t)     = putTriple $ Triple subj "tc:returnVal" t
  aux (Operation t)     = putTriple $ Triple subj "tc:operation" t
  aux (Args t)          = return () -- XXX  putTriple $ Triple subj "tc:args"
  aux (Cmd t)           = putTriple $ Triple subj "tc:commandLine" t
  aux (DeriveOp t)      = putTriple $ Triple subj "tc:deriveOp" t
  aux (ExecOp t)        = putTriple $ Triple subj "tc:execOp" t

predUsedTime :: Predicate -> Maybe (NodeId, UTCTime)
predUsedTime Predicate{..} = case predType of
  Used -> do
    t <- foldl aux Nothing predAttrs
    return (predSubject, t)
  _ -> Nothing
  where
  aux _ (AtTime t) = Just t
  aux acc _        = acc

insertNodeTimeMap :: NodeId -> UTCTime -> UsedTimeMap -> UsedTimeMap
insertNodeTimeMap node utc m = case Map.lookup node m of
  Nothing -> Map.insert node (epoch, epoch) m
  Just window -> Map.insert node (timeWindow window epoch) m
  where
  epoch = utcToEpoch utc
  timeWindow :: (Integer, Integer) -> Integer -> (Integer, Integer)
  timeWindow (lo,hi) a | a < lo    = (a, hi)
                       | a > hi    = (lo, a)
                       | otherwise = (lo, hi)


utcToEpoch :: UTCTime -> Integer
utcToEpoch u = read $ formatTime defaultTimeLocale "%s" u

nodeTimeTriples :: UsedTimeMap -> [Triple]
nodeTimeTriples m = Map.foldrWithKey aux [] m
  where
  aux :: NodeId -> (Integer, Integer) -> [Triple] -> [Triple]
  aux nodeid (mintime, maxtime) ts = start:end:ts
    where
    start = Triple n "prov:startedAtTime" (s mintime)
    end   = Triple n "prov:endedAtTime"   (s maxtime)
    n     = angleBracket nodeid
    s     = Text.pack . show


angleBracket :: Text -> Text
angleBracket t = Text.concat [ "<", t, ">" ]

quote :: Text -> Text
quote t = Text.concat ["\"", t', "\""]
  where t' = Text.concatMap (\x -> if x == '"' then "\\\"" else Text.singleton x) t
