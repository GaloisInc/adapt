{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}

module RDF
  ( turtle
  ) where

import Data.Set (Set)
import qualified Data.Set as Set
import Data.Monoid ((<>))
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import MonadLib
import Types

turtle :: [Stmt] -> Text
turtle = runMe . mapM_ tripleStmt

data Triple = Triple Text Text Text

------------------------------------------------------------
--  Monad Operations

type M a = StateT (Set Text) (StateT [Triple] Id) a

type NodeId = Text

runMe :: M a -> Text
runMe m = showTriples ts
  where (_, ts) = runId $ runStateT [] $ runStateT Set.empty m

emitTriple :: Triple -> M ()
emitTriple t = lift (sets_ (t:))

markVisited :: Text -> M ()
markVisited t = sets_ (Set.insert t)

wasVisited :: Text -> M Bool
wasVisited t = Set.member t `fmap` get

------------------------------------------------------------
--  Stmt/Triple Conversion

showTriples :: [Triple] -> Text
showTriples ts = Text.unlines (headers ++ map showTriple ts)
  where headers = [] -- XXX FIXME

showTriple :: Triple -> Text
showTriple (Triple a b c) = Text.intercalate " " [ a, b, c, "."]

tripleStmt :: Stmt -> M ()
tripleStmt (StmtEntity e)    = tripleEntity e
tripleStmt (StmtPredicate p) = triplePredicate p

tripleEntity :: Entity -> M ()
tripleEntity e = void $ case e of
  Agent i _aattr              -> memoNode i "Agent"
  UnitOfExecution i _uattr    -> memoNode i "UoE"
  Artifact i _aattr           -> memoNode i "Artifact"
  Resource i _devTy _devId    -> memoNode i "Resource"

triplePredicate :: Predicate -> M ()
triplePredicate (Predicate s o pTy _attr) =
  do sN <- memoNode s undefined
     oN <- memoNode o undefined
     newEdge sN oN [("label", show pTy)]

memoNode :: NodeId -> Text -> M NodeId
memoNode i nodetype = do
  b <- wasVisited i
  if b
  then return i
  else do node <- newNode i nodetype
          markVisited node
          return i

newEdge  :: NodeId -> NodeId -> [(String,String)] -> M ()
newEdge a b ps = emitTriple edge
  where
  edge = Triple a "XXX" b

newNode :: NodeId -> a -> M NodeId
newNode nodeid _typeof = do
  emitTriple node
  return nodeid
  where
  node = Triple nodeid "a" "XXX"
