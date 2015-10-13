{-# LANGUAGE GeneralizedNewtypeDeriving #-}

module LexerCore where

import Data.Bits (shiftR,(.&.))
import Data.Char (ord,isDigit)
import Data.Word (Word8)
import qualified Data.Text.Lazy as L
import Numeric (readDec)
import Data.Time

-- Alex Compatibility ----------------------------------------------------------

data AlexInput = AlexInput { aiChar    :: !Char
                           , aiBytes   :: [Word8]
                           , aiInput   :: L.Text
                           }

initialInput :: L.Text -> AlexInput
initialInput txt = AlexInput { aiChar = '\n'
                             , aiBytes = []
                             , aiInput = txt
                             }

fillBuffer :: AlexInput -> Maybe AlexInput
fillBuffer ai = do
  (c,rest) <- L.uncons (aiInput ai)
  return ai { aiBytes = utf8Encode c
            , aiChar  = c
            , aiInput = rest
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
              | InString  Char String
              | InURI String
              | InComment Int
              deriving (Show)

type Action = L.Text -> LexState -> (Maybe Token, LexState)

panic :: String -> a
panic = error

-- | Emit a token.
emit :: Token -> Action
emit tok _chunk sc = (Just tok, sc)

-- | Skip the current input.
skip :: Action
skip _ sc = (Nothing,sc)

-- | Generate an identifier token.
mkIdent :: Action
mkIdent chunk sc = (Just (Ident (L.unpack chunk) ), sc)

-- | Read  number
number :: Action
number chunk sc =
  case readDec (L.unpack chunk) of
    [(n, _)] -> (Just (Num n), sc)
    _        -> (Just (Err $ LexicalError "Could not decode number."), sc)

-- Time Literal ----------------------------------------------------------------

mkTime :: Action
mkTime chunk Normal = (Just (Time t), Normal)
  where
  t        = UTCTime day diff
  str      = L.unpack chunk
  numbers  = map (\x -> if isDigit x then x else ' ') str
  [y,m,d,hh,mm,ss] = [n | [(n,_)] <- map readDec $ words numbers]
  day  = fromGregorian (fromIntegral y) (fromIntegral m) (fromIntegral d)
  diff = picosecondsToDiffTime (fromIntegral hh*psH + fromIntegral mm*psM + fromIntegral ss*psS)
  psH = 60 * psM
  psM = 60 * psS
  psS = 1000000000000

mkTime _ _ = panic "Lexer tried to lex a time from withing a string literal."

-- Multi-Line Comments ---------------------------------------------------------

startComment :: Action
startComment _ (InComment n) = (Nothing, InComment (n+1))
startComment _ _             = (Nothing, InComment 1)

endComment :: Action
endComment _ (InComment n)
  | n <= 1    = (Nothing, Normal)
  | otherwise = (Nothing, InComment (n-1))

-- String Literals -------------------------------------------------------------

startString :: Char -> Action
startString term _ _ = (Nothing, InString term "")

-- | Discard the chunk, and use this string instead.
litString :: String -> Action
litString lit _ (InString t str) = (Nothing, InString t (str ++ lit))
litString _   _ _              = panic "Lexer expected string literal state"

addString :: Action
addString chunk (InString t str) = (Nothing, InString t (str ++ L.unpack chunk))
addString _     _              = panic "Lexer expected string literal state"

mkString :: Action
mkString _ (InString _ str) = (Just (String str), Normal)
mkString _ _              = panic "Lexer expected string literal state"

startURI :: Action
startURI _ _ = (Nothing, InURI "")

addURI :: Action
addURI chunk (InURI uri)  = (Nothing, InURI (uri ++ L.unpack chunk))
addURI _     _            = panic "Lexer expected uri state"

mkURI :: Action
mkURI _ (InURI uri)       = (Just (URI uri), Normal)
mkURI _ _                 = panic "Lexer expected uri state"


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
             deriving (Show,Eq)

tokenText :: Token -> L.Text
tokenText (String s) = L.pack s
tokenText _          = error "tokenText: Tried to extract the 'text' of a non-String token"

tokenNum :: Token -> Integer
tokenNum (Num n)     = n
tokenNum  _          = error "tokenText: Tried to extract the 'num' of a non-Num token"

-- | Virtual symbols inserted for layout purposes.
data Virt = VOpen
          | VClose
          | VSep
            deriving (Show,Eq)

data Keyword = KW_Activity
             | KW_Agent
             | KW_Artifact
             | KW_Resource
             | KW_WasAssociatedWith
             | KW_Entity
             | KW_Used
             | KW_WasStartedBy
             | KW_WasEndedBy
             | KW_WasInformedBy
             | KW_WasGeneratedBy
             | KW_WasDerivedFrom
             | KW_ActedOnBehalfOf
             | KW_WasAttributedTo
             | KW_WasInvalidatedBy
             | KW_Description
             | KW_IsPartOf
             | KW_Document
             | KW_EndDocument
             | KW_Prefix
               deriving (Show,Eq)

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
              deriving (Show,Eq)

data TokenError = LexicalError String
                  deriving (Show,Eq)

descTokenError :: TokenError -> String
descTokenError e = case e of
  LexicalError s -> "lexical error: " ++ s
