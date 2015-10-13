-- vim: ft=haskell

{

-- happy parsers produce a lot of warnings
{-# OPTIONS_GHC -w #-}

module Parser ( parseProvN, parseProvNFile
              -- * Lower level
              , parseProv, runParser) where

import ParserCore
import LexerCore
import Data.Monoid ( mempty, mappend, mconcat )
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

  'activity'            { KW KW_Activity        }
  'agent'               { KW KW_Agent           }
--  'artifact'            { KW KW_Artifact        }  XXX An entity with prov:type=tc:artifact
--  'resource'            { KW KW_Resource        }  XXX An entity with a 'devType' property
  'wasAssociatedWith'   { KW KW_WasAssociatedWith       }
  'entity'              { KW KW_Entity           }
  'used'                { KW KW_Used             }
  'wasStartedBy'        { KW KW_WasStartedBy     }
  'wasGeneratedBy'      { KW KW_WasGeneratedBy   }
  'wasEndedBy'          { KW KW_WasEndedBy       }
  'wasInformedBy'       { KW KW_WasInformedBy    }
  'wasAttributedTo'     { KW KW_WasAttributedTo  }
  'wasDerivedFrom'      { KW KW_WasDerivedFrom   }
  'actedOnBehalfOf'     { KW KW_ActedOnBehalfOf  }
  'wasInvalidatedBy'    { KW KW_WasInvalidatedBy }
  'description'         { KW KW_Description      }
  'isPartOf'            { KW KW_IsPartOf         }
  'document'            { KW KW_Document         }
  'endDocument'         { KW KW_EndDocument      }
  'prefix'              { KW KW_Prefix           }
  '%%'                  { Sym Type         }
  '='                   { Sym Assign       }
  ':'                   { Sym Colon        }
  ','                   { Sym Comma        }
  '('                   { Sym ParenL       }
  ')'                   { Sym ParenR       }
  '\''                  { Sym SingleQuote  }

  '['                   { Sym BracketL     }
  ']'                   { Sym BracketR     }

  '-'                   { Sym Hyphen   }
  ';'                   { Sym Semi         }

--  '_'                   { Sym Underscore   }
--  '{'                   { Sym BraceL       }
--  '}'                   { Sym BraceR       }


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
  : 'prefix' ident URILIT       { Prefix $2 (maybe (error $ "could not parse URI" ++ show $3) id $ (\(URI s) -> URI.parseURI s) $3) }

expr :: { Expr }
  : provExpr            { $1 }
  | dcExpr              { $1 }
  | rawExpr             { $1 }

provExpr :: { Expr }
  : entity           { $1 }
  | activity         { $1 }
  | generation       { $1 }
  | usage            { $1 }
  | start            { $1 }
  | end              { $1 }
  | invalidation   { $1 }
  | communication    { $1 }
  | agent            { $1 }
  | association      { $1 }
  | attribution      { $1 }
  | delegation     { $1 }
  | derivation       { $1 }
--  | influence      { $1 }
--  | alternate      { $1 }
--  | specialization { $1 }
--  | membership     { $1 }
--  | extensibility  { $1 }

dcExpr :: { Expr }
  : isPartOf    { $1 }
  | description { $1 }

rawExpr :: { Expr }
  : ident '(' args soattrVals ')' {  RawEntity $1 $3 $4 }

args :: { [ Ident ] }
  : sep(',', ident)      { $1 }

-- Prov Exprs ------------------------------------------------------------------

entity          :: { Expr }
  : 'entity' '(' ident soattrVals ')'     {  Entity $3 $4 }

activity        :: { Expr }
  : 'activity' '(' ident ',' may(time) ',' may(time) soattrVals ')'     {  Activity $3 $5 $7 $8 }
  | 'activity' '(' ident soattrVals ')'                                 {  Activity $3 Nothing Nothing $4 }

generation      :: { Expr }
  : 'wasGeneratedBy' '(' mayIdent ',' may(ident) ',' may(time) soattrVals ')' {  WasGeneratedBy (fst $3) (snd $3) $5 $7 $8 }
  | 'wasGeneratedBy' '(' mayIdent soattrVals ')'                              {  WasGeneratedBy (fst $3) (snd $3) Nothing Nothing $4 }

usage           :: { Expr }
  : 'used' '(' mayIdent ',' may(ident) ',' may(time) soattrVals ')' {  Used (fst $3) (snd $3) $5 $7 $8 }
  | 'used' '(' mayIdent soattrVals ')'                              {  Used (fst $3) (snd $3) Nothing Nothing $4 }

start           :: { Expr }
  : 'wasStartedBy' '(' mayIdent ',' may(ident) ',' may(ident) ',' may(time) soattrVals ')' {  WasStartedBy (fst $3) (snd $3) $5 $7 $9 $10 }
  | 'wasStartedBy' '(' mayIdent soattrVals ')'                                             {  WasStartedBy (fst $3) (snd $3) Nothing Nothing Nothing $4 }

end             :: { Expr }
  : 'wasEndedBy' '(' mayIdent ',' may(ident) ',' may(ident) ',' may(time) soattrVals ')' {  WasEndedBy (fst $3) (snd $3) $5 $7 $9 $10 }
  | 'wasEndedBy' '(' mayIdent soattrVals ')'                                             {  WasEndedBy (fst $3) (snd $3) Nothing Nothing Nothing $4 }

communication   :: { Expr }
  : 'wasInformedBy' '(' mayIdent ',' ident soattrVals ')'  {  WasInformedBy (fst $3) (snd $3) (Just $5) $6 }
  | 'wasInformedBy' '(' mayIdent soattrVals ')'            {  WasInformedBy (fst $3) (snd $3) Nothing $4 }

agent           :: { Expr }
  : 'agent' '(' ident soattrVals ')'    { Agent $3 $4 }

association     :: { Expr }
  : 'wasAssociatedWith' '(' mayIdent ',' may(ident) ',' may(ident) soattrVals ')' { WasAssociatedWith (fst $3) (snd $3) $5 $7 $8 }
  | 'wasAssociatedWith' '(' mayIdent soattrVals ')' { WasAssociatedWith (fst $3) (snd $3) Nothing Nothing $4 }

attribution     :: { Expr }
  : 'wasAttributedTo' '(' mayIdent ',' ident soattrVals ')' { WasAttributedTo (fst $3) (snd $3) $5 $6 }

invalidation    :: { Expr }
  : 'wasInvalidatedBy' '(' mayIdent ',' may(ident) ',' may(time) soattrVals ')'         { WasInvalidatedBy (fst $3) (snd $3) $5 $7 $8 }
  | 'wasInvalidatedBy' '(' mayIdent ',' soattrVals ')'                                  { WasInvalidatedBy (fst $3) (snd $3) Nothing Nothing $5 }

delegation      :: { Expr }
  : 'actedOnBehalfOf' '(' mayIdent ',' ident ',' may(ident) soattrVals ')'      { ActedOnBehalfOf (fst $3) (snd $3)  $5 $7 $8      }
  | 'actedOnBehalfOf' '(' mayIdent ',' ident     soattrVals ')'                 { ActedOnBehalfOf (fst $3) (snd $3)  $5 Nothing $6 }

derivation      :: { Expr }
  : 'wasDerivedFrom' '(' mayIdent ',' ident ',' may(ident) ',' may(ident) ',' may(ident) soattrVals ')' {  WasDerivedFrom (fst $3) (snd $3) $5 $7 $9 $11 $12 }
  | 'wasDerivedFrom' '(' mayIdent ',' ident soattrVals ')'                                              {  WasDerivedFrom (fst $3) (snd $3) $5 Nothing Nothing Nothing $6 }

isPartOf        :: { Expr}
  : 'isPartOf'  '(' ident ',' ident ')'         {  IsPartOf $3 $5 }

description     :: { Expr }
  : 'description' '(' ident soattrVals ')'      {  Description $3 $4 }

-- Can use marker instead
may(p)
  : '-'         { Nothing }
  | p           { Just $1 }

-- Typical for optional identifiers
mayIdent
  : '-' ';' ident    { (Nothing, $3) }
  | ident ';' ident  { (Just $1, $3) }
  | ident            { (Nothing, $1) }

time :: { Time }
  : TIME           { (\(Time t) -> t) $1 }

soattrVals :: { [(Key,Value)] }
  : ',' oattrVals       { $2 }
  | ',' '-'             { [] {- XXX the Prov-N spec does not acknowledge this, but validators say OK -} }
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
  -- XXX this will be lexed as a stringlit | '\'' ident '\''             { ValIdent $2                         }

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

{

parseProvN:: Text -> Either ParseError Prov
parseProvN txt = runParser txt parseProv

parseProvNFile :: FilePath -> IO (Either ParseError Prov)
parseProvNFile fp = parseProvN <$> L.readFile fp

}
