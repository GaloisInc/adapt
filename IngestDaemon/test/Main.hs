{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE DataKinds #-}
{-# LANGUAGE KindSignatures #-}
module Main where

import           Control.Monad (when,replicateM)
import qualified Data.ByteString.Lazy as BL
import           Data.Binary.Get (runGet)
import           Data.Map (fromList)
import           Data.Monoid
import           Data.Proxy
import qualified Data.Text.Lazy as T
import           Data.Time()
import           Test.Tasty
import           Test.Tasty.HUnit
import           Test.Tasty.QuickCheck
import           GHC.TypeLits

import FromProv
import CommonDataModel.Avro

main :: IO ()
main = defaultMain (testGroup "Adapt-Ingest" adaptIngest)

adaptIngest :: [TestTree]
adaptIngest = [ingest,trint,ingestd]

trint :: TestTree
trint = testGroup "Trint command line tool tests" []

ingestd :: TestTree
ingestd = testGroup "Ingest daemon tests" []

ingest :: TestTree
ingest =
  testGroup "Ingest library tests"
    [ provTC
    , cdmSerialization ]

cdmSerialization :: TestTree
cdmSerialization =
  testGroup "CDM (de)serialization"
   [ testProperty "Unique bytestrings -> unique 8 byte zigzags."
      (\(a :: ZigZag 8) (b :: ZigZag 8) -> a /= b ==> decodeZigZag a /= decodeZigZag b)
   , testProperty "ZigZag decoding is 'Right'"
      (\(a::ZigZag 8) -> (decodeZigZag a `seq` ()) == () )
   ]

data ZigZag (n::Nat) = ZZ { unZZ :: BL.ByteString }
  deriving (Eq, Ord, Show)

instance KnownNat n => Arbitrary (ZigZag n) where
  arbitrary =
    do lst   <- (`mod` 128) <$> arbitrary
       first <- replicateM (fromIntegral $ natVal (Proxy :: Proxy n)) arbitrary
       return $ ZZ (BL.pack (init first ++ [lst]))

decodeZigZag :: forall n. (KnownNat n) => ZigZag n -> Integer
decodeZigZag x = runGet (getZigZag (fromIntegral $ natVal (Proxy :: Proxy n))) (unZZ x)

provTC :: TestTree
provTC = testGroup "Prov-TC translations"
    [ activity
    , entity
    , used
    , wasGeneratedBy
    , wasInformedBy
    ]

entity :: TestTree
entity =
  testGroup "entity"
   [ testCase "Parse 'entity()'" $ assertCDMTranslation
      ( "entity(data:8fdbafb78a139753741fbb1ee6a0ae3ef5de7ab6227b1b4da890be0921638b24,[ tc:path=\"/etc/login.defs\",tc:entityType=\"file\",tc:hasVersion=\"0\"])"
      , [NodeEntity (File { entitySource = SourceLinuxAuditTrace
                         , entityUID = (1,2,3,4)
                         , entityInfo = Info { infoTime = Nothing
                                             , infoPermissions = Nothing
                                             , infoTrustworthiness = Nothing
                                             , infoSensitivity = Nothing
                                             , infoOtherProperties = fromList []
                                             }
                         , entityURL = "/etc/login.defs"
                         , entityFileVersion = -1
                         , entityFileSize = Nothing}
                    )]
        , []
        )
      ]

activity :: TestTree
activity =
  testGroup "activity"
    [ testCase "Parse 'activity()'" $ assertCDMTranslation
        ( "activity(someactivityid,[ tc:uid=\"uidcc\",tc:programName=\"sudo\",tc:pid=\"7561\",tc:source=\"/dev/audit\",tc:ppid=\"7555\",tc:group=\"groupCC\", tc:time=\"2015-09-28T01:06:56Z\"])"
        , [ NodeSubject (Subject {subjectSource = SourceLinuxAuditTrace
                                , subjectUID = (1,2,3,4)
                                , subjectType = SubjectProcess
                                , subjectStartTime = read "2015-09-28 01:06:56 UTC"
                                , subjectPID = Just 7561
                                , subjectPPID = Just 7555
                                , subjectUnitID = Nothing
                                , subjectEndTime = Nothing
                                , subjectCommandLine = Nothing
                                , subjectImportLibs = Nothing
                                , subjectExportLibs = Nothing
                                , subjectProcessInfo = Nothing
                                , subjectOtherProperties = fromList []
                                , subjectLocation = Nothing
                                , subjectSize = Nothing
                                , subjectPpt = Nothing
                                , subjectEnv = Nothing
                                , subjectArgs = Nothing}) ]
        , []
        )
    ]

used :: TestTree
used =
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

wasGeneratedBy :: TestTree
wasGeneratedBy =
  testGroup "wasGeneratedBy"
    [ testCase "parse 'wasGeneratedBy()'" $ assertCDMTranslation
        ("wasGeneratedBy(data:bd82b1fe07b788b29c7b02a2d0f648fb5a4ed5c4fe9461299aa84702aa02356c,data:c33ea9a5304f22162821bf929cd52eca21f5f5ca68101bb48ccc896fd36a5d1c, - ,[ tc:time=\"2015-09-28T01:06:56Z\",tc:operation=\"write\"])"
        , [ NodeSubject (Subject { subjectSource = SourceLinuxAuditTrace
                               , subjectUID = (9,10,11,12)
                               , subjectType = SubjectEvent EventWrite Nothing
                               , subjectStartTime = read "2015-09-28 01:06:56 UTC"
                               , subjectPID = Nothing
                               , subjectPPID = Nothing
                               , subjectUnitID = Nothing
                               , subjectEndTime = Nothing
                               , subjectCommandLine = Nothing
                               , subjectImportLibs = Nothing
                               , subjectExportLibs = Nothing
                               , subjectProcessInfo = Nothing
                               , subjectOtherProperties = fromList []
                               , subjectLocation = Nothing
                               , subjectSize = Nothing
                               , subjectPpt = Nothing
                               , subjectEnv = Nothing
                               , subjectArgs = Nothing}) ]
        , [ Edge { edgeSource = (1,2,3,4)
                 , edgeDestination = (9,10,11,12)
                 , edgeRelationship = WasInformedBy}
          , Edge { edgeSource = (9,10,11,12)
                 , edgeDestination = (5,6,7,8)
                 , edgeRelationship = WasGeneratedBy}
          ])
        ]

wasInformedBy :: TestTree
wasInformedBy =
  testGroup "wasInformedBy"
    [ testCase "parse 'wasInformedBy()'" $ assertCDMTranslation
        ("wasInformedBy(data:c33ea9a5304f22162821bf929cd52eca21f5f5ca68101bb48ccc896fd36a5d1c,data:13bc87ee8c261313e8474a5c259602e2b132e944ea6ecffb147339f19bdcb001,[ tc:time=\"2015-09-28T01:06:56Z\",tc:operation=\"execve\"])"
        , [ ]
        , [ Edge { edgeSource       = (1,2,3,4)
                 , edgeDestination  = (5,6,7,8)
                 , edgeRelationship = WasInformedBy}
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
