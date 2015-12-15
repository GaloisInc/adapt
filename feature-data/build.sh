#!/bin/sh

set -e

if [ ! -f cabal.sandbox.config ]; then
	cabal sandbox init
	cabal install --only-dep
fi

cabal configure
cabal build
