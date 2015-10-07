{
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
module Adapt.Parser (

    -- * Parser
    parseDecls,

    module Exports

  ) where

import           Adapt.Parser.AST as Exports
import           Adapt.Parser.Lexeme
import           Adapt.Parser.Lexer
import           Adapt.Parser.PP as Exports
import           Adapt.Parser.Position as Exports (PP(..), pp, pretty)

import qualified Control.Applicative as A
import qualified Data.Text.Lazy as L
import           MonadLib (runM,StateT,get,set,ExceptionT,raise,Id)

}

%token
  ACTIVITY { $$ @ (Located _ (Activity _) ) }
  IDENT    { $$ @ (Located _ (Ident _   ) ) }

  ';'      { Located $$ (Symbol SymSep) }
  '='      { Located $$ (Symbol SymDef) }

%tokentype { Lexeme }
%monad     { ParseM }
%lexer     { lexer  } { Located _ EOF }

%name decls decls

%%

decls :: { [Decl] }
  : {- empty -}    { []      }
  | decl           { [$1]    }
  | decls ';' decl { $3 : $1 }

decl :: { Decl }
  : ident '=' { Decl { dName = $1
                     } }

ident :: { Located L.Text }
  : IDENT { let Located r (Ident n) = $1
            in n `at` r }

activity :: { Located L.Text }
  : ACTIVITY { let Located r (Activity n) = $1
               in n `at` r }

{

-- External Interface-----------------------------------------------------------

parseDecls :: String -> L.Text -> Either Error [Decl]
parseDecls source bytes = runParseM (primLexer source bytes) decls


-- Parser Internals ------------------------------------------------------------

newtype ParseM a = ParseM { unParseM :: StateT [Lexeme] (ExceptionT Error Id) a
                          } deriving (A.Applicative,Functor,Monad)

runParseM :: [Lexeme] -> ParseM a -> Either Error a
runParseM toks m =
  case runM (unParseM m) toks of
    Right (a,_) -> Right a
    Left err    -> Left err

lexer :: (Lexeme -> ParseM a) -> ParseM a
lexer k = ParseM $
  do toks <- get
     case toks of

       Located _ (Error err) : _ ->
            raise err

       t : ts ->
         do set ts
            unParseM (k t)

       [] ->
            raise UnexpectedEOF

happyError :: ParseM a
happyError  = ParseM $
     raise HappyError

-- vim: ft=haskell
}
