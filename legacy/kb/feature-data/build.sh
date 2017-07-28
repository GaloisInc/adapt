#!/bin/sh

set -e

if [ "$1" = "clean" ]; then
	cabal clean
	cabal sandbox delete
	exit 0
fi

if [ ! -f cabal.sandbox.config ]; then
	cabal sandbox init
	cabal install --only-dep
fi

cabal configure
cabal build
