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
import Position

import Control.Applicative ((<$>))
import Data.Monoid ( mempty, mappend, mconcat )
import Namespaces
import qualified Network.URI as URI
import qualified Data.Text.Lazy as L
import           Data.Text.Lazy (Text)
import qualified Data.Text.Lazy.IO as L

}

%token

  IDENT                 { $$@(Located _ (Ident {})) }
  NUMLIT                { Located _ (Num   $$)      }
  STRINGLIT             { Located _ (String $$)     }
  TIME                  { Located _ (Time $$)       }
  URILIT                { $$@(Located _ (URI{}))    }

  'document'            { Located $$ (KW KW_Document)         }
  'endDocument'         { Located $$ (KW KW_EndDocument)      }
  'prefix'              { Located $$ (KW KW_Prefix)           }

  '%%'                  { Located $$ (Sym Type)         }
  '='                   { Located $$ (Sym Assign)       }
  ':'                   { Located $$ (Sym Colon)        }
  ','                   { Located $$ (Sym Comma)        }
  '('                   { Located $$ (Sym ParenL)       }
  ')'                   { Located $$ (Sym ParenR)       }

  '['                   { Located $$ (Sym BracketL)     }
  ']'                   { Located $$ (Sym BracketR)     }

  '-'                   { Located $$ (Sym Hyphen)   }
  ';'                   { Located $$ (Sym Semi)         }
  'sq'                  { Located $$ (Sym SingleQuote)  }


%name parseProv prov

%tokentype { Located Token }
%monad     { Parser }
%lexer     { lexerP } { Located _ Eof }

%%

-- Names  ---------------------------------------------------------------------
ident :: { Located Ident }
  : IDENT ':' IDENT     { let Located r1 (Ident d) = $1 in let Located r2 (Ident l) = $3 in Qualified (L.pack d) (L.pack l) `at` (r1,r2) }
  | ':' IDENT           { let Located r (Ident v) = $2 in Located r (Unqualified $ L.pack v)  }
  | IDENT               { let Located r (Ident v) = $1 in Located r (Unqualified $ L.pack v) }

-- Prov-O ---------------------------------------------------------------------

-- XXX notice 'bundle' is not yet supported.

prov :: { Prov }
  : 'document' list(prefix) list(expr) 'endDocument'
    { Prov $2 $3 }

prefix :: { Prefix }
  : 'prefix' IDENT URILIT       { mkPrefix $2 $3 }

expr :: { Expr }
  : ident '(' ident ';' args(',', may(identOrTime)) soattrVals ')' {  RawEntity (locValue $1) (Just $ locValue $3) (reverse $5) $6 (getRange ($1,$7)) }
  | ident '(' args(',', may(identOrTime)) soattrVals ')'           {  RawEntity (locValue $1) Nothing (reverse $3) $4 (getRange ($1,$5)) }

identOrTime :: { Either Ident Time }
 : ident                { let Located _ i = $1 in Left i  }
 | time                 { Right $1 }

-- Can use marker instead
may(p)
  : '-'         { Nothing }
  | p           { Just $1 }

time :: { Time }
  : TIME           { $1 }

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
  : ident '=' literal { (locValue $1, $3) }

literal :: { Value }
  : STRINGLIT                   { ValString $ L.pack $1 }
  | STRINGLIT '%%' ident        { ValTypedLit (L.pack $1) (locValue $3)        }
  | NUMLIT                      { ValNum $1                           }
  | '-' NUMLIT                  { ValNum (negate $2)                  }
  | 'sq' ident 'sq'             { let Located _ i = $2 in ValIdent i  }
  | ident                       { let Located _ i = $1 in ValIdent i {- not in spec, see above -} }
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
