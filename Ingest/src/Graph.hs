{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}

module Graph
  (graph
  ) where

import Data.Map (Map)
import qualified Data.Map as Map
import Data.Monoid ((<>))
import qualified Data.Text.Lazy as Text
import Text.Dot
import MonadLib
import Types

type M a = StateT (Map Text NodeId) Dot a

runMe :: M a -> Text
runMe = Text.pack . showDot . runStateT Map.empty

graph :: [Stmt] -> Text
graph = runMe . mapM_ graphStmt

graphStmt :: Stmt -> M ()
graphStmt (StmtEntity e)          = graphEntity e
graphStmt (StmtPredicate p)       = graphPredicate p
graphStmt (StmtLoc (Located _ s)) = graphStmt s

graphEntity :: Entity -> M ()
graphEntity e =
  do _ <- case e of
           Agent i _aattr              -> memoNode $ "Agent:"    <> i
           UnitOfExecution i _uattr    -> memoNode $ "UoE:"      <> i
           Artifact i _aattr           -> memoNode $ "Artifact:" <> i
           Resource i _devTy _devId    -> memoNode $ "Resource:" <> i
     return ()

graphPredicate :: Predicate -> M ()
graphPredicate (Predicate s o pTy _attr) =
  do sN <- memoNode s
     oN <- memoNode o
     newEdge sN oN [("label", show pTy)]

memoNode :: Text -> M NodeId
memoNode i =
  do mp <- get
     case Map.lookup i mp of
        Just n  -> return n
        Nothing -> do nd <- newNode [("label", Text.unpack i)] -- (objectProperties obj)
                      set (Map.insert i nd mp)
                      return nd

newEdge  :: NodeId -> NodeId -> [(String,String)] -> M ()
newEdge a b ps = lift (edge a b ps)

newNode :: [(String,String)] -> M NodeId
newNode = lift . node

-- verbProperties :: Predicate Type -> [(String,String)]
-- verbProperties v = [("label",Text.unpack $ pp $ theObject v)]
-- 
-- objectProperties :: Entity a -> [(String,String)]
-- objectProperties o = [("label",Text.unpack $ pp $ theObject o)]
