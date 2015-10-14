{-# LANGUAGE RecordWildCards #-}

module Graph
  (graph
  ) where

import Data.Map (Map)
import qualified Data.Map as Map
import Data.Text.Lazy (Text)
import qualified Data.Text.Lazy as Text
import Text.Dot
import MonadLib
import Types

type M a = StateT (Map Text NodeId) Dot a

runMe :: M a -> String
runMe = showDot . runStateT Map.empty

graph :: [Stmt] -> String
graph = runMe . mapM_ graphStmt

graphStmt :: Stmt -> M ()
graphStmt (StmtEntity e)  = undefined -- XXX graph entity
graphStmt (StmtPredicate p) = undefined -- XXX graph pred

memoNode :: Text -> M NodeId
memoNode i =
  do mp <- get
     case Map.lookup i mp of
        Just n  -> return n
        Nothing -> do node <- newNode [("label", Text.unpack i)] -- (objectProperties obj)
                      set (Map.insert i node mp)
                      return node

newEdge  :: NodeId -> NodeId -> [(String,String)] -> M ()
newEdge a b ps = lift (edge a b ps)

newNode :: [(String,String)] -> M NodeId
newNode = lift . node

-- verbProperties :: Predicate Type -> [(String,String)]
-- verbProperties v = [("label",Text.unpack $ pp $ theObject v)]
-- 
-- objectProperties :: Entity a -> [(String,String)]
-- objectProperties o = [("label",Text.unpack $ pp $ theObject o)]
