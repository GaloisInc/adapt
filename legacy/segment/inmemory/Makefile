
PRFX = prov_out_segmented

SEG1 = test$(PRFX)_694638006_1389213733.dot

OUT = \
 $(SEG1).pdf \

all: $(OUT)

seg1: $(SEG1)

$(SEG1):
	cd $(HOME)/.virtualenvs/segmenter || mkvirtualenv segmenter
	if [ _`basename $(VIRTUAL_ENV)` != _segmenter ]; then bash -c 'echo Please run: workon segmenter; exit 1'; fi
	python -c 'import SPARQLWrapper' || pip install -r requirements.txt
	./segmenter.py     test/prov_out.ttl 694575727
	./segmented2dot.py test/prov_out.ttl

%.dot.pdf: %.dot
	./dot2viz.py

clean:
	rm -f $(OUT) test$(PRFX)_*.dot test$(PRFX)_*.dot.pdf test/$(PRFX)_*.ttl
