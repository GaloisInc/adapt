{-# LANGUAGE RecordWildCards #-}

module Graph
  (graph
  ) where

import Data.Map (Map)
import qualified Data.Map as Map
import qualified Data.Text as Text
import Data.Text (Text)
import Text.Dot
import MonadLib
import Types

type M a = StateT (Map (Object Type) NodeId) Dot a

runMe :: M a -> String
runMe = showDot . runStateT Map.empty

graph :: [TypeAnnotatedTriple] -> String
graph = runMe . mapM_ graphTriple

graphTriple :: TypeAnnotatedTriple -> M ()
graphTriple (TypeAnnotatedTriple (Triple s v o)) =
  do subj <- memoNode s
     obj  <- memoNode o
     newEdge subj obj (verbProperties v)

memoNode :: Object Type -> M NodeId
memoNode obj =
  do mp <- get
     case Map.lookup obj mp of
        Just n  -> return n
        Nothing -> do node <- newNode (objectProperties obj)
                      set (Map.insert obj node mp)
                      return node

newEdge  :: NodeId -> NodeId -> [(String,String)] -> M ()
newEdge a b ps = lift (edge a b ps)

newNode :: [(String,String)] -> M NodeId
newNode = lift . node

verbProperties :: Verb Type -> [(String,String)]
verbProperties v = [("label",Text.unpack $ objectText v)]

objectProperties :: Object a -> [(String,String)]
objectProperties o = [("label",Text.unpack $ objectText o)]

objectText :: Object a -> Text
objectText (IRI {..}) = theObject
objectText (Lit {..}) = litValue
