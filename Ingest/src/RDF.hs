{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}

module RDF
  ( turtle
  ) where

import Data.List (find)
import Data.Map (Map)
import qualified Data.Map as Map
import Data.Monoid ((<>))
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import Data.Time
import MonadLib
import Types

turtle :: [Stmt] -> Text
turtle = runMe . mapM_ tripleStmt

data Triple = Triple Text Text Text

type UsedTimeMap = Map NodeId (Integer, Integer)

type M a = StateT [Triple] (StateT UsedTimeMap Id) a

type NodeId = Text

runMe :: M a -> Text
runMe m = showTriples ts'
  where
  ((_, ts), ntts) = runId
                $ runStateT Map.empty
                $ runStateT [] m
  ts' = ts ++ nodeTimeTriples ntts


putTriple :: Triple -> M ()
putTriple t = sets_ $ \ts -> t:ts

setsUsedTimeMap :: (UsedTimeMap -> UsedTimeMap) -> M ()
setsUsedTimeMap f = lift (sets_ f)

showTriples :: [Triple] -> Text
showTriples ts = Text.unlines (headers ++ map turtleTriple ts)
  where
  headers =
    [ "@prefix prov: <http://www.w3.org/ns/prov#> ."
    , "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ."
    , "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ."
    , "@prefix dt: <http://m000.github.com/ns/v1/desktop#> ."
    , "@prefix tc: <http://spade.csl.sri.com/rdf/audit-tc.rdfs#> ."
    ]

turtleTriple :: Triple -> Text
turtleTriple (Triple a b c) = Text.intercalate " " [ a, b, c, "."]

tripleStmt :: Stmt -> M ()
tripleStmt (StmtEntity e)    = tripleEntity e
tripleStmt (StmtPredicate p) = triplePredicate p

tripleEntity :: Entity -> M ()
tripleEntity e = case e of
  Agent i _aattr           -> putEntity i "prov:Activity"
  UnitOfExecution i _uattr -> putEntity i "prov:Activity"
  Artifact i _aattr        -> putEntity i "prov:Entity"
  Resource i _devTy _devId -> putEntity i "prov:Entity"
  where
  putEntity nodeid typeof = putTriple $ Triple (angleBracket nodeid) "a" typeof

triplePredicate :: Predicate -> M ()
triplePredicate pred = do
  case predUsedTime pred of
    Just (node, utc) ->
      setsUsedTimeMap $ \s -> insertNodeTimeMap node utc s
    Nothing -> return ()


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
