{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE OverloadedStrings          #-}
{-# LANGUAGE DeriveDataTypeable         #-}
{-# LANGUAGE CPP                        #-}

module ParserCore (module ParserCore, ParseError(..), Ident(..), textOfIdent, Time) where

import LexerCore hiding (mkIdent)
import Lexer
import Position
import PP

import           Namespaces as NS
import           Types (ParseError(..), Time)

#if (__GLASGOW_HASKELL__ < 710)
import           Control.Applicative (Applicative)
#endif
import           Data.Char (isDigit)
import           Data.Data (Data,Typeable)
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import qualified Data.HashMap.Strict as Map
import           MonadLib (runM,StateT,get,set,ExceptionT,raise,Id)
import qualified Network.URI as URI

data Prov = Prov [Prefix] [Expr]
  deriving (Eq, Ord, Show,Data,Typeable)

data Prefix = Prefix Text URI
  deriving (Eq,Ord,Show,Data,Typeable)

data Expr = RawEntity { exprOper     :: Ident
                      , exprIdent    :: (Maybe Ident)
                      , exprArgs     :: [Maybe (Either Ident Time)]
                      , exprAttrs    :: KVs
                      , exprLocation :: Range
                      }
      deriving (Eq,Ord,Show,Data,Typeable)

expandPrefixes :: Prov -> Prov
expandPrefixes (Prov ps' ex) = Prov ps (map expand ex)
 where
  ps = map (\orig@(Prefix t p) -> if p == adaptOld then Prefix t adapt else orig) ps'
  expand (RawEntity o i as ats l) =
    RawEntity { exprOper     = f o
              , exprIdent    = fmap f i
              , exprArgs     = map (fmap (either (Left . f) Right)) as
              , exprAttrs    = map (\(a,b) -> (f a, b)) ats
              , exprLocation = l
              }

  prefixToTuple (Prefix t uri) = (t,uri)
  m = Map.fromList $ ("prov", NS.prov) : map prefixToTuple ps

  f :: Ident -> Ident
  f i@(Qualified d l) = maybe i (\d' -> mkIdent d' l) $ Map.lookup d m
  f i                 = i

mkPrefix :: Located Token -> Located Token -> Prefix
mkPrefix (Located _ (Ident i)) (Located uriLoc (URI s)) =
  case URI.parseURI s of
    Just u  -> Prefix (L.pack i) u
    Nothing -> error $ "Could not parse URI: " ++ show (s, prettyStr uriLoc)
                -- XXX Make the parser an exception monad
mkPrefix _ _ = error "Impossible: mkPrefix called onn non-URI, non-Ident."

type KVs   = [(Key,Value)]
type Key   = Ident

data Value = ValString Text
           | ValNum    Integer
           | ValIdent Ident
           | ValTypedLit Text Ident
           | ValTime Time
  deriving (Eq,Ord,Show,Data,Typeable)

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

-- Identifiers being allowed to have/start with numbers means the lexer
-- produces identifier tokens instead of numbers, breaking the attribute
-- lexing until/unless we add a new lexer state for attribute lists.
fixIdentLex :: Ident -> Value
fixIdentLex (Unqualified i)
  | L.all isDigit i = ValNum (read (L.unpack i))
fixIdentLex i = ValIdent i

newtype Parser a = Parser
  { getParser :: StateT RW (ExceptionT ParseError Id) a
  } deriving (Functor,Monad, Applicative)

data RW = RW { rwInput  :: [Located Token]
             , rwCursor :: Maybe (Located Token)
             }

runParser :: L.Text -> Parser a -> Either ParseError a
runParser txt p =
  case runM (getParser p) rw of
    Right (a,_) -> Right a
    Left e      -> Left e

  where
  rw = RW { rwInput  = primLexer txt
          , rwCursor = Nothing
          }

lexerP :: ((Located Token) -> Parser a) -> Parser a
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
