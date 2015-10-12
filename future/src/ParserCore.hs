{-# LANGUAGE GeneralizedNewtypeDeriving #-}

module ParserCore where

import LexerCore
import Lexer

import           Control.Applicative (Applicative)
import           Data.Time (UTCTime(..), fromGregorian, picosecondsToDiffTime)
import           Data.List ( nub )
import           Data.Monoid (mconcat)
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import           MonadLib (runM,StateT,get,set,ExceptionT,raise,Id)
import           Network.URI (URI)

data Prov = Prov [Prefix] [Expr]
  deriving (Eq, Ord, Show)

data Prefix = Prefix Ident URI
  deriving (Eq,Ord,Show)

data Ident = Qualified Text Text
           | Unqualified Text
  deriving (Eq,Ord,Show)

type Time = UTCTime

mkTime y m d hh mm ss = UTCTime (fromGregorian (fromIntegral y) (fromIntegral m) (fromIntegral d)) (picosecondsToDiffTime (fromIntegral hh*psH + fromIntegral mm*psM + fromIntegral ss*psS))
 where
  psH = 60 * psM
  psM = 60 * psS
  psS = 1000000000000

data Expr = Entity Ident KVs
          | Agent Ident KVs
          | RawEntity Ident [Ident] KVs
          | Activity Ident (Maybe Time) (Maybe Time) KVs
          | WasGeneratedBy (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | Used (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | WasStartedBy (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | WasEndedBy (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Time) KVs
          | WasInformedBy (Maybe Ident) (Maybe Ident) (Maybe Ident) KVs
          | WasAssociatedWith (Maybe Ident) (Maybe Ident) (Maybe Ident) (Maybe Ident) KVs
          | WasDerivedFrom (Maybe Ident) (Maybe Ident) Ident (Maybe Ident) (Maybe Ident) (Maybe Ident) KVs
          | WasAttributedTo (Maybe Ident) (Maybe Ident) Ident KVs
          | IsPartOf Ident Ident
          | Description Ident KVs
      deriving (Eq,Ord,Show)

type KVs   = [(Key,Value)]
type Key   = Ident
data Value = ValString Text
           | ValNum    Integer
           | ValIdent Ident
           | ValTypedLit Text Ident
  deriving (Eq,Ord,Show)


newtype Parser a = Parser
  { getParser :: StateT RW (ExceptionT ParseError Id) a
  } deriving (Functor,Monad, Applicative)

data RW = RW { rwInput  :: [Token]
             , rwCursor :: Maybe Token
             }

data ParseError = HappyError (Maybe Token)
                | HappyErrorMsg String
                  deriving (Show)

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
