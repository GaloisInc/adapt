{
-- At present Alex generates code with too many warnings.
{-# OPTIONS_GHC -w #-}
{-# LANGUAGE RecordWildCards #-}
module Adapt.Parser.Lexer where

import Adapt.Parser.Lexeme
import Adapt.Parser.Position

import qualified Data.Text.Lazy as L
import           Data.Word (Word8)

}

$upper = [A-Z]
$lower = [a-z]
$digit = [0-9]

@activity = $upper [$upper $lower $digit _]* "_activity"
@ident    = $lower [$upper $lower $digit _]*

:-

<0> {

-- White space and comments are skipped
$white+                ;
"--" .*                ;

"="                    { token (Symbol SymDef) }
";"                    { token (Symbol SymSep) }

-- Terminals
@activity              { tokenS Activity }
@ident                 { tokenS Ident    }

}


{


-- Actions ---------------------------------------------------------------------

type AlexAction = Range -> L.Text -> Maybe Lexeme

token :: Token -> AlexAction
token tok loc _ = Just (tok `at` loc)

tokenS :: (L.Text -> Token) -> AlexAction
tokenS mkToken loc txt = Just (mkToken txt `at` loc)


-- Lexer Interface -------------------------------------------------------------

data AlexInput = AlexInput { alexPos           :: !Position
                           , alexInputPrevChar :: !Char
                           , alexInput         :: L.Text
                           } deriving (Show)

-- | This just assumes that we're parsing ASCII.  If we ever need UTF-8, this
-- should be revisited.
alexGetByte :: AlexInput -> Maybe (Word8, AlexInput)
alexGetByte AlexInput { .. } =
  do (c,rest) <- L.uncons alexInput
     let inp' = AlexInput { alexPos           = advance c alexPos
                          , alexInput         = rest
                          , alexInputPrevChar = c }
     return (toEnum (fromEnum c), inp')

-- | Returns the tokens and the last position of the input that we processed.
-- The tokens include whte space tokens.
primLexer :: String -> L.Text -> [Lexeme]
primLexer src inp = run AlexInput { alexPos           = start
                                  , alexInputPrevChar = '\n'
                                  , alexInput         = inp }

  where

  start     = Position { posCol = 1, posRow = 1, posOff = 0 }

  eofR p = Range p' p' src
    where
    p' = Position { posRow = posRow p + 1
                  , posCol = 0
                  , posOff = posOff p + 1 }

  -- NOTE: the lexer currently only uses one state. In the future, we may need
  -- to add more, at which point this will need to change.
  run i =
    case alexScan i 0 of

      AlexEOF ->
        [ EOF `at` eofR (alexPos i) ]

      AlexError i'  ->
        let loc = Range (alexPos i) (alexPos i') src
        in [ Error LexicalError `at` loc ]

      AlexSkip i' _ ->
        run i'

      AlexToken i' l act ->
        let txt   = L.take (fromIntegral l) (alexInput i)
            end   = L.foldl' (flip advance) (alexPos i) txt
            range = Range (alexPos i) end src
            mtok  = act range txt
            rest  = run i'
        in case mtok of
             Just t   -> t : rest
             Nothing  ->     rest

-- vim: ft=haskell
}



