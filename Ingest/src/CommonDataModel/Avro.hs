{-# LANGUAGE GADTs               #-}
{-# LANGUAGE FlexibleInstances   #-}
{-# LANGUAGE TupleSections       #-}
{-# LANGUAGE EmptyDataDecls      #-}
{-# LANGUAGE KindSignatures      #-}
{-# LANGUAGE DataKinds           #-}
{-# LANGUAGE ScopedTypeVariables #-}
{-# LANGUAGE DeriveDataTypeable  #-}
{-# LANGUAGE FlexibleContexts    #-}
{-# LANGUAGE MultiWayIf          #-}
{-# LANGUAGE OverloadedStrings   #-}
module CommonDataModel.Avro
  (
  -- * Main entry point
    decodeObjectContainer
  -- * Binary interface
  , decodeObjectContainerFor
  , G.runGet, G.runGetOrFail
  , getBytesOfObject, getByteLength, getByteLengths, GetAvro(..)
  -- ** Type specific Getters
  , getDouble, getNull, getMap, getFloat, Value(..)
  -- ** GADT interface
  , AvroValue(..)
  , Array, Null, Enumeration, Mapping, Fixed, Union
  ) where
import Prelude as P
import qualified Codec.Compression.Zlib as Z
import           CommonDataModel.Types as CDM
import           Control.Monad (replicateM, when)
import           Data.Bits
import           Data.Binary.Get (ByteOffset, Get, runGetOrFail)
import qualified Data.Binary.Get as G
import           Data.ByteString (ByteString)
import qualified Data.ByteString.Lazy as BL
import qualified Data.ByteString.Lazy.Char8 as BC
import           Data.Data
import           Data.Int (Int32,Int64)
import           Data.List (foldl')
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Monoid ((<>))
import           Data.Text (Text)
import qualified Data.Text as Text
import qualified Data.Text.Encoding as Text
import           Data.Word (Word16)

import GHC.TypeLits

decodeObjectContainerFor
  :: forall a.
     Get a
  -> BL.ByteString
  -> Either (BL.ByteString, ByteOffset, String)
            (BL.ByteString, ByteOffset, [[a]])
decodeObjectContainerFor getThing bs0 =
  flip runGetOrFail bs0 $ do
    magic <- getFixed avroMagicSize
    when (BL.fromStrict magic /= avroMagicBytes)
         (fail "Invalid magic number at start of container.")
    metadata <- getMap :: Get (Map Text ByteString) -- avro.schema, avro.codec
    sync  <- BL.fromStrict <$> getFixed nrSyncBytes
    codec <- getCodec (BL.fromStrict <$> Map.lookup "avro.codec" metadata)
    getBlocks sync codec
 where
 nrSyncBytes :: Integral sb => sb
 nrSyncBytes = 16

 getBlocks :: BL.ByteString -> (BL.ByteString -> Get BL.ByteString) -> Get [[a]]
 getBlocks sync decompress =
  do nrObj    <- sFromIntegral =<< getLong
     _nrBytes <- getLong
     r        <- replicateM nrObj getThing
     marker   <- G.getLazyByteString nrSyncBytes
     when (marker /= sync) (fail "Invalid marker, does not match sync bytes.")
     e <- G.isEmpty
     if e
       then return [r]
       else (r :) <$> getBlocks sync decompress

 getCodec :: Monad m => Maybe BL.ByteString -> m (BL.ByteString -> m BL.ByteString)
 getCodec code | Just "null"    <- code =
                    return return
               | Just "deflate" <- code =
                    return (maybe (fail "Decompression failed.") return . Z.decompress)
               | Just x <- code =
                    fail ("Unrecognized codec: " ++ BC.unpack x)
               | otherwise = return return

-- | Given an Avro-formatted Object Container, decode the file and return
-- the included datum's in lists of their original block sets 
decodeObjectContainer
    :: forall a. GetAvro a
    => BL.ByteString
    -> Either (BL.ByteString, ByteOffset, String)
              (BL.ByteString, ByteOffset, [[a]])
decodeObjectContainer bs0 = decodeObjectContainerFor getAvro bs0

avroMagicSize :: Integral a => a
avroMagicSize = 4

avroMagicBytes :: BL.ByteString
avroMagicBytes = BC.pack "Obj" <> BL.pack [1]

--------------------------------------------------------------------------------
--  CDM Avro Deserialization

getCDM09 :: Get CDM.TCCDMDatum
getCDM09 =
 do tag <- getLong
    case tag of
      0  -> DatumPTN <$> getAvro
      1  -> DatumSub <$> getAvro
      2  -> DatumEve <$> getAvro
      3  -> DatumNet <$> getAvro
      4  -> DatumFil <$> getAvro
      5  -> DatumSrc <$> getAvro
      6  -> DatumMem <$> getAvro
      7  -> DatumPri <$> getAvro
      8  -> DatumSim <$> getAvro
      _  -> fail $ "Bad tag in CDM Datum: " ++ show tag

instance GetAvro TCCDMDatum where
  getAvro = getCDM09

instance GetAvro ProvenanceTagNode where
  getAvro =
    PTN <$> getAvro
        <*> getAvro
        <*> getAvro
        <*> getAvro

instance GetAvro PTValue where
  getAvro =
    do t <- getLong
       case t of
        0 -> PTVInt <$> getLong
        1 -> PTVTagOpCode <$> getAvro
        2 -> PTVIntegrityTag <$> getAvro
        3 -> PTVConfidentialityTag <$> getAvro
        _ -> fail "Bad union tag in PTValue"

instance GetAvro ConfidentialityTag where
  getAvro = getEnum

instance GetAvro IntegrityTag where
  getAvro = getEnum

instance GetAvro TagOpCode where
  getAvro = getEnum

instance GetAvro Subject where
  getAvro =
       Subject <$> getAvro
               <*> getAvro
               <*> getInt
               <*> getInt
               <*> getAvro
               <*> getLong
               <*> getAvro
               <*> getAvro
               <*> getAvro
               <*> getAvro
               <*> getAvro
               <*> getAvro
               <*> getAvro

instance GetAvro SubjectType where
  getAvro = getEnum

instance GetAvro Event where
  getAvro =
    Event <$> getAvro
          <*> getLong
          <*> getAvro
          <*> getInt
          <*> getAvro
          <*> getAvro
          <*> getAvro
          <*> getAvro
          <*> getAvro
          <*> getAvro
          <*> getAvro
          <*> getAvro


instance GetAvro Value where
  getAvro =
    Value <$> getInt
          <*> getAvro
          <*> getAvro
          <*> getAvro

instance GetAvro EventType where
  getAvro = getEnum

instance GetAvro NetFlowObject where
  getAvro =
    NetFlowObject <$> getAvro
                  <*> getAvro
                  <*> getString
                  <*> getInt
                  <*> getString
                  <*> getInt

instance GetAvro AbstractObject where
  getAvro =
    AbstractObject <$> getAvro
                   <*> getAvro
                   <*> getAvro
                   <*> getAvro
                   <*> getAvro

instance GetAvro Word16 where
  getAvro = G.getWord16le

instance GetAvro FileObject where
  getAvro =
    FileObject <$> getAvro
               <*> getAvro
               <*> getString
               <*> getBoolean
               <*> getInt
               <*> getAvro

instance GetAvro SrcSinkObject where
  getAvro =
    SrcSinkObject <$> getAvro
                  <*> getAvro
                  <*> getAvro

instance GetAvro SrcSinkType where
  getAvro = getEnum

instance GetAvro MemoryObject where
  getAvro =
    MemoryObject <$> getAvro
                 <*> getAvro
                 <*> getAvro
                 <*> getLong

instance GetAvro Principal where
  getAvro =
    Principal <$> getAvro
              <*> getAvroWithDefault PRINCIPAL_LOCAL
              <*> getInt
              <*> getAvro
              <*> getAvro
              <*> getAvro

instance GetAvro PrincipalType where
  getAvro = getEnum

instance GetAvro InstrumentationSource where
  getAvro = getEnum

getAvroWithDefault :: GetAvro a => a -> Get a
getAvroWithDefault _def = {- XXX -} getAvro

instance GetAvro SimpleEdge where
  getAvro =
    SimpleEdge <$> getAvro
               <*> getAvro
               <*> getAvro
               <*> getLong
               <*> getAvro

instance GetAvro EdgeType where
  getAvro = getEnum

--------------------------------------------------------------------------------
--  Native Haskell Getter instances

class GetAvro ty where
  getAvro :: Get ty

instance GetAvro ty => GetAvro (Map Text ty) where
  getAvro = getMap
instance GetAvro Bool where
  getAvro = getBoolean
instance GetAvro Int32 where
  getAvro = getInt
instance GetAvro Int64 where
  getAvro = getLong
instance GetAvro Short where
  getAvro = Short <$> G.getWord16le
instance GetAvro UUID where
  getAvro = UUID <$> getFixed 32
instance GetAvro ByteString where
  getAvro = getBytes
instance GetAvro Text where
  getAvro = getString
instance GetAvro String where
  getAvro = Text.unpack <$> getString
instance GetAvro a => GetAvro [a] where
  getAvro = getArray
instance GetAvro a => GetAvro (Maybe a) where
  getAvro =
    do t <- getLong
       case t of
        0 -> return Nothing
        1 -> Just <$> getAvro
        _ -> fail "Invalid tag for expected {null,a} Avro union"

--------------------------------------------------------------------------------
--  GADT 'Value' Getter instances

data Array a
data Null = Null
data Enumeration
data Mapping a
data Fixed (n :: Nat)
data Union a -- A union that has an 'a' as one of the elements

data AvroValue a where
    ValueNull    :: AvroValue Null
    ValueBoolean :: Bool -> AvroValue Bool
    ValueInt     :: Int32 -> AvroValue Int32
    ValueLong    :: Int64 -> AvroValue Int64
    ValueFloat   :: Float -> AvroValue Float
    ValueDouble  :: Double -> AvroValue Double
    ValueBytes   :: ByteString -> AvroValue ByteString
    ValueString  :: Text -> AvroValue Text
    ValueProd    :: AvroValue a -> AvroValue b -> AvroValue (a,b) -- For record construnctions
    ValueEnum    :: Int -> AvroValue Enumeration
    ValueArray   :: [AvroValue a] -> AvroValue (Array a)
    ValueMap     :: Map Text (AvroValue a) -> AvroValue (Mapping a)
    ValueUnion   :: Int -> AvroValue a -> AvroValue a
    ValueFixed   :: ByteString -> AvroValue (Fixed (n :: Nat))

instance GetAvro (AvroValue Null) where
  getAvro = return ValueNull
instance GetAvro (AvroValue Bool) where
  getAvro = ValueBoolean <$> getBoolean
instance GetAvro (AvroValue Int32) where
  getAvro = ValueInt <$> getInt
instance GetAvro (AvroValue Int64) where
  getAvro = ValueLong <$> getLong
instance GetAvro (AvroValue ByteString) where
  getAvro = ValueBytes <$> getBytes
instance GetAvro (AvroValue Text) where
  getAvro = ValueString <$> getString

instance (GetAvro (AvroValue tyA), GetAvro (AvroValue tyB)) =>
         GetAvro (AvroValue (tyA,tyB)) where
  getAvro = ValueProd <$> getAvro <*> getAvro

instance GetAvro (AvroValue Enumeration) where
  getAvro = ValueEnum . fromIntegral <$> getInt

instance GetAvro (AvroValue a) => GetAvro (AvroValue (Array a)) where
  getAvro = ValueArray <$> getArray

instance GetAvro (AvroValue a) => GetAvro (AvroValue (Mapping a)) where
  getAvro = ValueMap <$> getMap

instance GetAvro a => GetAvro (AvroValue (Union a)) where
  getAvro =
    do (i,t) <- getUnion
       return (ValueUnion (fromIntegral i) t)

instance KnownNat n => GetAvro (AvroValue (Fixed n)) where
  getAvro = ValueFixed <$> getFixed (fromIntegral $ natVal (Proxy :: Proxy n))

--------------------------------------------------------------------------------
--  Specialized Getters

getNull :: Get Null
getNull = return Null

getBoolean :: Get Bool
getBoolean =
 do w <- G.getWord8
    return (w == 0x01)

getInt :: Get Int32
getInt = getZigZag 5

getLong :: Get Int64
getLong = getZigZag 10

getZigZag :: (Bits i, Integral i) => Int -> Get i
getZigZag nrMaxBytes =
  do orig <- getWord8s nrMaxBytes
     let word0 = foldl' (\a x -> (a `shiftL` 7) + fromIntegral x) 0 (reverse orig)
     return ((word0 `shiftR` 1) `xor` (negate (word0  .&. 1) ))
 where
  getWord8s 0 = return []
  getWord8s n =
    do w <- G.getWord8
       let msb = w `testBit` 7
       (w .&. 0x7F :) <$> if msb then getWord8s (n-1)
                                 else return []

getBytes :: Get ByteString
getBytes =
 do w <- getLong
    G.getByteString (fromIntegral w)

getString :: Get Text
getString = Text.decodeUtf8 <$> getBytes

-- a la Java:
--  Bit 31 (the bit that is selected by the mask 0x80000000) represents the
--  sign of the floating-point number. Bits 30-23 (the bits that are
--  selected by the mask 0x7f800000) represent the exponent. Bits 22-0 (the
--  bits that are selected by the mask 0x007fffff) represent the
--  significand (sometimes called the mantissa) of the floating-point
--  number.
--
--  If the argument is positive infinity, the result is 0x7f800000.
--
--  If the argument is negative infinity, the result is 0xff800000.
--
--  If the argument is NaN, the result is 0x7fc00000. 
getFloat :: Get Float
getFloat =
 do f <- G.getWord32le
    let dec | f == 0x7f800000 = 1 / 0
            | f == 0xff800000 = negate 1 /0
            | f == 0x7fc00000 = 0 / 0
            | otherwise =
                let s = if f .&. 0x80000000 == 0 then id else negate
                    e = (f .&. 0x7f800000) `shiftR` 23
                    m = f .&. 0x007fffff
                in s (fromIntegral m * 2^e)
    return dec

-- As in Java:
--  Bit 63 (the bit that is selected by the mask 0x8000000000000000L)
--  represents the sign of the floating-point number. Bits 62-52 (the bits
--  that are selected by the mask 0x7ff0000000000000L) represent the
--  exponent. Bits 51-0 (the bits that are selected by the mask
--  0x000fffffffffffffL) represent the significand (sometimes called the
--  mantissa) of the floating-point number.
--
--  If the argument is positive infinity, the result is
--  0x7ff0000000000000L.
--
--  If the argument is negative infinity, the result is
--  0xfff0000000000000L.
--
--  If the argument is NaN, the result is 0x7ff8000000000000L
getDouble :: Get Double
getDouble =
 do f <- G.getWord64le
    let dec | f == 0x7ff0000000000000 = 1 / 0
            | f == 0xfff0000000000000 = negate 1 / 0
            | f == 0x7ff8000000000000 = 0 / 0
            | otherwise =
                let s = if f .&. 0x8000000000000000 == 0 then id else negate
                    e = (f .&. 0x7ff0000000000000) `shiftR` 52
                    m = f .&. 0x000fffffffffffff
                in s (fromIntegral m * 2^e)
    return dec

--------------------------------------------------------------------------------
--  Complex AvroValue Getters

-- getRecord :: GetAvro ty => Get (AvroValue ty)
-- getRecord = getAvro

-- XXX The type information is inverted here.  You might expect this
-- function to determine the type but that isn't the case as it would
-- require dependent types.  Rather, the
-- caller specifies the type and this function can fail hard if the
-- decode fails, but the tag (first element) and existentially typed
-- second value are unrelated.
getUnion :: GetAvro ty => Get (Int64,ty)
getUnion = (,) <$> getLong <*> getAvro

getFixed :: Int -> Get ByteString
getFixed = G.getByteString

getEnum :: forall a. (Bounded a, Enum a) => Get a
getEnum =
 do x <- fromIntegral <$> getInt
    if x < fromEnum (minBound :: a) || x > fromEnum (maxBound :: a)
      then fail "Decoded enum falls outside the valid range."
      else return (toEnum $ fromIntegral x)

getArray :: GetAvro ty => Get [ty]
getArray =
  do nr <- getLong
     if
      | nr == 0 -> return []
      | nr < 0  ->
          do _len <- getLong
             rs <- replicateM (fromIntegral (abs nr)) getAvro
             (rs ++) <$> getArray
      | otherwise ->
          do rs <- replicateM (fromIntegral nr) getAvro
             (rs ++) <$> getArray

getBytesOfObject :: forall ty. GetAvro ty => Proxy ty -> Get BL.ByteString
getBytesOfObject p =
  do len <- G.lookAhead (getByteLength p)
     G.getLazyByteString len

getByteLength :: forall ty . GetAvro ty => Proxy ty -> Get Int64
getByteLength _ = do
  base <- G.bytesRead
  _ <- getAvro :: Get ty
  off <- G.bytesRead
  return (off - base)

getByteLengths :: forall ty. GetAvro ty => Proxy ty -> Get [Int64]
getByteLengths _ =
  do base <- G.bytesRead
     os <- go
     return (zipWith (-) os (base : os))
 where
 go =
  do e <- G.isEmpty
     if e
      then return []
      else do offset <- (getAvro :: Get ty) >> G.bytesRead
              (offset :) <$> go

getMap :: GetAvro ty => Get (Map Text ty)
getMap = go Map.empty
 where
 go acc =
  do nr <- getLong
     if nr == 0
       then return acc
       else do m <- Map.fromList <$> replicateM (fromIntegral nr) getKVs
               go (Map.union m acc)
 getKVs = (,) <$> getString <*> getAvro

-- Safe-ish from integral
sFromIntegral :: forall a b m. (Monad m, Bounded a, Bounded b, Integral a, Integral b) => a -> m b
sFromIntegral a
  | aI > fromIntegral (maxBound :: b) ||
    aI < fromIntegral (minBound :: b)   = fail "Integral overflow."
  | otherwise                           = return (fromIntegral a)
 where aI = fromIntegral a :: Integer
