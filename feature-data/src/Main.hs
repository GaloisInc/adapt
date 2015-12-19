{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE GeneralizedNewtypeDeriving #-}

module Main where

import Gen
import JSON

import           Data.List (intersperse,intercalate)
import qualified Data.Text.Lazy as T
import           GHC.Generics (Generic)
import           System.Environment (getArgs)


main :: IO ()
main  =
  do args <- getArgs
     case args of

       ["user-executes-program"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 return (UserExecutesProgram u p)

            dumpJSONList features

       ["program-reads-file"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 f <- file u
                 return (ProgramReadsFile u p f)

            dumpJSONList features

       ["program-listens-on-port"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 x <- port
                 return (ProgramListensOnPort u p x)
            dumpJSONList features

       ["program-makes-tcp-connection"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 h <- host
                 x <- port
                 return (ProgramMakesTcpConnection u p h x)
            dumpJSONList features

       ["program-receives-signal"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 s <- signal
                 return (ProgramReceivesSignal u p s)
            dumpJSONList features

       ["program-creates-file"] ->
         do features <- runGen $ vectorOf 100 $ sometimesAdmin $
              do u <- user
                 p <- program
                 f <- file u
                 return (ProgramCreatesFile u p f)
            dumpJSONList features

       _ ->
         putStrLn "Unknown feature"


-- Features --------------------------------------------------------------------

data UserExecutesProgram = UserExecutesProgram !User !Program
                           deriving (Show,Generic)

instance ToFields UserExecutesProgram



data ProgramReadsFile = ProgramReadsFile !User !Program !File
                        deriving (Show,Generic)

instance ToFields ProgramReadsFile



data ProgramListensOnPort = ProgramListensOnPort !User !Program !Port
                            deriving (Show,Generic)

instance ToFields ProgramListensOnPort



data ProgramMakesTcpConnection =
  ProgramMakesTcpConnection !User !Program !Host !Port
  deriving (Show,Generic)

instance ToFields ProgramMakesTcpConnection



data ProgramReceivesSignal = ProgramReceivesSignal !User !Program !Signal
                             deriving (Show,Generic)
instance ToFields ProgramReceivesSignal



data ProgramCreatesFile = ProgramCreatesFile !User !Program !File
                          deriving (Show,Generic)

instance ToFields ProgramCreatesFile


-- Feature Elements ------------------------------------------------------------

newtype Program = Program T.Text deriving Show
newtype File    = File    T.Text deriving Show
newtype User    = User    T.Text deriving Show
newtype Port    = Port    Int    deriving Show
newtype Host    = Host    T.Text deriving Show
newtype Signal  = Signal  Int    deriving Show

newtype WriteFile  = WriteFile  File deriving (Show,ToFields)
newtype ReadFile   = ReadFile   File deriving (Show,ToFields)


-- Serialization Support -------------------------------------------------------

instance ToFields Program where toFields (Program a) = [ "program" .= a ]
instance ToFields File    where toFields (File a)    = [ "file"    .= a ]
instance ToFields User    where toFields (User a)    = [ "user"    .= a ]
instance ToFields Port    where toFields (Port a)    = [ "port"    .= a ]
instance ToFields Host    where toFields (Host a)    = [ "host"    .= a ]
instance ToFields Signal  where toFields (Signal a)  = [ "signal"  .= a ]


-- Data Generation -------------------------------------------------------------

sometimesAdmin :: Gen a -> Gen a
sometimesAdmin m =
  do admin <- frequency [ (1, pure True)
                        , (4, pure False) ]
     if admin
        then withAdmin m
        else           m


host :: Gen Host
host  =
  do a <- choose (1,254 :: Int)
     case a of
       127 -> return (Host "127.0.0.1")
       _   -> do b <- choose (1,254 :: Int)
                 c <- choose (1,254 :: Int)
                 d <- choose (1,254 :: Int)
                 return $ Host
                        $ T.pack
                        $ intercalate "." [show a,show b,show c,show d]


user :: Gen User
user  =
  do isAdmin <- checkAdmin
     if isAdmin then return (User "root")
                else frequency [ (3, pure (User "bob"))
                               , (2, pure (User "httpd")) ]

program :: Gen Program
program  =
  do bin  <- elements [ ["bin"]
                      , ["usr", "bin"] ]

     prog <- elements [ "ls", "cd", "sh", "ssh" ]

     return (Program (mkPath (bin ++ [prog])))

mkPath :: [T.Text] -> T.Text
mkPath chunks = T.concat ("/" : intersperse "/" chunks)

file :: User -> Gen File
file u =
  do dir <- directory u
     f   <- elements [ "key", "data.bin", "vimrc", "input.txt" ]
     return (File (mkPath (dir ++ [f])))

-- | Generate a random directory structure.
directory :: User -> Gen [T.Text]
directory (User u) =
  do len   <- choose (0,3)
     admin <- checkAdmin
     if admin
        then frequency [ (3, ("usr" :)       `fmap` usrGen  len)
                       , (2, ("etc" :)       `fmap` etcGen     )
                       , (2, ("var" :)       `fmap` varGen  len)
                       , (1, ("tmp" :)       `fmap` tmpGen     )
                       , (3, (["home",u] ++) `fmap` homeGen len) ]

        else frequency [ (2, ("home":) `fmap` homeGen len)
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


port :: Gen Port
port  =
  do isAdmin <- checkAdmin
     p       <- if isAdmin then frequency [ (4,choose (1,1024))
                                          , (1,choose (1025,65535)) ]
                           else choose (1025,65535)
     return (Port p)


signal :: Gen Signal
signal  =
  do sig <- elements [ 6  -- SIGABRT
                     , 14 -- SIGALRM
                     , 1  -- SIGHUP
                     , 2  -- SIGINT
                     , 9  -- SIGKILL
                     , 3  -- SIGQUIT
                     , 15 -- SIGTERM
                     , 11 -- SIGSEGV
                     ]

     return (Signal sig)
