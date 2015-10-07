module Adapt.Parser.Lexeme where

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
                deriving (Eq,Show)

data Error    = LexicalError
              | UnexpectedEOF
              | HappyError
                deriving (Eq,Show)
