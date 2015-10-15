{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE DeriveDataTypeable         #-}

module ParserCore (module ParserCore, Ident(..), textOfIdent) where

import LexerCore hiding (mkIdent)
import Lexer

import           Namespaces
import           Control.Applicative (Applicative)
import           Data.Data
import           Data.Time (UTCTime(..), fromGregorian, picosecondsToDiffTime)
import           Data.List ( nub )
import           Data.Monoid (mconcat, (<>))
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import           MonadLib (runM,StateT,get,set,ExceptionT,raise,Id)
import           Network.URI (URI)

data Prov = Prov [Prefix] [Expr]
  deriving (Eq, Ord, Show,Data)

data Prefix = Prefix Ident URI
  deriving (Eq,Ord,Show,Data)

type Time = UTCTime

data Expr = Entity Ident KVs
          | Agent Ident KVs
          | RawEntity Ident [Ident] KVs
          | Activity Ident (Maybe Time) (Maybe Time) KVs
          | WasGeneratedBy (Maybe Ident) Ident (Maybe Ident) (Maybe Time) KVs
          | Used (Maybe Ident) Ident (Maybe Ident) (Maybe Time) KVs
          | WasStartedBy (Maybe Ident) Ident (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | WasEndedBy (Maybe Ident) Ident (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | WasInformedBy (Maybe Ident) Ident (Maybe Ident) KVs
          | WasAssociatedWith (Maybe Ident) Ident (Maybe Ident) (Maybe Ident) KVs
          | WasDerivedFrom (Maybe Ident) Ident Ident (Maybe Ident) (Maybe Ident) (Maybe Ident) KVs
          | ActedOnBehalfOf (Maybe Ident) Ident Ident (Maybe Ident) KVs
          | WasAttributedTo (Maybe Ident) Ident Ident KVs
          | WasInvalidatedBy (Maybe Ident) Ident (Maybe Ident) (Maybe Time) KVs
          | IsPartOf Ident Ident
          | Description Ident KVs
      deriving (Eq,Ord,Show,Data)

type KVs   = [(Key,Value)]
type Key   = Ident

data Value = ValString Text
           | ValNum    Integer
           | ValIdent Ident
           | ValTypedLit Text Ident
           | ValTime Time
  deriving (Eq,Ord,Show,Data)

valueString :: Value -> Maybe Text
valueString (ValString t) = Just t
valueString _             = Nothing

valueTime :: Value -> Maybe Time
valueTime (ValTime t) = Just t
valueTime _           = Nothing

valueIdent :: Value -> Maybe Ident
valueIdent (ValIdent t) = Just t
valueIdent _            = Nothing

valueNum :: Value -> Maybe Integer
valueNum (ValNum t) = Just t
valueNum _          = Nothing

newtype Parser a = Parser
  { getParser :: StateT RW (ExceptionT ParseError Id) a
  } deriving (Functor,Monad, Applicative)

data RW = RW { rwInput  :: [Token]
             , rwCursor :: Maybe Token
             }

data ParseError = HappyError (Maybe Token)
                | HappyErrorMsg String
                  deriving (Data, Eq, Ord, Show)

runParser :: L.Text -> Parser a -> Either ParseError a
runParser txt p =
  case runM (getParser p) rw of
    Right (a,_) -> Right a
    Left e      -> Left e

  where
  rw = RW { rwInput  = primLexer txt
          , rwCursor = Nothing
          }

lexerP :: (Token -> Parser a) -> Parser a
lexerP k = Parser $ do
  rw <- get
  case rwInput rw of
    l : rest ->
               do set RW { rwInput  = rest
                         , rwCursor = Just l
                         }
                  getParser (k l)
    []       -> raise (HappyError (rwCursor rw))

happyError :: Parser a
happyError  = Parser $ do
  rw <- get
  raise (HappyError (rwCursor rw))
