{-# LANGUAGE RecordWildCards            #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE MultiParamTypeClasses      #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE ViewPatterns               #-}
-- | Compiling typechecked data into the higher-level abstraction (PROV).

module Compile
 ( -- * High-level interface
   Error(..), translate
   -- * Low-level interface
   , ParseError(..), TypeError(..)
   , TypeAnnotatedTriple(..)
   , Object(..), Entity, Verb
   ) where

import MonadLib
import Types
import Typecheck
import Parser
import Data.Monoid
import qualified Data.Text as Text
import qualified Control.Exception as X
import qualified Data.Map as Map
import           Data.Map (Map)

-- | Translate the TA1-style RDF inputs into the prov-motivated TA2 internal
-- language.
translate :: [TypeAnnotatedTriple] -> [TypeAnnotatedTriple]
translate = execTranslate . mapM_ go
 where
     go :: TypeAnnotatedTriple -> Translate ()
     go (TypeAnnotatedTriple t) =
                do s <- rename (triSubject t)
                   o <- rename (triObject t)
                   emit (TypeAnnotatedTriple $ provOrdering $ Triple s (triVerb t) o)
                   when (isOutputSubj t) $ step (triSubject t)
                   when (isOutputObj t)  $ step (triObject t)

-- | Verbs with reversed causality: the object has impacted the subject.
isOutputObj :: Triple a -> Bool
isOutputObj (Triple _ p _) =
    theObject p `elem` ["wasDerivedFrom", "spawnedBy", "wasInformedBy", "wasKilledBy", "wasAttributedTo"]

-- | Verbs with forward causality: the subject has impacted the object.
isOutputSubj :: Triple a -> Bool
isOutputSubj (Triple _ p _) =
    theObject p `elem` ["modified", "destroyed", "write"]

provOrdering :: Triple a -> Triple a
provOrdering t@(Triple v p o)
    | theObject p == "write" = Triple o (p { theObject = "writtenBy"}) v
    | otherwise              = t

data TranslateState = TS { objectMap :: Map (Entity Type) Int
                         , emittedTriples :: [TypeAnnotatedTriple] -- XXX use dlist
                         }

newtype Translate a = Translate { runTranslate :: StateT TranslateState Id a } deriving (Monad, Applicative, Functor)

instance StateM Translate TranslateState where
  set = Translate . set
  get = Translate get

execTranslate :: Translate a -> [TypeAnnotatedTriple]
execTranslate = reverse . emittedTriples . snd . runId . runStateT (TS Map.empty []) . runTranslate

getObjectNum :: Entity Type -> Translate Int
getObjectNum e = maybe 0 id . Map.lookup e . objectMap <$> get

incObjectNum :: Entity Type -> Translate ()
incObjectNum e = sets (\(TS {..}) -> ((),TS (Map.insertWith (+) e 1 objectMap) emittedTriples) )

emit :: TypeAnnotatedTriple -> Translate ()
emit v = do s <- get
            set s { emittedTriples = v : emittedTriples s }

-- Rename objects by post-pending them the number provided in the state.
rename :: Entity Type -> Translate (Entity Type)
rename e = do
  num <- getObjectNum e
  return e { theObject = theObject e <> Text.pack (show num) }

-- Step an object by incrementing the state _and_ emitting a new triple
-- indicating 'was derived from'.
step :: Entity Type -> Translate ()
step e =
  do e' <- rename e
     incObjectNum e
     e'' <- rename e
     emit $ TypeAnnotatedTriple $ Triple e'' wasDerivedFrom  e'

wasDerivedFrom :: Object Type
wasDerivedFrom =
  let (a,b) = either X.throw id $ runExcept $ tcVerb obj
      obj   = IRI "tc://" "wasDerivedFrom" ()
  in obj { objectTag = TyArrow a b }

runExcept :: Except i b -> Either i b
runExcept = runId . runExceptionT

type Except i a = ExceptionT i Id a
