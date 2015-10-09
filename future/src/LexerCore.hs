{-# LANGUAGE GeneralizedNewtypeDeriving #-}

module LexerCore where

import Data.Bits (shiftR,(.&.))
import Data.Char (ord)
import Data.Word (Word8)
import qualified Data.Text.Lazy as L
import Numeric (readDec)

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
              | InString String

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
    _        -> (Just (Err LexicalError), sc)

-- String Literals -------------------------------------------------------------

startString :: Action
startString _ _ = (Nothing, InString "")

-- | Discard the chunk, and use this string instead.
litString :: String -> Action
litString lit _ (InString str) = (Nothing, InString (str ++ lit))
litString _   _ _              = panic "Lexer: Expected string literal state"

addString :: Action
addString chunk (InString str) = (Nothing, InString (str ++ L.unpack chunk))
addString _     _              = panic "Lexer Expected string literal state"

mkString :: Action
mkString _ (InString str) = (Just (String str ), Normal)
mkString _ _              = panic "Lexer Expected string literal state"


-- Tokens ----------------------------------------------------------------------

data Token = KW Keyword
           | Num Integer
           | String String
           | Ident String
           | Sym Symbol
           | Eof
           | Err TokenError
             deriving (Show,Eq)

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
             | KW_Document
             | KW_EndDocument
               deriving (Show,Eq)

data Symbol = BracketL
            | BracketR
            | Semi
            | Pipe
            | BraceL
            | BraceR
            | ParenL
            | ParenR
            | Assign
            | Comma
            | Period
            | Colon
            | Hyphen
              deriving (Show,Eq)

data TokenError = LexicalError
                  deriving (Show,Eq)

descTokenError :: TokenError -> String
descTokenError e = case e of
  LexicalError -> "lexical error"
