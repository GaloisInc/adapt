{-# LANGUAGE TupleSections #-}
{-# LANGUAGE CPP           #-}
module Ingest
    ( -- * High-level interface
      ingestFile, ingestText
    , Error(..), ParseError(..), TypeError(..), TranslateError(..), Prefix(..)
    , pp, PP(..)
    , module Types
    ) where

import Types
import Typecheck
import Translate
import Parser
import PP (pp, PP(..))
#if (__GLASGOW_HASKELL__ < 710)
import Control.Applicative
#endif
import qualified Control.Exception as X
import qualified Data.Text.Lazy.IO as Text
import MonadLib
import System.IO

ingestFile ::  FilePath -> IO [Stmt]
ingestFile fp =
  do (_,stmts,ws) <- (either X.throw id . ingestText) <$> Text.readFile fp
     mapM_ (hPutStrLn stderr . show . pp) ws
     return stmts

eX :: ExceptionM m i => (e -> i) -> Either e a -> m a
eX f = either (raise . f) return

ingestText :: Text -> Either Error ([Prefix], [Stmt], [Warning])
ingestText t = runId $ runExceptionT $ do
  p@(Prov px _) <- eX PE  (parseProvN t)
  (ts,ws)       <- eX TRE (translate p)
  ()            <- eX TCE (typecheck ts)
  return  (px,ts,ws)
