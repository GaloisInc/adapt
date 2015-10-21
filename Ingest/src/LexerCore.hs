{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE DeriveDataTypeable         #-}

module LexerCore where

import Data.Bits (shiftR,(.&.))
import Data.Char (ord,isDigit,toLower)
import Data.Data
import Data.Word (Word8)
import qualified Data.Text.Lazy as L
import Numeric (readDec)
import Data.Time
import Data.List (foldl')

import Position
import PP

-- Alex Compatibility ----------------------------------------------------------

data AlexInput = AlexInput { aiChar    :: !Char
                           , aiBytes   :: [Word8]
                           , aiInput   :: L.Text
                           , aiPos     :: Position
                           }

aiRng :: AlexInput -> Range
aiRng p = Range (aiPos p) (aiPos p) ""

initialInput :: L.Text -> AlexInput
initialInput txt = AlexInput { aiChar  = '\n'
                             , aiBytes = []
                             , aiInput = txt
                             , aiPos   = Position 0 0 0
                             }

fillBuffer :: AlexInput -> Maybe AlexInput
fillBuffer ai = do
  (c,rest) <- L.uncons (aiInput ai)
  return ai { aiBytes = utf8Encode c
            , aiChar  = c
            , aiInput = rest
            , aiPos   = advance c (aiPos ai)
            }

-- | Encode a Haskell String to a list of Word8 values, in UTF8 format.
utf8Encode :: Char -> [Word8]
utf8Encode = map fromIntegral . go . ord
 where
  go oc
   | oc <= 0x7f       = [oc]

   | oc <= 0x7ff      = [ 0xc0 + (oc `shiftR` 6)
                        , 0x80 +  oc .&. 0x3f
                        ]

   | oc <= 0xffff     = [ 0xe0 + ( oc `shiftR` 12)
                        , 0x80 + ((oc `shiftR` 6) .&. 0x3f)
                        , 0x80 +   oc             .&. 0x3f
                        ]
   | otherwise        = [ 0xf0 + ( oc `shiftR` 18)
                        , 0x80 + ((oc `shiftR` 12) .&. 0x3f)
                        , 0x80 + ((oc `shiftR` 6)  .&. 0x3f)
                        , 0x80 +   oc              .&. 0x3f
                        ]

alexGetByte :: AlexInput -> Maybe (Word8, AlexInput)
alexGetByte ai = case aiBytes ai of
                b : rest -> return (b, ai { aiBytes = rest } )
                _        -> alexGetByte =<< fillBuffer ai

alexInputPrevChar :: AlexInput -> Char
alexInputPrevChar = aiChar

-- Lexer Actions ---------------------------------------------------------------

data LexState = Normal
              | InString String
              | InURI String
              | InComment Int
              deriving (Show)

type Action = Range -> L.Text -> LexState -> (Maybe (Located Token), LexState)

panic :: String -> Range -> Maybe (Located Token)
panic s r = Just ((Err (LexicalError s)) `at` r)

-- | Emit a token.
emit :: Token -> Action
emit tok r chunk sc = (Just (tok `at` r), sc)

-- | Skip the current input.
skip :: Action
skip _ _ sc = (Nothing,sc)

-- | Generate an identifier token.
mkIdent :: Action
mkIdent r chunk sc = (Just (Ident (L.unpack chunk) `at` r), sc)

-- | Read  number
number :: Action
number r chunk sc =
  case readDec (L.unpack chunk) of
    [(n, _)] -> (Just (Num n `at` r), sc)
    _        -> (Just (Err (LexicalError "Could not decode number.") `at` r), sc)

-- Time Literal ----------------------------------------------------------------

mkTime :: Action
mkTime r chunk Normal = (fmap (Located r . Time) (parseUTC chunk), Normal)
mkTime r _     _      = (panic "Lexer tried to lex a time from within a string literal." r, Normal)

parseUTC :: L.Text -> Maybe UTCTime
parseUTC chunk =
  let str      = L.unpack chunk
      numbers  = map (\x -> if isDigit x then x else ' ') str
  in case words numbers of
      [y,m,d,hh,mm,ss] ->
        let day  = fromGregorian (readD y) (readD m) (readD d)
            ps   = psH * readD hh + psM * readD mm + floor (psS * readD ss)
            diff = picosecondsToDiffTime ps
        in Just (UTCTime day diff)
      _ -> Nothing
  where
  psH = psM * 60
  psM = psS * 60
  psS :: Num a => a
  psS = 1000000000000

  readD x = let ((n,_):_) = readDec x in n


-- Multi-Line Comments ---------------------------------------------------------

startComment :: Action
startComment _ _ (InComment n) = (Nothing, InComment (n+1))
startComment _ _ _             = (Nothing, InComment 1)

endComment :: Action
endComment _ _ (InComment n)
  | n <= 1    = (Nothing, Normal)
  | otherwise = (Nothing, InComment (n-1))

-- String Literals -------------------------------------------------------------

startString :: Action
startString _ _ _ = (Nothing, InString "")

-- | Discard the chunk, and use this string instead.
litString :: String -> Action
litString lit _ _ (InString str) = (Nothing, InString (str ++ lit))
litString _   r _ _              = (panic "Lexer expected string literal state" r, Normal)

addString :: Action
addString _ chunk (InString str) = (Nothing, InString (str ++ L.unpack chunk))
addString r _     _            = (panic "Lexer expected string literal state" r, Normal)

mkString :: Action
mkString r _ (InString str) = (Just (String str `at` r), Normal)
mkString r _ _              = (panic "Lexer expected string literal state" r, Normal)

startURI :: Action
startURI _ _ _ = (Nothing, InURI "")

addURI :: Action
addURI _ chunk (InURI uri) = (Nothing, InURI (uri ++ L.unpack chunk))
addURI r  _  _             = (panic "Lexer expected uri state" r, Normal)

mkURI :: Action
mkURI r _ (InURI uri)       = (Just (URI uri `at` r), Normal)
mkURI r _ _               = (panic "Lexer expected uri state at pos" r, Normal)


-- Tokens ----------------------------------------------------------------------

data Token = KW Keyword
           | Num Integer
           | String String
           | URI String
           | Time UTCTime
           | Ident { unIdent :: String }
           | Sym Symbol
           | Eof
           | Err TokenError
             deriving (Data,Ord,Show,Eq)

tokenText :: Token -> L.Text
tokenText (String s) = L.pack s
tokenText _          = error "tokenText: Tried to extract the 'text' of a non-String token"

tokenNum :: Token -> Integer
tokenNum (Num n)     = n
tokenNum  _          = error "tokenText: Tried to extract the 'num' of a non-Num token"

data Keyword = KW_Document
             | KW_EndDocument
             | KW_Prefix
               deriving (Show,Eq,Ord,Data)

wordOfKeyword :: Keyword -> String
wordOfKeyword kw =
  let (x:xs) = drop 3 (show kw)
  in toLower x : xs

data Symbol = BracketL
            | BracketR
            | Semi
            | Pipe
            | ParenL
            | ParenR
            | Assign
            | Type
            | Comma
            | Period
            | Colon
            | Hyphen
            | SingleQuote
              deriving (Show,Eq,Ord,Data)

data TokenError = LexicalError String
                  deriving (Data, Eq, Ord, Show)

descTokenError :: TokenError -> String
descTokenError e = case e of
  LexicalError s -> "lexical error: " ++ s
