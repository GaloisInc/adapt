module CommonDataModel where

import Data.ByteString (ByteString)
import Data.Text (Text)

import CommonDataModel.Types
import qualified Schema as S

toSchema :: [TCCDMDatum] -> ([Node], [Edge])
toSchema [] = ([],[])
toSchema (x:xs) =
  let (ns,es) = runTr $ translate x
      (nss,ess) = toSchema xs
  in (ns ++ nss, es ++ ess)

type Translate a = StateT State Identity a

data Result = Result { warnings :: [Warning]
                     , nodes    :: [Node]
                     , edges    :: [Edge]
                     } deriving (Eq, Ord, Show)

newtype Warning = WarnNoTranslation Text
                | WarnOther Text
      deriving (Eq, Ord, Show)

translate :: TCCDMDatum -> Result
translate datum =
  case datum of
   PTN provenanceTagNode -> translatePTN           provenanceTagNode
   Sub subj              -> translateSubject       subj
   Eve event             -> translateEvent         event
   Net netFlowObject     -> translateNetFlowObject netFlowObject
   Fil fileObject        -> translateFileObject    fileObject
   Src srcSinkObject     -> translateSrcSinkObject srcSinkObject
   Mem memoryObject      -> translateMemoryObject  memoryObject
   Pri principal         -> translatePrincipal     principal
   Sim simpleEdge        -> translateSimpleEdge    simpleEdge

translatePTN :: ProvenanceTagNode -> Translate ()
translatePTN           (ProvenanceTagNode {..}) = 
  warnNoTranslation "No Adapt Schema for Provenance Tag Nodes."

translateSubject :: Subject -> Translate ()
translateSubject       (Subject {..}) =

translateEvent :: Event -> Translate ()
translateEvent         (Event {..})

translateNetFlowObject ::NetFlowObject -> Translate () 
translateNetFlowObject (NetFlowObject {..})

translateFileObject :: FileObject -> Translate ()
translateFileObject    (FileObject {..})

translateSrcSinkObject ::SrcSinkObject -> Translate () 
translateSrcSinkObject (SrcSinkObject {..})

translateMemoryObject :: MemoryObject -> Translate ()
translateMemoryObject  (MemoryObject {..})

translatePrincipal :: Principal -> Translate ()
translatePrincipal     (Principal {..})

translateSimpleEdge :: SimpleEdge -> Translate ()
translateSimpleEdge    (SimpleEdge {..})
