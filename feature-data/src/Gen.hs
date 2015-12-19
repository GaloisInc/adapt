
module Gen (
    Gen(), runGen,
    frequency,
    elements,
    oneOf,
    choose,
    vectorOf,
    withAdmin,
    checkAdmin,
  ) where

import qualified Control.Applicative as A
import           Control.Monad (replicateM)
import           System.Random (StdGen,newStdGen,Random(randomR))


-- Random Data -----------------------------------------------------------------

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

  {-# INLINE fmap #-}


instance A.Applicative Gen where
  pure x  = Gen (\ _ i -> (x,i))

  f <*> x = Gen $ \ s i ->
    let (g,i')  = unGen f s i
        (y,i'') = unGen x s i'
     in (g y, i'')

  {-# INLINE pure  #-}
  {-# INLINE (<*>) #-}

instance Monad Gen where
  return = pure

  m >>= k = Gen $ \ s i ->
    let (a,i') = unGen m s i
     in unGen (k a) s i'

  {-# INLINE return #-}
  {-# INLINE (>>=)  #-}
