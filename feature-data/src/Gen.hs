{-# LANGUAGE DefaultSignatures #-}
{-# LANGUAGE TypeOperators #-}
{-# LANGUAGE FlexibleContexts #-}

module Gen (
    Gen(), runGen,
    GenValue(..),
    frequency,
    elements,
    oneOf,
    choose,
    vectorOf
  ) where

import Control.Monad (replicateM)
import GHC.Generics
import System.Random (StdGen,newStdGen,Random(randomR))


-- Random Data -----------------------------------------------------------------

class GenValue a where
  genValue :: Gen a

  default genValue :: (GGenValue (Rep a), Generic a) => Gen a
  genValue =
    do g <- ggenValue
       return (to g)


class GGenValue f where
  ggenValue :: Gen (f a)

instance GGenValue f => GGenValue (M1 i c f) where
  ggenValue =
    do a <- ggenValue
       return (M1 a)

instance (GGenValue f, GGenValue g) => GGenValue (f :+: g) where
  ggenValue =
    do n <- choose (0,1 :: Int)
       if n == 1 then L1 `fmap` ggenValue
                 else R1 `fmap` ggenValue

instance (GGenValue f, GGenValue g) => GGenValue (f :*: g) where
  ggenValue =
    do f <- ggenValue
       g <- ggenValue
       return (f :*: g)

instance GenValue a => GGenValue (K1 i a) where
  ggenValue =
    do a <- genValue
       return (K1 a)

instance GGenValue U1 where
  ggenValue =
    return U1


frequency :: [(Int,Gen a)] -> Gen a
frequency xs =
  do n <- choose (0,total)
     pick n xs

  where

  total = sum (map fst xs)

  pick n ((k,x):gens)
    | n <= k    = x
    | otherwise = pick (n-k) gens

  pick _ [] = error "frequency: null list given"

elements :: [a] -> Gen a
elements [] = error "elements: null list given"
elements xs =
  do i <- choose (0,length xs-1)
     return (xs !! i)
{-# INLINE elements #-}

oneOf :: [Gen a] -> Gen a
oneOf [] = error "oneOf: null list given"
oneOf xs =
  do i <- choose (0,length xs-1)
     xs !! i
{-# INLINE oneOf #-}

choose :: Random a => (a,a) -> Gen a
choose r = Gen (randomR r)
{-# INLINE choose #-}

vectorOf :: Int -> Gen a -> Gen [a]
vectorOf  = replicateM
{-# INLINE vectorOf #-}


-- | NOTE: The Gen monad is a simplistic port of the Gen monad from QuickCheck,
-- and many of the operations are exactly the same.
newtype Gen a = Gen { unGen :: StdGen -> (a,StdGen) }

runGen :: Gen a -> IO a
runGen f =
  do g <- newStdGen
     case unGen f g of
       (a,_) -> return a

instance Functor Gen where
  fmap f a = Gen $ \i ->
    let (x,i') = unGen a i
     in (f x, i')

  a <$ m = Gen $ \ i ->
    let (_,i') = unGen m i
     in (a,i')

  {-# INLINE fmap #-}
  {-# INLINE (<$) #-}


instance Applicative Gen where
  pure x  = Gen (\i -> (x,i))

  f <*> x = Gen $ \ i ->
    let (g,i')  = unGen f i
        (y,i'') = unGen x i'
     in (g y, i'')

  a *> b = Gen $ \ i ->
    let (_,i') = unGen a i
     in unGen b i'

  a <* b = Gen $ \ i ->
    let (x,i')  = unGen a i
        (_,i'') = unGen b i'
     in (x,i'')

  {-# INLINE pure  #-}
  {-# INLINE (<*>) #-}
  {-# INLINE (*>)  #-}
  {-# INLINE (<*)  #-}

instance Monad Gen where
  return = pure

  m >>= k = Gen $ \ i ->
    let (a,i') = unGen m i
     in unGen (k a) i'

  (>>) = (*>)

  {-# INLINE return #-}
  {-# INLINE (>>=)  #-}
  {-# INLINE (>>)   #-}


