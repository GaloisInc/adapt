{-# LANGUAGE TupleSections       #-}
module IngestDaemon.Types where

import           Control.Concurrent.MVar
import           Data.HashMap.Strict as HMap
import           Data.Text

import           CommonDataModel.Types
import           CompileSchema

--------------------------------------------------------------------------------
--  Map of operations that failed or have yet to get a result

newtype FailedInsertionDB =
  FIDB { getMV :: MVar (HashMap Text OperationRecord) }

type HttpCode = Int
data OperationRecord = OpRecord
                          { input    :: Input
                          , code     :: Maybe HttpCode
                          }

data Input = Input { original  :: TCCDMDatum
                   , statement :: Statement
                   }

type Statement = Operation Text


--  Mutations on the map

insertDB :: Text -> Input -> FailedInsertionDB -> IO ()
insertDB key ipt (FIDB mv) = modifyMVar_ mv (pure . HMap.insert key rec)
 where rec = OpRecord ipt Nothing

lookupDB :: Text -> FailedInsertionDB -> IO (Maybe OperationRecord)
lookupDB uid fdb =
 do mp <- tryReadMVar (getMV fdb)
    return $ maybe Nothing (HMap.lookup uid) mp

deleteDB :: Text -> FailedInsertionDB -> IO ()
deleteDB k (FIDB mv) = modifyMVar_ mv (pure . HMap.delete k)

resetDB :: FailedInsertionDB -> IO [OperationRecord]
resetDB (FIDB mv) = HMap.elems <$> modifyMVar mv (pure . (HMap.empty,))

-- Sets the HTTP code for the OperationRecord indicated by a particular map
-- key.
setCodeDB :: Text -> HttpCode -> FailedInsertionDB -> IO ()
setCodeDB key cd (FIDB mv) = modifyMVar_ mv (pure . HMap.adjust (\rec -> rec { code = Just cd}) key)

newDB :: IO FailedInsertionDB
newDB = FIDB <$> newMVar HMap.empty
