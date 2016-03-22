#!/bin/sh
set -e
echo "Pulling master for all known subtrees!"
SQUASH=
# SQUASH="--squash"

git checkout master
git subtree pull --prefix=ingest git@github.com:GaloisInc/Adapt-Ingest.git master $SQUASH
git subtree pull --prefix=ad git@github.com:GaloisInc/Adapt-AD.git master $SQUASH
git subtree pull --prefix=kb git@github.com:GaloisInc/Adapt-KB.git master $SQUASH
git subtree pull --prefix=classifier git@github.com:GaloisInc/Adapt-classify.git master $SQUASH
git subtree pull --prefix=dx git@github.com:GaloisInc/Adapt-diagnose.git master $SQUASH
git subtree pull --prefix=px git@github.com:GaloisInc/Adapt-px.git master $SQUASH
git subtree pull --prefix=segment git@github.com:GaloisInc/Adapt-segment.git master $SQUASH
git subtree pull --prefix=ui git@github.com:GaloisInc/Adapt-UI.git master $SQUASH
