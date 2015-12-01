SEGMENT ?= ../../Adapt-segment/segmenter/segmenter.py

show:
	@cat -n SmallExample.provn

syntax:
	@cat -n BadSyntax.provn
	trint -l BadSyntax.provn

types:
	@cat -n BadTypes.provn
	trint -l BadTypes.provn

fivedirections:
	wc -l 5D.provn
	trint -s 5D.provn

SRI:
	wc -l SRI.provn
	trint -s SRI.provn

visualSegment:
	time trint -s -t SRI.provn
	time $(SEGMENT) SRI.provn.ttl 10
	time for i in *.ttl ; do rapper -w -q -i turtle -o dot $$i > $$i.dot ; dot -T svg $$i.dot -o $$i.svg ; done
	@if [ `uname` == "Darwin" ] ; then open *.svg ; else xdg-open *.svg ; fi

visualSmall:
	trint -g SmallExample.provn
	dot -T svg SmallExample.provn.dot -o SmallExample.provn.svg
	@if [ `uname` == "Darwin" ] ; then open SmallExample.provn.svg ; else xdg-open SmallExample.provn.svg ; fi

clean:
	rm -rf *.ttl *.dot *.trint *.svg *.png
