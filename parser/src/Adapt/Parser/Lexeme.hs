{-# LANGUAGE RecordWildCards #-}

module Adapt.Parser.Lexeme where

import Adapt.Parser.PP
import Adapt.Parser.Position

import qualified Data.Text.Lazy as L


type Lexeme = Located Token

type Activity = L.Text

type Ident    = L.Text

data Token    = Symbol Symbol
              | Activity Activity
              | Ident Ident
              | Error Error
              | EOF
                deriving (Eq,Show)

data Symbol   = SymDef
              | SymSep
              | SymLParen
              | SymRParen
              | SymComma
                deriving (Eq,Show)

data Error    = LexicalError Range
              | UnexpectedEOF
              | HappyError Range
                deriving (Eq,Show)

ppError :: Maybe L.Text -> Error -> Doc

ppError mb err =
  case err of

    LexicalError range ->
      sep [ text "Lexical error:", getContext range ]

    UnexpectedEOF ->
      text "Unexpected end of file"

    HappyError range ->
      sep [ text "Parse error:", getContext range ]

  where
  getContext NoLoc                     = empty
  getContext range @ (Range start _ _) =
    let line = text (replicate (posCol start - 1) '~') <> char '^'
    in case mb of
         Just input -> pp (getLines input 3 range) <> line
         Nothing    -> pp range
