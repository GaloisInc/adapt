-- vim: ft=haskell

{

-- happy parsers produce a lot of warnings
{-# LANGUAGE OverloadedStrings #-}
{-# OPTIONS_GHC -w #-}

module Parser ( parseProvN, parseProvNFile
              -- * Lower level
              , textOfIdent, Ident
              , parseProv, runParser) where

import ParserCore
import LexerCore
import Data.Monoid ( mempty, mappend, mconcat )
import Namespaces
import qualified Network.URI as URI
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import qualified Data.Text.Lazy.IO as L

}

%token

  IDENT                 { $$@(Ident {}) }
  NUMLIT                { $$@(Num   {}) }
  STRINGLIT             { $$@(String{}) }
  TIME                  { $$@(Time {})  }
  URILIT                { $$@(URI{})    }

  'document'            { KW KW_Document         }
  'endDocument'         { KW KW_EndDocument      }
  'prefix'              { KW KW_Prefix           }

  '%%'                  { Sym Type         }
  '='                   { Sym Assign       }
  ':'                   { Sym Colon        }
  ','                   { Sym Comma        }
  '('                   { Sym ParenL       }
  ')'                   { Sym ParenR       }

  '['                   { Sym BracketL     }
  ']'                   { Sym BracketR     }

  '-'                   { Sym Hyphen   }
  ';'                   { Sym Semi         }
  'sq'                  { Sym SingleQuote  }


%name parseProv prov

%tokentype { Token }
%monad     { Parser }
%lexer     { lexerP } { Eof }

%%

-- Names  ---------------------------------------------------------------------
ident :: { Ident }
  : IDENT ':' IDENT     { Qualified (L.pack $ unIdent $1) (L.pack $ unIdent $3) }
  | ':' IDENT           { Unqualified (L.pack $ unIdent $2)  }
  | IDENT               { Unqualified (L.pack $ unIdent $1)  }

-- Prov-O ---------------------------------------------------------------------

-- XXX notice 'bundle' is not yet supported.

prov :: { Prov }
  : 'document' list(prefix) list(expr) 'endDocument'
    { Prov $2 $3 }

prefix :: { Prefix }
  : 'prefix' IDENT URILIT       { Prefix (L.pack $ unIdent $2) (maybe (error $ "could not parse URI " ++ show $3) id $ (\(URI s) -> URI.parseURI s) $3) }

expr :: { Expr }
  : ident '(' ident ';' args(',', may(identOrTime)) soattrVals ')' {  RawEntity $1 (Just $3) (reverse $5) $6 }
  | ident '(' args(',', may(identOrTime)) soattrVals ')'           {  RawEntity $1 Nothing (reverse $3) $4 }

identOrTime :: { Either Ident Time }
 : ident                { Left $1  }
 | time                 { Right $1 }

-- Can use marker instead
may(p)
  : '-'         { Nothing }
  | p           { Just $1 }

time :: { Time }
  : TIME           { (\(Time t) -> t) $1 }

optAttrs(avParse)
  : ',' attrs(avParse)          { $2 }
  | {- empty -}                 { [] }

attrs(avParse)
  : '[' sep(',', avParse) ']'        { $2 }

soattrVals :: { [(Key,Value)] }
  : ',' oattrVals       { $2 }
  | {- empty -}         { [] }

oattrVals :: { [(Key,Value)] }
  : '['  attrVals ']'   { $2 }

attrVals :: { [(Key,Value)] }
  : sep(',', attrVal)    { $1 }

attrVal :: { (Key,Value) }
  : ident '=' literal { ($1, $3) }

literal :: { Value }
  : STRINGLIT                   { ValString (tokenText $1)            }
  | STRINGLIT '%%' ident        { ValTypedLit (tokenText $1) $3       }
  | NUMLIT                      { ValNum (tokenNum $1)                }
  | '-' NUMLIT                  { ValNum (negate $ tokenNum $1)       }
  | 'sq' ident 'sq'             { ValIdent $2                         }
  | ident                       { ValIdent $1 {- not in spec, see above -} }
  | time                        { ValTime $1      {- not in spec -}   }

-- Utilities -------------------------------------------------------------------

list(p)
  : list1(p)    { $1 }
  | {- empty -} { [] }

list1(p)
  : list1_body(p) { reverse $1 }

list1_body(p)
  : list1_body(p) p { $2 : $1 }
  | p               { [$1]    }

sep(punc,p)
  : sep1(punc,p) { $1 }
  | {- empty -}  { [] }

sep1(punc,p)
  : sep1_body(punc,p) { reverse $1 }

sep1_body(punc,p)
  : sep1_body(punc,p) punc p { $3 : $1 }
  | p                        { [$1]    }

args(punc,p)
  : p                           { [$1]    }
  | args(punc,p) punc p        { $3 : $1 }

{

parseProvN:: Text -> Either ParseError Prov
parseProvN txt = fmap fullyQualifyIdents (runParser txt parseProv)

parseProvNFile :: FilePath -> IO (Either ParseError Prov)
parseProvNFile fp = parseProvN <$> L.readFile fp

}
