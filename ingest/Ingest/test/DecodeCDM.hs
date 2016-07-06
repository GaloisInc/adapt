import CommonDataModel as CDM
import Data.Avro as Avro
import qualified Data.Avro.Types as T
import qualified Data.Avro.Schema as S
import qualified Data.Avro.Decode as D
import Data.Avro.Schema (Result(..))
import qualified Data.ByteString.Lazy as BL
import System.Environment

main :: IO ()
main = do
  [f] <- getArgs
  let k (Success x) = x
      k (Error e)  = error e
  bytes <- BL.readFile f
  let Right (sch,_) = D.decodeContainer bytes
  let cont = Avro.decodeContainerBytes bytes
  putStrLn $ "Number of objects: " ++ show (length $ concat cont)
  let valsFromBytes = (map (either error id . D.decodeAvro sch) (concat cont) :: [T.Value S.Type])
  -- putStrLn (show valsFromBytes)

  stmts <- CDM.readContainer bytes
  print stmts
  print $ concat stmts == map (k . fromAvro) valsFromBytes
  let (ns,es) = CDM.toSchema (concat stmts)
  print (length $ show stmts)
  print $ (length ns, length es, length (show es), length (show ns))
