{-# LANGUAGE DeriveFunctor #-}
{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}
{-# LANGUAGE DefaultSignatures #-}
{-# LANGUAGE FlexibleContexts #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE TypeOperators #-}
{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE MultiWayIf #-}

module Main where

import Gen
import Options

import           Data.List (intercalate,foldl')
import qualified Data.Map.Strict as Map
import           Data.Proxy (Proxy(..),asProxyTypeOf)
import           GHC.Generics
import           System.Exit (exitFailure)
import           System.IO (Handle,stdout,hPutStr,hPutStrLn)


main :: IO ()
main  =
  do opts <- parseArgs
     case optFeature opts of
       "CreatesFile" -> genData opts (Proxy :: Proxy CreatesFile)
       "DeletesFile" -> genData opts (Proxy :: Proxy DeletesFile)
       "ReadsFile"   -> genData opts (Proxy :: Proxy ReadsFile)
       "WritesFile"  -> genData opts (Proxy :: Proxy WritesFile)
       "RunsProgram" -> genData opts (Proxy :: Proxy RunsProgram)
       "CopyFile"    -> genData opts (Proxy :: Proxy CopyFile)
       ""            -> showHelp ["Missing feature"]        >> exitFailure
       f             -> showHelp ["Invalid feature: " ++ f] >> exitFailure


-- Types -----------------------------------------------------------------------

data User       = Root | Guest | Unprivileged
                  deriving (Eq,Ord,Show,Enum,Generic)

data FileOrigin = GeneratedByOther | GeneratedByUser | GeneratedByRoot
                  deriving (Eq,Ord,Show,Enum,Generic)

data File       = TempFile | UserFile | ProtectedSystemFile
                  deriving (Eq,Ord,Show,Enum,Generic)

data Copy       = CopyRemote | CopyLocal
                  deriving (Eq,Ord,Show,Enum,Generic)


-- Primitive Features ----------------------------------------------------------

data CreatesFile = CreatesFile User File       deriving (Show,Eq,Ord,Generic)
data DeletesFile = DeletesFile User File       deriving (Show,Eq,Ord,Generic)
data ReadsFile   = ReadsFile   User File       deriving (Show,Eq,Ord,Generic)
data WritesFile  = WritesFile  User File       deriving (Show,Eq,Ord,Generic)
data RunsProgram = RunsProgram User FileOrigin deriving (Show,Eq,Ord,Generic)
data CopyFile    = CopyFile    Copy File       deriving (Show,Eq,Ord,Generic)


-- Generation ------------------------------------------------------------------

genData :: (Ord a, ElementsOf a, ShowFeature a, GenData a)
        => Options -> Proxy a -> IO ()

genData Options { optRaw = False, .. } p =
  do g <- newStdGen
     let vs = vectorData g p optNumResults optMaxAnomalies
     hPrintVectors stdout p vs

genData Options { .. } p =
  do g <- newStdGen
     let Sample { .. } = runGen g (sampleData optNumResults optMaxAnomalies)
     let _ = head samples `asProxyTypeOf` p
     mapM_ (putStrLn . showFeature) samples


-- | Generate random feature vectors.
vectorData :: (Ord a, GenData a, ElementsOf a)
           => StdGen -> Proxy a -> Int -> Int -> [Sample [Int]]
vectorData g p numVectors numAnomalies =
  take numVectors (iterateGen g numAnomalies step)
  where

  hasAnomalies =
    frequency [ (numVectors - numAnomalies, pure False)
              , (numAnomalies,              pure True) ]

  step a =
    do len <- choose (50, 1000)

       b         <- hasAnomalies
       (a',anom) <- if | a <= 0    -> return (0,0)
                       | b         -> do anom <- choose (1,10)
                                         return (a-1,anom)
                       | otherwise -> return (a,0)

       s <- sampleData len anom
       let _ = head (samples s) `asProxyTypeOf` p
       let counts = genCounts `fmap` s

       return (counts,a')

-- | Generate random raw feature data.
sampleData :: GenData a => Int -> Int -> Gen (Sample [a])
sampleData numRaw numBad = go [] numRaw numBad
  where

  lift b m =
    do a <- m
       return (b,a)

  go acc n a

    | n > 0 =
      do (a',x) <- frequency [ (n, lift a     genGood)
                             , (a, lift (a-1) genBad) ]

         go (x:acc) (n-1) a'

    | otherwise =
       return (Sample (numBad == a) acc)


class GenData a where
  genGood :: Gen a
  genBad  :: Gen a

instance GenData CreatesFile where
  genGood = goodFileOp CreatesFile
  genBad  = badFileOp  CreatesFile

instance GenData DeletesFile where
  genGood = goodFileOp DeletesFile
  genBad  = badFileOp  DeletesFile

instance GenData ReadsFile where
  genGood = goodFileOp ReadsFile
  genBad  = badFileOp  ReadsFile

instance GenData WritesFile where
  genGood = goodFileOp WritesFile
  genBad  = badFileOp  WritesFile

instance GenData RunsProgram where
  genGood =
    do u <- user
       o <- case u of
              Root -> elements [ GeneratedByUser, GeneratedByRoot ]
              _    -> elements [ GeneratedByOther, GeneratedByUser
                               , GeneratedByRoot ]

       return (RunsProgram u o)

  genBad =
       return (RunsProgram Root GeneratedByOther)

instance GenData CopyFile where
  genGood =
    do c <- elements [ CopyRemote, CopyLocal ]
       f <- case c of
              CopyRemote -> elements [ TempFile, UserFile, ProtectedSystemFile ]
              _          -> elements [ TempFile, UserFile ]

       return (CopyFile c f)

  genBad =
       return (CopyFile CopyRemote ProtectedSystemFile)



goodFileOp, badFileOp :: (User -> File -> a) -> Gen a

goodFileOp mk =
  do u <- user
     f <- case u of
            Root -> file
            _    -> elements [TempFile,UserFile]
     return (mk u f)

badFileOp mk =
  do u <- elements [Guest,Unprivileged]
     return (mk u ProtectedSystemFile)

user :: Gen User
user  =
  frequency [ (2, return Root)
            , (1, return Guest)
            , (4, return Unprivileged) ]

file :: Gen File
file  =
  frequency [ (1, return ProtectedSystemFile)
            , (4, return UserFile)
            , (2, return TempFile) ]


-- Serialization ---------------------------------------------------------------

-- | Displaying features, usually of the form @feature(args..)@.
class ShowFeature a where
  showFeature :: a -> String

  default showFeature :: (Generic a, GShowFeature (Rep a)) => a -> String
  showFeature a = gshowFeature (from a)

instance ShowFeature CreatesFile
instance ShowFeature DeletesFile
instance ShowFeature ReadsFile
instance ShowFeature WritesFile
instance ShowFeature RunsProgram
instance ShowFeature CopyFile


-- | Generate a tuple-like formatting of a primitive feature.
class GShowFeature f where
  gshowFeature :: f a -> String

instance GShowFeature f => GShowFeature (M1 D c f) where
  gshowFeature (M1 a) = gshowFeature a

instance (Constructor c, GFields f) => GShowFeature (M1 C c f) where
  gshowFeature m@(M1 a) = conName m ++ "(" ++ intercalate "," (gfields a) ++ ")"

instance (GShowFeature f, GShowFeature g) => GShowFeature (f :+: g) where
  gshowFeature (L1 f) = gshowFeature f
  gshowFeature (R1 g) = gshowFeature g


class GFields f where
  gfields :: f a -> [String]

instance (GFields f, GFields g) => GFields (f :*: g) where
  gfields (f :*: g) = gfields f ++ gfields g

instance GFields f => GFields (S1 s f) where
  gfields (M1 a) = gfields a

instance GFields U1 where
  gfields U1 = []

instance Show a => GFields (K1 i a) where
  gfields (K1 a) = [show a]


-- Vectors ---------------------------------------------------------------------

data Sample a = Sample { nominal :: Bool, samples :: a
                       } deriving (Show,Functor)


hPrintVectors :: (Ord a, ElementsOf a, ShowFeature a)
              => Handle -> Proxy a -> [Sample [Int]] -> IO ()
hPrintVectors h p vs =
  do hPutStrLn h (intercalate "," ("ground.truth" : header))
     mapM_ (hPrintVector h) vs
  where
  header = map show (genHeader (undefined `asProxyTypeOf` p))

genHeader :: (ShowFeature a, ElementsOf a) => a -> [String]
genHeader a = [ showFeature k | k <- elementsOf `asTypeOf` [a] ]


type Vector = Sample [Int]

hPrintVector :: Handle -> Vector -> IO ()
hPrintVector h Sample { .. } =
  do hPutStr h (if nominal then "nominal" else "anomaly")
     go samples
  where
  go (v:vs) =
    do hPutStr h ","
       hPutStr h (show v)
       go vs

  go [] = hPutStrLn h ""

genCounts :: (Ord a, ElementsOf a) => [a] -> [Int]
genCounts as =
  Map.elems (foldl' update (Map.fromList [ (k,0) | k <- elementsOf ]) as)
  where
  update acc a = Map.adjust (+1) a acc


class ElementsOf a where
  elementsOf :: [a]

  default elementsOf :: (Generic a, GElementsOf (Rep a)) => [a]
  elementsOf = map to gelementsOf

instance ElementsOf CreatesFile
instance ElementsOf DeletesFile
instance ElementsOf ReadsFile
instance ElementsOf WritesFile
instance ElementsOf RunsProgram
instance ElementsOf CopyFile


class GElementsOf f where
  gelementsOf :: [f a]

instance GElementsOf f => GElementsOf (M1 i t f) where
  gelementsOf = map M1 gelementsOf

instance (GElementsOf f, GElementsOf g) => GElementsOf (f :+: g) where
  gelementsOf = go True gelementsOf gelementsOf
    where

    go True  (l:ls) rs = L1 l : (go False ls rs)
    go False ls (r:rs) = R1 r : (go True  ls rs)

    go _     [] rs     = map R1 rs
    go _     ls []     = map L1 ls

instance (GElementsOf f, GElementsOf g) => GElementsOf (f :*: g) where
  gelementsOf = [ a :*: b | a <- gelementsOf, b <- gelementsOf ]

instance GElementsOf U1 where
  gelementsOf = []

instance Enum a => GElementsOf (K1 t a) where
  gelementsOf = [ K1 a | a <- [ toEnum 0 .. ] ]
