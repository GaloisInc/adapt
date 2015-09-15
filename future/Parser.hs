{-# LANGUAGE OverloadedStrings #-}
module Parser
    ( -- * High-level interface
      parseLazy, parseStrict
      -- * Low-level interface
    , ParseError(..), TypeError(..)
    , RawTA1(..)
    , Object(..), Entity, Verb
    -- * Internal
    , n3, triple, object
    ) where

-- Despite its maturity, we are not using the 'swish' package because it
-- drops the order information on which the ingest depends.  Also, we
-- haven't actually agreed on the wire format so the polish seems
-- premature. Instead we parse individual triples for this prototype.
import           Data.Monoid
import           Data.Attoparsec.Text as A
import           Data.Char (isSpace)
import           Data.Data
import           Data.Text (Text)
import qualified Data.Text as Text
import           MonadLib

import Types

-- Entities are simply IRI's or literal strings with no IRI type or
-- language tag. No blank nodes are supported.
--
-- XXX fails to parse embedded '"' or '>', as it uses those for termination.
object :: Parser (Entity ())
object =
  do skipSpace
     c   <- anyChar
     res <- case c of
              '<' -> iri <?> fail "Error parsing IRI"
              '"' -> stringLit <?> fail "Error parsing Lit"
              't' -> (string "c:" >> tcIRI) <?> fail "Error parsing TC-centric value"
              _   -> fail $ "Expected an '<IRI>' '\"string\"' literal, or 'tc:obj'.  Found character: " ++ show c ++ "."
     return res
  where
    tcIRI,iri, stringLit :: Parser (Object ())
    -- XXX Fix the <someString> case, which is common RDF syntax
    iri       =
        do r <- IRI <$> takeTill (== ':') <*> (takeWhile1 (`elem` (":/" :: String)) >> takeTill (== '>')) <*> pure ()
           A.take 1
           return r
    stringLit =
        do r <- Lit <$> takeTill (== '"') <*> pure ()
           A.take 1
           return r
    tcIRI     = IRI "tc" <$> takeTill isSpace <*> pure ()

-- Triples are RDF elements in the form `subject predicate object .\n`.
-- That is, an Entity type, Verb type, and Entity type followed by a period
-- and (optionally) a new line.
triple :: Parser RawTA1
triple =
  do skipSpace
     res <- RawTA1 <$> (Triple <$> object <*> object <*> object)
     skipSpace
     _ <- char '.'
     return res

n3 :: Text -> Either ParseError (Text, RawTA1)
n3 txt = case parse triple txt of
             Done i r         -> Right (i,r)
             Fail _ ctx err   -> Left (ParseFailure err)
             Partial _consume -> Left PartialParse

-- | Parse errors are silently ignored, resulting in a truncated result.
parseLazy :: Text -> [RawTA1]
parseLazy txt =
    case n3 txt of
        Right (i,r) -> r : parseLazy i
        _           -> []

parseStrict :: Text -> Either ParseError [RawTA1]
parseStrict txt = go [] txt
  where
    go acc t = case n3 t of
           Right (i,r) | Text.empty == i || Text.all isSpace i -> Right (reverse (r:acc))
                       | otherwise       -> go (r:acc) i
           Left x   -> Left x

