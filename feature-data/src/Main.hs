{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE OverloadedStrings #-}

module Main where

import Gen
import JSON

import           Data.List (intersperse)
import qualified Data.Text.Lazy as T
import           GHC.Generics (Generic)
import           System.Environment (getArgs)


main :: IO ()
main  =
  do args <- getArgs
     case args of

       ["user-executes-program"] ->
         do features <- runGen (vectorOf 100 genValue)
            dumpJSONList (features :: [UserExecutesProgram])

       ["program-reads-file"] ->
         do features <- runGen (vectorOf 100 genValue)
            dumpJSONList (features :: [ProgramReadsFile])

       _ ->
         putStrLn "Unknown feature"


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


-- Serialization Support -------------------------------------------------------

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


-- Data Generation -------------------------------------------------------------

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


