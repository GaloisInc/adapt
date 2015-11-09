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
import qualified Data.Text.Lazy as Text
import Data.Time
import Data.Char (toLower)
import MonadLib
import Types
import PP (pretty)

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
  ((_, (_,ts)), _ntts) = runId
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
tripleStmt (StmtLoc (Located _ s))     = tripleStmt s
tripleStmt (StmtEntity e)    = tripleEntity e
tripleStmt (StmtPredicate p) = triplePredicate p

tripleEntity :: Entity -> M ()
tripleEntity e = case e of
  Agent i aattr           -> putEntity i "prov:Activity" >> mapM_ putAgentAttr aattr
  UnitOfExecution i uattr -> putEntity i "prov:Activity" >> mapM_ putUOEAttr uattr
  Artifact i aattr        -> putEntity i "prov:Entity"   >> mapM_ putArtifactAttr aattr
  Resource i _devTy _devId -> putEntity i "prov:Entity"
  where
  putEntity nodeid typeof = putTriple $ Triple (angleBracket $ pretty nodeid) "a" typeof

  subj = angleBracket (nameOf e)
  putT a b = putTriple (Triple subj a (quote b))

  putAgentAttr a =
    case a of
      AAName t    -> putT "foaf:name" t
      AAUser t    -> putT "foaf:user" t
      AAMachine m -> putT "tc:machine" m

  putUOEAttr a =
    case a of
         UAUser t       -> putT "foaf:user" t
         UAPID t        -> putT "tc:pid" t
         UAPPID t       -> putT "tc:ppid" t
         UAMachine t    -> putT "tc:machine" t
         UAHadPrivs t   -> putT "tc:privs" t
         UAPWD t        -> putT "tc:pwd" t
         UAStarted t    -> putTriple $ Triple subj "tc:time" (Text.pack $ show $ utcToEpoch t)
         UAEnded _      -> return ()
         UAGroup t      -> putT "tc:group" t
         UASource t     -> putT "tc:source" t
         UACWD t        -> putT "tc:cwd" t
         UAUID t        -> putT "tc:uid" t
         UACommandLine t        -> putT "tc:commandLine" t
         UAProgramName t        -> putT "tc:programName" t
  putArtifactAttr a =
    case a of
       ArtAType t               -> putT "tc:artifactType" t
       ArtARegistryKey t        -> putT "tc:registryKey" t
       ArtADestinationAddress t -> putT "tc:destinationAddress" t
       ArtADestinationPort t    -> putT "tc:destinationPort" t
       ArtASourceAddress t      -> putT "tc:sourceAddress" t
       ArtASourcePort t         -> putT "tc:sourcePort" t
       _ -> return ()

triplePredicate :: Predicate -> M ()
triplePredicate Predicate{..} =
    thisPred >> mapM_ aux predAttrs
 where
  thisObj  = angleBracket (pretty predObject)
  thisVerb =
      let (h:t) = show predType
          vb    = Text.singleton  (toLower h) <> Text.pack t
      in if predType < Description
            then "prov:" <> vb
            else "dc:" <> vb
  thisPred
    | predType == Description = return ()
    | otherwise               = putTriple $ Triple subj thisVerb thisObj
  subj = angleBracket (pretty predSubject)

  putT a b = putTriple (Triple subj a (quote b))

  aux (Raw _erb _bj)    = return () -- XXX blank nodes in RDF are unloved by all putTriple $ Triple subj verb obj
  aux (AtTime t)        = putT "tc:time" (Text.pack $ show $ utcToEpoch t)
  aux (StartTime t)     = putT "prov:startTime" (Text.pack $ show $ utcToEpoch t)
  aux (EndTime t)       = putT "prov:endTime" (Text.pack $ show $ utcToEpoch t)
  aux (GenOp g)         = putT "tc:genOp" g
  aux (Permissions t)   = putT "tc:permissions" t
  aux (ReturnVal t)     = putT "tc:returnVal" t
  aux (Operation t)     = putT "tc:operation" t
  aux (Args _)          = return () -- XXX  putTriple $ Triple subj "tc:args"
  aux (Cmd t)           = putT "tc:commandLine" t
  aux (DeriveOp t)      = putT "tc:deriveOp" t
  aux (ExecOp t)        = putT "tc:execOp" t
  -- The prov description are rather contrary to everything else in our
  -- language, so we must handle them in an unusual manner.
  aux (MachineID t)          = putTriple $ Triple thisObj "tc:machineID" (quote t)
  aux (SourceAddress t)      = putTriple $ Triple thisObj "tc:sourceAddress" (quote t)
  aux (DestinationAddress t) = putTriple $ Triple thisObj "tc:destinationAddress" (quote t)
  aux (SourcePort t)         = putTriple $ Triple thisObj "tc:sourcePort" (quote t)
  aux (DestinationPort t)    = putTriple $ Triple thisObj "tc:destinationPort" (quote t)
  aux (Protocol t)           = putTriple $ Triple thisObj "tc:protocol" (quote t)

utcToEpoch :: UTCTime -> Integer
utcToEpoch u = read $ formatTime defaultTimeLocale "%s" u

angleBracket :: Text -> Text
angleBracket t = Text.concat [ "<", t, ">" ]

quote :: Text -> Text
quote t = Text.concat ["\"", t', "\""]
  where t' = Text.concatMap esc t
        escSet = "\\\"" :: String
        esc x = (if x `elem` escSet
                  then ("\\" <>)
                  else id ) (Text.singleton x)
