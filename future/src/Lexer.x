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

$id_first       = [A-Za-z]
$id_next        = [A-Zaa-z0-9_\-]
$digit          = [0-9]
$hex_digit      = [0-9a-fA-F]
$oct_digit      = [0-7]
$bin_digit      = [0-1]

@uriPart        = [^\\\>]+
@strPart        = [^\\\"]+
@str1Part       = [^\\\"]+

:-

<uri> {
@uriPart                { addURI }
\\n                     { \_ _ -> (Just (Err LexicalError), Normal) }
>                       { mkURI         }
}

<string> {
@strPart                { addString      }
\\n                     { litString "\n" }
\"                      { mkString       }
}

<string1> {
@str1Part               { addString      }
\\n                     { litString "\n" }
\'                      { mkString       }
}

<0> {
$white+                 { skip }
"//" .*                 { skip }

"prefix"                { emit $ KW KW_Prefix            }
"activity"              { emit $ KW KW_Activity          }
"agent"                 { emit $ KW KW_Agent             }
"artifact"              { emit $ KW KW_Artifact          }
"resource"              { emit $ KW KW_Resource          }
"wasAssociatedWith"     { emit $ KW KW_WasAssociatedWith }
"wasDerivedFrom"        { emit $ KW KW_WasDerivedFrom    }
"wasAttributedTo"       { emit $ KW KW_WasAttributedTo   }
"wasInformedBy"         { emit $ KW KW_WasInformedBy     }
"entity"                { emit $ KW KW_Entity            }
"used"                  { emit $ KW KW_Used              }
"wasStartedBy"          { emit $ KW KW_WasStartedBy      }
"wasEndedBy"            { emit $ KW KW_WasEndedBy        }
"wasGeneratedBy"        { emit $ KW KW_WasGeneratedBy    }
"description"           { emit $ KW KW_Description       }
"isPartOf"              { emit $ KW KW_IsPartOf          }
"document"              { emit $ KW KW_Document          }
"endDocument"           { emit $ KW KW_EndDocument       }

"T"                     { emit $ Sym TimeSymT    }
"Z"                     { emit $ Sym TimeSymZ    }
"("                     { emit $ Sym ParenL      }
")"                     { emit $ Sym ParenR      }
"["                     { emit $ Sym BracketL    }
"]"                     { emit $ Sym BracketR    }
","                     { emit $ Sym Comma       }
"="                     { emit $ Sym Assign      }
"%%"                    { emit $ Sym Type        }
":"                     { emit $ Sym Colon       }
"-"                     { emit $ Sym Hyphen      }
"."                     { emit $ Sym Period      }
"'"                     { emit $ Sym SingleQuote }

\"                      { startString }
\<                      { startURI    }

$id_first $id_next*     { mkIdent }
$digit+                 { number }

}

{

stateToInt :: LexState -> Int
stateToInt Normal      = 0
stateToInt InString {} = string
stateToInt InURI    {} = uri

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
        AlexError  _              -> [Err LexicalError]
        AlexEOF                   ->
                case sc of
                    Normal     -> [Eof]
                    InString s -> panic $ "Unexpected end of file: non-terminated string starting with: " ++ take 10 s
                    InURI    s -> panic $ "Unexpected end of file: non-terminated URI starting with: "    ++ take 10 s

}
