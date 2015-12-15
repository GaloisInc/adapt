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
    vectorOf,
    withAdmin,
    checkAdmin,
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
choose r = Gen (\ _ -> randomR r)
{-# INLINE choose #-}

vectorOf :: Int -> Gen a -> Gen [a]
vectorOf  = replicateM
{-# INLINE vectorOf #-}

-- | Generate data in the context of an administrator.
withAdmin :: Gen a -> Gen a
withAdmin m = Gen (\ _ i -> unGen m True i)
{-# INLINE withAdmin #-}

-- | True when data is being generated in the context of an administrator.
checkAdmin :: Gen Bool
checkAdmin  = Gen (\ s i -> (s,i))
{-# INLINE checkAdmin #-}


-- | NOTE: The Gen monad is a simplistic port of the Gen monad from QuickCheck,
-- and many of the operations are exactly the same.
newtype Gen a = Gen { unGen :: Bool -> StdGen -> (a,StdGen) }

runGen :: Gen a -> IO a
runGen f =
  do g <- newStdGen
     case unGen f False g of
       (a,_) -> return a

instance Functor Gen where
  fmap f a = Gen $ \ s i ->
    let (x,i') = unGen a s i
     in (f x, i')

  a <$ m = Gen $ \ s i ->
    let (_,i') = unGen m s i
     in (a,i')

  {-# INLINE fmap #-}
  {-# INLINE (<$) #-}


instance Applicative Gen where
  pure x  = Gen (\ _ i -> (x,i))

  f <*> x = Gen $ \ s i ->
    let (g,i')  = unGen f s i
        (y,i'') = unGen x s i'
     in (g y, i'')

  a *> b = Gen $ \ s i ->
    let (_,i') = unGen a s i
     in unGen b s i'

  a <* b = Gen $ \ s i ->
    let (x,i')  = unGen a s i
        (_,i'') = unGen b s i'
     in (x,i'')

  {-# INLINE pure  #-}
  {-# INLINE (<*>) #-}
  {-# INLINE (*>)  #-}
  {-# INLINE (<*)  #-}

instance Monad Gen where
  return = pure

  m >>= k = Gen $ \ s i ->
    let (a,i') = unGen m s i
     in unGen (k a) s i'

  (>>) = (*>)

  {-# INLINE return #-}
  {-# INLINE (>>=)  #-}
  {-# INLINE (>>)   #-}


