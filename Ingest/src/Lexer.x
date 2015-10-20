-- vim: ft=haskell

{

{-# OPTIONS_GHC -w #-}

module Lexer (
        primLexer
        ) where

import Data.Char
import Numeric
import LexerCore
import qualified Data.Text.Lazy as T

}

@time           = [0-9]{4}[\-][0-9]{2}[\-][0-9]{2}[T][0-9]{1,2}[:][0-9]{1,2}[:][0-9]{1,2}(\.[0-9]{1,9})?Z?
$id_first       = [A-Za-z]
$id_next        = [A-Zaa-z0-9_\-]
$digit          = [0-9]
$hex_digit      = [0-9a-fA-F]
$oct_digit      = [0-7]
$bin_digit      = [0-1]

@uriPart        = [^\\\>]+
@strPart        = [^\\\"]+
@str1Part       = [^\\\']+
@junk           = . | $white+ | [\n]

:-

<uri> {
@uriPart                { addURI }
\\n                     { \_ _ -> (Just (Err $ LexicalError "Lexer: Newline in uri."), Normal) }
>                       { mkURI         }
}

<string> {
@strPart                { addString      }
\\n                     { litString "\n" }
\\\"                    { litString "\"" }
\"                      { mkString       }
\\                      { litString "\\" }
}


<mlc> {
"/*"                     { startComment }
"*/"                     { endComment }
@junk                    { skip }
}

<0> {
$white+                 { skip }
"//" .*                 { skip }

"prefix"                { emit $ KW KW_Prefix            }
"document"              { emit $ KW KW_Document          }
"endDocument"           { emit $ KW KW_EndDocument       }
"end" $white+ "document"          { emit $ KW KW_EndDocument       }

"("                     { emit $ Sym ParenL      }
")"                     { emit $ Sym ParenR      }
"["                     { emit $ Sym BracketL    }
"]"                     { emit $ Sym BracketR    }
","                     { emit $ Sym Comma       }
"="                     { emit $ Sym Assign      }
"%%"                    { emit $ Sym Type        }
":"                     { emit $ Sym Colon       }
";"                     { emit $ Sym Semi        }
"-"                     { emit $ Sym Hyphen      }
"."                     { emit $ Sym Period      }
"'"                     { emit $ Sym SingleQuote }

\"                      { startString }
\<                      { startURI    }
"/*"                    { startComment }

@time                   { mkTime  }
$id_first $id_next*     { mkIdent }
$digit+                 { number  }

}

{

stateToInt :: LexState -> Int
stateToInt Normal       = 0
stateToInt (InString _) = string
stateToInt InURI     {} = uri
stateToInt InComment {} = mlc

primLexer :: T.Text -> [Token]
primLexer = loop Normal . initialInput
 where
 loop :: LexState -> AlexInput -> [Token]
 loop sc ai =
    case alexScan ai (stateToInt sc) of
        AlexToken ai' len action  ->
            let chunk    = T.take (fromIntegral len) (aiInput ai)
                (mb,sc') = action chunk sc
                rest     = loop sc' ai'
            in maybe rest (:rest) mb
        AlexSkip ai' _            -> loop sc ai'
        AlexError  x              -> [Err $ LexicalError (show (sc, aiChar x, aiBytes x))]
        AlexEOF                   ->
                case sc of
                    Normal       -> [Eof]
                    InString s   -> panic $ "Unexpected end of file: non-terminated string starting with: " ++ take 10 s
                    InURI    s   -> panic $ "Unexpected end of file: non-terminated URI starting with: "    ++ take 10 s
                    InComment _  -> panic $ "Unexpected end of file: non-terminated comment."

}
