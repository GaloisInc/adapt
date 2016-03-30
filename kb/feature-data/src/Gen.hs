
module Gen (
    Gen(), runGen, iterateGen,
    frequency,
    elements,
    oneOf,
    choose,
    vectorOf,

    -- ** Re-exported
    StdGen,
    newStdGen,
  ) where

import qualified Control.Applicative as A
import           Control.Monad (replicateM)
import           System.Random (StdGen,newStdGen,Random(randomR),split)


-- Random Data -----------------------------------------------------------------

frequency :: [(Int,Gen a)] -> Gen a
frequency xs =
  do n <- choose (1,total)
     pick n xs

  where

  total = sum (map fst xs)

  pick n ((k,x):gens)
    | n <= k    = x
    | otherwise = pick (n-k) gens

  pick _ [] = error "frequency: null list given"
{-# INLINE frequency #-}

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

runGen :: StdGen -> Gen a -> a
runGen g m = fst (unGen m g)

iterateGen :: StdGen -> b -> (b -> Gen (a,b)) -> [a]
iterateGen g0 b0 m = go g0 b0
  where
  go g b =
    case split g of
      (g1,g2) ->
        case runGen g1 (m b) of
          (a,b2) -> a : go g2 b2


instance Functor Gen where
  fmap f a = Gen $ \ i ->
    let (x,i') = unGen a i
     in (f x, i')

  {-# INLINE fmap #-}


instance A.Applicative Gen where
  pure = \x -> Gen (\ i -> (x,i))

  f <*> x = Gen $ \ i ->
    case unGen f i of
      (g,i') -> case unGen x i' of
                  (y,i'') -> (g y, i'')

  x *> y = Gen $ \ i ->
    case unGen x i of
      (_,i') -> unGen y i'

  {-# INLINE pure  #-}
  {-# INLINE (<*>) #-}
  {-# INLINE (*>) #-}

instance Monad Gen where
  return = A.pure

  m >>= k = Gen $ \ i ->
    case unGen m i of
      (a,i') -> unGen (k a) i'

  (>>) = (*>)

  {-# INLINE return #-}
  {-# INLINE (>>=)  #-}
  {-# INLINE (>>)   #-}
