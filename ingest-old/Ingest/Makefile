
default:
	cabal build

create-sandbox:
	cabal sandbox init
	cabal install --dependencies-only

%.pdf: %.md
	pandoc -Vfontsize=12pt -o $@ $<
