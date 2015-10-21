{-# LANGUAGE TupleSections #-}
module Ingest
    ( -- * High-level interface
      ingestFile, ingestText
    , Error(..), ParseError(..), TypeError(..), TranslateError(..)
    , module Types
    -- XXX PP
    ) where

import Types
import Typecheck
import Translate
import Parser
import qualified Control.Exception as X
import qualified Data.Text.Lazy.IO as Text
import qualified Data.Text.Lazy as Text
import Graph
import MonadLib
import System.IO

ingestFile ::  FilePath -> IO [Stmt]
ingestFile fp =
  do (stmts,ws) <- (either X.throw id . ingestText) <$> Text.readFile fp
     mapM_ (hPutStrLn stderr . Text.unpack . ppWarning) ws
     return stmts

eX :: ExceptionM m i => (e -> i) -> Either e a -> m a
eX f = either (raise . f) return

ingestText :: Text -> Either Error ([Stmt], [Warning])
ingestText t = runId $ runExceptionT $ do
  p       <- eX PE  (parseProvN t)
  (ts,ws) <- eX TRE (translate p)
  ()      <- eX TCE (typecheck ts)
  return  (ts,ws)
