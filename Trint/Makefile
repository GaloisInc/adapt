
.PHONY: test create-sandbox

test:
	cabal run -- --verbose --quiet --turtle --ast ${PWD}/test/amazon_ie_template.provn

create-sandbox:
	cabal sandbox init
	cabal sandbox add-source ../Ingest
	cabal install --dependencies-only


