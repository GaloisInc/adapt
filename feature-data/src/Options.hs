{-# LANGUAGE RecordWildCards #-}

module Options (
    Options(..),
    parseArgs,
    showHelp,
  ) where

import qualified Data.Monoid as M
import           System.Console.GetOpt
import           System.Environment (getArgs,getProgName)
import           System.Exit (exitFailure)


-- Options ---------------------------------------------------------------------

data Options = Options { optFeature      :: String
                       , optNumResults   :: !Int
                       , optMaxAnomalies :: !Int
                       , optRaw          :: Bool
                       } deriving (Show)

defaultOptions :: Options
defaultOptions  = Options { optFeature      = ""
                          , optNumResults   = 1000
                          , optMaxAnomalies = 5
                          , optRaw          = False
                          }


options :: [OptDescr (Parser Options)]
options  =
  [ Option "n" ["num-results"] (ReqArg setNumResults "INTEGER")
    "Set the number of results to output"

  , Option "a" ["max-anomalies"] (ReqArg setMaxAnomalies "INTEGER")
    "Set the maximum number of anomalies to be present"

  , Option "h" ["help"] (NoArg (Error []))
    "Show this message"

  , Option "" ["raw"] (NoArg setRaw)
    "Show the raw feature values, instead of feature vectors"
  ]

setFeature :: String -> Parser Options
setFeature str = Ok (\Options { .. } -> Options { optFeature = str, .. })

setNumResults :: String -> Parser Options
setNumResults str =
  case reads str of
    [(n,"")] -> Ok (\Options { .. } -> Options { optNumResults = n, .. })
    _        -> Error ["Unable to parse --num-results argument"]

-- | When the anomaly value is given as a percentage, the number is calculated
-- based on previous values of @--num-results@.
setMaxAnomalies :: String -> Parser Options
setMaxAnomalies str =
  case reads str of
    [(n,"")]  -> Ok (\Options { .. } -> Options { optMaxAnomalies = n, .. })
    [(n,"%")] -> Ok (\Options { .. } -> Options { optMaxAnomalies = n `percentOf` optNumResults, .. })
    _         -> Error ["Unable to parse --num-anomalies argument"]

percentOf :: Int -> Int -> Int
percentOf p n = round ((fromIntegral p / 100) * fromIntegral n :: Double)

setRaw :: Parser Options
setRaw = Ok (\Options { .. } -> Options { optRaw = True, .. })

parseArgs :: IO Options
parseArgs  =
  do args <- getArgs
     case getOpt (ReturnInOrder setFeature) options args of

       (ps,_,[]) ->
         case M.mconcat ps of
           Ok f     -> return (f defaultOptions)
           Error es -> do showHelp es
                          exitFailure

       (_,_,es) ->
         do showHelp es
            exitFailure


showHelp :: [String] -> IO ()
showHelp es =
  do prog <- getProgName
     let banner = unlines $ es
                         ++ ["", "Usage: " ++ prog ++ " [OPTIONS] <feature>"
                            , "  Features include:" ]
     putStrLn (usageInfo banner options)


-- Parser ----------------------------------------------------------------------

data Parser a = Ok (a -> a) | Error [String]

instance M.Monoid (Parser a) where
  mempty                      = Ok id

  mappend (Ok f)    (Ok g)    = Ok (g . f)
  mappend (Error a) (Error b) = Error (a ++ b)
  mappend e@Error{} _         = e
  mappend _         e         = e
