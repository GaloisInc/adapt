#!/bin/sh

set -e

if [ ! -d .cabal-sandbox ]; then
	cabal sandbox init &
	cabal update       &
	wait

	cabal install happy alex

	cabal install --only-dep
	cabal configure

fi

# add alex and happy to the path
export PATH=$PWD/.cabal-sandbox/bin:$PATH

cabal build
