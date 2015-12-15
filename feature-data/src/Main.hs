{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE DefaultSignatures #-}
{-# LANGUAGE FlexibleContexts #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE TypeOperators #-}

module Main where

import           Control.Monad (replicateM,foldM_)
import           Data.Aeson
import           Data.Aeson.Types (Pair)
import qualified Data.ByteString.Lazy.Char8 as L
import           Data.Char (isUpper,toLower)
import           Data.List (intersperse)
import qualified Data.Text.Lazy as T
import           GHC.Generics
import           System.Environment (getArgs)
import           System.Random (StdGen,newStdGen,Random(randomR))


main :: IO ()
main  =
  do args <- getArgs
     case args of

       ["user-executes-program"] ->
         do features <- runGen (vectorOf 100 genValue)
            dumpJSONList (features :: [UserExecutesProgram])

       _ ->
         putStrLn "Unknown feature"



-- Feature Elements ------------------------------------------------------------

newtype Program = Program T.Text deriving (Show,Generic)
newtype File    = File    T.Text deriving (Show,Generic)
newtype User    = User    T.Text deriving (Show,Generic)
newtype Port    = Port    Int    deriving (Show,Generic)
newtype Host    = Host    T.Text deriving (Show,Generic)
newtype Signal  = Signal  Int    deriving (Show,Generic)

isRoot :: User -> Bool
isRoot (User "root") = True
isRoot _             = False


-- Features --------------------------------------------------------------------

data UserExecutesProgram = UserExecutesProgram !User !Program
                           deriving (Show,Generic)

instance ToFields UserExecutesProgram
instance GenValue UserExecutesProgram



data ProgramReadsFile = ProgramReadsFile !User !Program !File
                        deriving (Show,Generic)

instance ToFields ProgramReadsFile
instance GenValue ProgramReadsFile



data ProgramListensOnPort = ProgramListensOnPort !User !Program !Port
                            deriving (Show,Generic)

instance ToFields ProgramListensOnPort



data ProgramMakesTcpConnection = ProgramMakesTcpConnection !User !Program !Host !Port
                                 deriving (Show,Generic)

instance ToFields ProgramMakesTcpConnection



data ProgramReceivesSignal = ProgramReceivesSignal !User !Program !Signal
                             deriving (Show,Generic)

instance ToFields ProgramReceivesSignal



data ProgramCreatesFile = ProgramCreatesFile !User !Program !File
                          deriving (Show,Generic)

instance ToFields ProgramCreatesFile


-- Random Data -----------------------------------------------------------------

class GenValue a where
  genValue :: Gen a

  default genValue :: (GGenValue (Rep a), Generic a) => Gen a
  genValue =
    do g <- ggenValue
       return (to g)


instance GenValue User where
  genValue =
    do user <- frequency [ (3, pure "bob")
                         , (2, pure "httpd")
                         , (1, pure "root") ]
       return (User user)

instance GenValue Program where
  genValue =
    do bin  <- elements [ ["bin"]
                        , ["usr", "bin"] ]

       prog <- elements [ "ls", "cd", "sh", "ssh" ]

       return (Program (mkPath (bin ++ [prog])))

instance GenValue File where
  genValue =
    do dir  <- directory
       file <- elements [ "key", "data.bin", "vimrc", "input.txt" ]
       return (File (mkPath (dir ++ [file])))


class GGenValue f where
  ggenValue :: Gen (f a)

instance GGenValue f => GGenValue (M1 i c f) where
  ggenValue =
    do a <- ggenValue
       return (M1 a)

instance (GGenValue f, GGenValue g) => GGenValue (f :+: g) where
  ggenValue =
    do n <- choose (0,1 :: Int)
       if n == 1 then L1 `fmap` ggenValue
                 else R1 `fmap` ggenValue

instance (GGenValue f, GGenValue g) => GGenValue (f :*: g) where
  ggenValue =
    do f <- ggenValue
       g <- ggenValue
       return (f :*: g)

instance GenValue a => GGenValue (K1 i a) where
  ggenValue =
    do a <- genValue
       return (K1 a)

instance GGenValue U1 where
  ggenValue =
    return U1


mkPath :: [T.Text] -> T.Text
mkPath chunks = T.concat ("/" : intersperse "/" chunks)

-- | Generate a random directory structure.
directory :: Gen [T.Text]
directory  =
  do len <- choose (0,3)
     frequency [ (3, ("usr" :) `fmap` usrGen  len)
               , (3, ("home":) `fmap` homeGen len)
               , (2, ("etc" :) `fmap` etcGen     )
               , (2, ("var" :) `fmap` varGen  len)
               , (1, ("tmp" :) `fmap` tmpGen     ) ]

  where

  usrGen len = vectorOf len $
    frequency [ (3, pure "local")
              , (3, pure "bin")
              , (2, pure "share")
              , (1, pure "lib") ]

  homeGen len = vectorOf len $
    frequency [ (3, pure ".vim")
              , (2, pure "bin")
              , (1, pure ".config") ]

  etcGen =
    frequency [ (1, pure [])
              , (2, pure ["system"]) ]

  varGen len = vectorOf len $
    frequency [ (2, pure "run")
              , (1, pure "lock") ]

  tmpGen = pure []


frequency :: [(Int,Gen a)] -> Gen a
frequency xs =
  do n <- choose (0,total)
     pick n xs

  where

  total = sum (map fst xs)

  pick n ((k,x):xs)
    | n <= k    = x
    | otherwise = pick (n-k) xs

  pick n [] = error "frequency: null list given"

elements :: [a] -> Gen a
elements [] = error "elements: null list given"
elements xs =
  do i <- choose (0,length xs-1)
     return (xs !! i)
{-# INLINE elements #-}

oneOf :: [Gen a] -> Gen a
oneOf [] = error "oneOf: null list given"
oneOf xs =
  do i <- choose (0,length xs-1)
     xs !! i
{-# INLINE oneOf #-}

choose :: Random a => (a,a) -> Gen a
choose r = Gen (randomR r)
{-# INLINE choose #-}

vectorOf :: Int -> Gen a -> Gen [a]
vectorOf  = replicateM
{-# INLINE vectorOf #-}


-- | NOTE: The Gen monad is a simplistic port of the Gen monad from QuickCheck,
-- and many of the operations are exactly the same.
newtype Gen a = Gen { unGen :: StdGen -> (a,StdGen) }

runGen :: Gen a -> IO a
runGen f =
  do g <- newStdGen
     case unGen f g of
       (a,_) -> return a

instance Functor Gen where
  fmap f a = Gen $ \i ->
    let (x,i') = unGen a i
     in (f x, i')

  a <$ m = Gen $ \ i ->
    let (_,i') = unGen m i
     in (a,i')

  {-# INLINE fmap #-}
  {-# INLINE (<$) #-}


instance Applicative Gen where
  pure x  = Gen (\i -> (x,i))

  f <*> x = Gen $ \ i ->
    let (g,i')  = unGen f i
        (y,i'') = unGen x i'
     in (g y, i'')

  a *> b = Gen $ \ i ->
    let (_,i') = unGen a i
     in unGen b i'

  a <* b = Gen $ \ i ->
    let (x,i')  = unGen a i
        (_,i'') = unGen b i'
     in (x,i'')

  {-# INLINE pure  #-}
  {-# INLINE (<*>) #-}
  {-# INLINE (*>)  #-}
  {-# INLINE (<*)  #-}

instance Monad Gen where
  return = pure

  m >>= k = Gen $ \ i ->
    let (a,i') = unGen m i
     in unGen (k a) i'

  (>>) = (*>)

  {-# INLINE return #-}
  {-# INLINE (>>=)  #-}
  {-# INLINE (>>)   #-}



-- Serialization Support -------------------------------------------------------

class ToFields a where
  toFields :: a -> [Pair]

  default toFields :: (GToFields (Rep a), Generic a) => a -> [Pair]
  toFields a = gtoFields (from a)

instance ToFields Program where
  toFields (Program a) = [ "program" .= a ]

instance ToFields File where
  toFields (File a) = [ "file" .= a ]

instance ToFields User where
  toFields (User a) = [ "user" .= a ]

instance ToFields Port where
  toFields (Port a) = [ "port" .= a ]

instance ToFields Host where
  toFields (Host a) = [ "host" .= a ]

instance ToFields Signal where
  toFields (Signal a) = [ "signal" .= a ]


toObject :: ToFields a => a -> Value
toObject a = object (toFields a)

dumpJSONList :: ToFields a => [a] -> IO ()
dumpJSONList xs =
  do foldM_ dump '[' xs
     putStrLn "]"
  where
  dump c x =
    do putStr [c,' ']
       L.putStrLn (encode (toObject x))
       return ','

unCamelCase :: String -> String
unCamelCase (x:xs)
  | isUpper x = toLower x : concatMap hyphenate xs
  | otherwise =         x : concatMap hyphenate xs
  where
  hyphenate c
    | isUpper c = [ '-', toLower c ]
    | otherwise = [ c ]


class GToFields f where
  gtoFields :: f a -> [Pair]

-- Datatypes define the feature name
instance (GToFields f, Datatype c) => GToFields (M1 D c f) where
  gtoFields m@(M1 a) = "feature" .= unCamelCase (datatypeName m)
                     : gtoFields a

-- constructors are just passed through
instance GToFields f => GToFields (M1 C c f) where
  gtoFields (M1 a) = gtoFields a

instance GToFields f => GToFields (M1 S c f) where
  gtoFields (M1 a) = gtoFields a

instance (GToFields f, GToFields g) => GToFields (f :*: g) where
  gtoFields (l :*: r) = gtoFields l ++ gtoFields r

instance ToFields a => GToFields (K1 i a) where
  gtoFields (K1 a) = toFields a


