{-# LANGUAGE RecordWildCards #-}

module Adapt.Parser.AST where

import Adapt.Parser.PP
import Adapt.Parser.Position

import qualified Data.Text.Lazy as L


data Decl = Decl { dName :: Located L.Text
                 } deriving (Show)

instance PP Decl where
  ppPrec _ Decl { .. } = pp dName <+> char '='

  ppList ds = vcat (map pp ds)
