{-# LANGUAGE OverloadedStrings #-}
module Main where

import           Control.Monad (when)
import           Data.Map (fromList)
import           Data.Monoid
import qualified Data.Text.Lazy as T
import           Data.Time()
import           Test.Tasty
import           Test.Tasty.HUnit

import CommonDataModel.FromProv

main :: IO ()
main = defaultMain (testGroup "Adapt-Ingest" adaptIngest)

adaptIngest :: [TestTree]
adaptIngest = [ingest,trint]

trint :: TestTree
trint = testGroup "trint" []

ingest :: TestTree
ingest =
  testGroup "used"
     [ testCase "Parse 'used()'" $ assertCDMTranslation 
          ( "used(thesource,thedest, -, [ tc:time=\"2015-09-28T01:06:56Z\", tc:operation=\"read\"])"
          , [ NodeSubject (Subject { subjectSource = SourceLinuxAuditTrace
                                   , subjectUID = (9,10,11,12)
                                   , subjectType = SubjectEvent EventRead Nothing
                                   , subjectStartTime = read "2015-09-28 01:06:56 UTC"
                                   , subjectPID = Nothing , subjectPPID = Nothing
                                   , subjectUnitID = Nothing , subjectEndTime = Nothing
                                   , subjectCommandLine = Nothing , subjectImportLibs = Nothing
                                   , subjectExportLibs = Nothing , subjectProcessInfo = Nothing
                                   , subjectOtherProperties = fromList []
                                   , subjectLocation = Nothing , subjectSize = Nothing
                                   , subjectPpt = Nothing , subjectEnv = Nothing
                                   , subjectArgs = Nothing })]
          , [ Edge { edgeSource = (1,2,3,4)
                   , edgeDestination = (9,10,11,12)
                   , edgeRelationship = WasInformedBy
                   }
            , Edge { edgeSource = (9,10,11,12)
                   , edgeDestination = (5,6,7,8)
                   , edgeRelationship = Used
                   }
            ]
          )
     ]

assertCDMTranslation :: (T.Text, [Node], [Edge]) -> IO ()
assertCDMTranslation (s,nodes,edges) =
  do let cdm = translateTextCDMPure (cycle [1..31337])
                 (T.unlines [ "document"
                            , "prefix tc <http://spade.csl.sri.com/rdf/audit-tc.rdfs#>"
                            , s
                            , "end document"
                            ])
     when (cdm /= Right (nodes,edges,[]))
          (assertFailure $ unlines [ "Expected: " <> show (nodes,edges,([] :: [()]))
                                   , "But got:  " <> show cdm
                                   ])
