{-# LANGUAGE TupleSections       #-}
module IngestDaemon.Types where

import           Control.Concurrent.MVar
import           Data.HashMap.Strict as HMap
import           Data.Text
import           Data.UUID as UUID

import           CommonDataModel.Types
import           CompileSchema

--------------------------------------------------------------------------------
--  Map of operations that failed or have yet to get a result

newtype FailedInsertionDB =
  FIDB { getMV :: MVar (HashMap UUID.UUID OperationRecord) }

type HttpCode = Int
data OperationRecord = OpRecord
                          { input    :: [Input]
                          , code     :: Maybe HttpCode
                          }

data Input = Input { original  :: TCCDMDatum
                    -- ^ CDM received on the wire
                   , statement :: Statement
                    -- ^ Operation we've compiled the CDM into for Titan
                   , inputAge  :: !Int
                    -- ^ Number of times we've tried to run this operation.
                   } deriving (Show)

type Statement = Operation Text


--  Mutations on the map

insertDB :: UUID.UUID -> [Input] -> FailedInsertionDB -> IO ()
insertDB key ipts (FIDB mv) = modifyMVar_ mv (pure . HMap.insert key rec)
 where rec = OpRecord ipts Nothing

lookupDB :: UUID.UUID -> FailedInsertionDB -> IO (Maybe OperationRecord)
lookupDB uid fdb =
 do mp <- tryReadMVar (getMV fdb)
    return $ maybe Nothing (HMap.lookup uid) mp

deleteDB :: UUID.UUID -> FailedInsertionDB -> IO ()
deleteDB k (FIDB mv) = modifyMVar_ mv (pure . HMap.delete k)

resetDB :: FailedInsertionDB -> IO [OperationRecord]
resetDB (FIDB mv) = HMap.elems <$> modifyMVar mv (pure . (HMap.empty,))

sizeDB :: FailedInsertionDB -> IO Int
sizeDB (FIDB mv) = HMap.size <$> readMVar mv

-- Sets the HTTP code for the OperationRecord indicated by a particular map
-- key.
setCodeDB :: UUID.UUID -> HttpCode -> FailedInsertionDB -> IO ()
setCodeDB key cd (FIDB mv) = modifyMVar_ mv (pure . HMap.adjust (\rec -> rec { code = Just cd}) key)

newDB :: IO FailedInsertionDB
newDB = FIDB <$> newMVar HMap.empty
